package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.Account
import com.example.domain.BackupPayload
import com.example.domain.Category
import com.example.domain.InitialSeedData
import com.example.domain.JalaliCalendar
import com.example.domain.Loan
import com.example.domain.Person
import com.example.domain.Transaction
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreGhestYarRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("کاربر باید با حساب گوگل وارد شده باشد.")
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("حساب کاربری تغییر کرده است.")
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun loansCol(uid: String) = userDoc(uid).collection("loans")
    private fun accountsCol(uid: String) = userDoc(uid).collection("accounts")
    private fun categoriesCol(uid: String) = userDoc(uid).collection("categories")
    private fun transactionsCol(uid: String) = userDoc(uid).collection("transactions")
    private fun peopleCol(uid: String) = userDoc(uid).collection("people")

    private fun <T> observeSubcollection(
        userId: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/$collectionName"
        emitAll(
            userDoc(uid).collection(collectionName)
                .snapshots()
                .map { snap -> snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLoans(userId: String): Flow<List<Loan>> =
        observeSubcollection(userId, "loans", Loan::class.java)

    fun observeAccounts(userId: String): Flow<List<Account>> =
        observeSubcollection(userId, "accounts", Account::class.java)

    fun observeCategories(userId: String): Flow<List<Category>> =
        observeSubcollection(userId, "categories", Category::class.java)

    fun observeTransactions(userId: String): Flow<List<Transaction>> =
        observeSubcollection(userId, "transactions", Transaction::class.java)

    fun observePeople(userId: String): Flow<List<Person>> =
        observeSubcollection(userId, "people", Person::class.java)

    suspend fun ensureInitialDataSeeded(userId: String): Result<Boolean> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val metaSnap = userDoc(uid).get().await()
            if (metaSnap.exists() && metaSnap.getBoolean("initialDataSeeded") == true) {
                return Result.success(false)
            }
            val seed = InitialSeedData.initialPayload()
            val ops = mutableListOf<BatchOp>()
            ops.add(
                BatchOp.Set(
                    userDoc(uid),
                    mapOf(
                        "userId" to uid,
                        "initialDataSeeded" to true,
                        "categoriesInitialized" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            seed.loans.forEach { ops.add(BatchOp.Set(loansCol(uid).document(it.id), loanCreateMap(it))) }
            seed.accounts.forEach { ops.add(BatchOp.Set(accountsCol(uid).document(it.id), accountCreateMap(it))) }
            seed.categories.forEach { ops.add(BatchOp.Set(categoriesCol(uid).document(it.id), categoryCreateMap(it))) }
            seed.people.forEach { ops.add(BatchOp.Set(peopleCol(uid).document(it.id), personCreateMap(it))) }
            seed.transactions.forEach { ops.add(BatchOp.Set(transactionsCol(uid).document(it.id), transactionCreateMap(it))) }
            commitChunked(ops)
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Loans ---
    suspend fun saveLoan(userId: String, loan: Loan, isEdit: Boolean): Result<Unit> {
        val docId = loan.id.ifBlank { "loan-${System.currentTimeMillis()}" }
        val path = "users/$userId/loans/$docId"
        return try {
            val uid = requireUserId(userId)
            val ref = loansCol(uid).document(docId)
            if (isEdit) {
                ref.update(loanUpdateMap(loan)).await()
            } else {
                ref.set(loanCreateMap(loan)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLoan(userId: String, loanId: String): Result<Unit> {
        val path = "users/$userId/loans/$loanId"
        return try {
            val uid = requireUserId(userId)
            loansCol(uid).document(loanId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun payInstallment(
        userId: String,
        loan: Loan,
        installmentIndex: Int,
        accountId: String?,
        categoryId: String?,
        subcategory: String
    ): Result<Unit> {
        val path = "users/$userId/loans/${loan.id}"
        return try {
            val uid = requireUserId(userId)
            val updatedPaid = (loan.paidIndices + installmentIndex).distinct().sorted()
            val ops = mutableListOf<BatchOp>()
            ops.add(
                BatchOp.Update(
                    loansCol(uid).document(loan.id),
                    mapOf(
                        "paidIndices" to updatedPaid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            if (!accountId.isNullOrBlank() && !categoryId.isNullOrBlank()) {
                val today = JalaliCalendar.getTodayJalali()
                val txId = "tx-inst-${System.currentTimeMillis()}"
                val tx = Transaction(
                    id = txId,
                    type = "expense",
                    amount = loan.amount,
                    accountId = accountId,
                    categoryId = categoryId,
                    subcategory = subcategory.take(100),
                    year = today.year,
                    month = today.month,
                    day = today.day,
                    note = "پرداخت قسط ${JalaliCalendar.toPersianDigits(installmentIndex + 1)} ${loan.title}".take(300),
                    linkedLoanId = loan.id,
                    linkedInstallmentIndex = installmentIndex
                )
                ops.add(BatchOp.Set(transactionsCol(uid).document(txId), transactionCreateMap(tx)))
            }
            commitChunked(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun unpayInstallment(
        userId: String,
        loan: Loan,
        installmentIndex: Int,
        linkedTransactionIds: List<String>
    ): Result<Unit> {
        val path = "users/$userId/loans/${loan.id}"
        return try {
            val uid = requireUserId(userId)
            val updatedPaid = loan.paidIndices.filter { it != installmentIndex }
            val ops = mutableListOf<BatchOp>()
            ops.add(
                BatchOp.Update(
                    loansCol(uid).document(loan.id),
                    mapOf(
                        "paidIndices" to updatedPaid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            linkedTransactionIds.forEach { txId ->
                ops.add(BatchOp.Delete(transactionsCol(uid).document(txId)))
            }
            commitChunked(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Accounts ---
    suspend fun saveAccount(userId: String, account: Account, isEdit: Boolean): Result<Unit> {
        val docId = account.id.ifBlank { "acc-${System.currentTimeMillis()}" }
        val path = "users/$userId/accounts/$docId"
        return try {
            val uid = requireUserId(userId)
            val ref = accountsCol(uid).document(docId)
            if (isEdit) {
                ref.update(accountUpdateMap(account)).await()
            } else {
                ref.set(accountCreateMap(account)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(userId: String, accountId: String): Result<Unit> {
        val path = "users/$userId/accounts/$accountId"
        return try {
            val uid = requireUserId(userId)
            accountsCol(uid).document(accountId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Categories ---
    suspend fun saveCategory(userId: String, category: Category, isEdit: Boolean): Result<Unit> {
        val docId = category.id.ifBlank { "cat-${System.currentTimeMillis()}" }
        val path = "users/$userId/categories/$docId"
        return try {
            val uid = requireUserId(userId)
            val ref = categoriesCol(uid).document(docId)
            if (isEdit) {
                ref.update(categoryUpdateMap(category)).await()
            } else {
                ref.set(categoryCreateMap(category)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> {
        val path = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireUserId(userId)
            categoriesCol(uid).document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSubcategoriesAndCascade(
        userId: String,
        categoryId: String,
        newSubcategories: List<String>,
        renamedFromTo: Pair<String, String>?,
        affectedTransactionIds: List<String>
    ): Result<Unit> {
        val path = "users/$userId/categories/$categoryId"
        return try {
            val uid = requireUserId(userId)
            val ops = mutableListOf<BatchOp>()
            ops.add(
                BatchOp.Update(
                    categoriesCol(uid).document(categoryId),
                    mapOf(
                        "subcategories" to newSubcategories.take(50),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            if (renamedFromTo != null) {
                val (_, newName) = renamedFromTo
                affectedTransactionIds.forEach { txId ->
                    ops.add(
                        BatchOp.Update(
                            transactionsCol(uid).document(txId),
                            mapOf(
                                "subcategory" to newName.take(100),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                }
            }
            commitChunked(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Transactions ---
    suspend fun saveTransaction(userId: String, tx: Transaction, isEdit: Boolean): Result<Unit> {
        val docId = tx.id.ifBlank { "tx-${System.currentTimeMillis()}" }
        val path = "users/$userId/transactions/$docId"
        return try {
            val uid = requireUserId(userId)
            val ref = transactionsCol(uid).document(docId)
            if (isEdit) {
                ref.update(transactionUpdateMap(tx)).await()
            } else {
                ref.set(transactionCreateMap(tx)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, txId: String): Result<Unit> {
        val path = "users/$userId/transactions/$txId"
        return try {
            val uid = requireUserId(userId)
            transactionsCol(uid).document(txId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- People ---
    suspend fun savePerson(userId: String, person: Person): Result<String> {
        val docId = person.id.ifBlank { "person-${System.currentTimeMillis()}" }
        val path = "users/$userId/people/$docId"
        return try {
            val uid = requireUserId(userId)
            peopleCol(uid).document(docId).set(personCreateMap(person)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePerson(userId: String, personId: String): Result<Unit> {
        val path = "users/$userId/people/$personId"
        return try {
            val uid = requireUserId(userId)
            peopleCol(uid).document(personId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Backup Restore & Full Reset ---
    suspend fun replaceAllData(userId: String, payload: BackupPayload): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val ops = mutableListOf<BatchOp>()

            // Delete existing docs across all 5 subcollections first
            val subCols = listOf("loans", "accounts", "categories", "transactions", "people")
            for (colName in subCols) {
                val existingSnap = userDoc(uid).collection(colName).get().await()
                existingSnap.documents.forEach { doc ->
                    ops.add(BatchOp.Delete(doc.reference))
                }
            }
            commitChunked(ops)

            val writeOps = mutableListOf<BatchOp>()
            val metaExists = userDoc(uid).get().await().exists()
            if (metaExists) {
                writeOps.add(
                    BatchOp.Update(
                        userDoc(uid),
                        mapOf(
                            "initialDataSeeded" to true,
                            "categoriesInitialized" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
            } else {
                writeOps.add(
                    BatchOp.Set(
                        userDoc(uid),
                        mapOf(
                            "userId" to uid,
                            "initialDataSeeded" to true,
                            "categoriesInitialized" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
            }

            payload.loans.forEach { writeOps.add(BatchOp.Set(loansCol(uid).document(it.id), loanCreateMap(it))) }
            payload.accounts.forEach { writeOps.add(BatchOp.Set(accountsCol(uid).document(it.id), accountCreateMap(it))) }
            payload.categories.forEach { writeOps.add(BatchOp.Set(categoriesCol(uid).document(it.id), categoryCreateMap(it))) }
            payload.people.forEach { writeOps.add(BatchOp.Set(peopleCol(uid).document(it.id), personCreateMap(it))) }
            payload.transactions.forEach { writeOps.add(BatchOp.Set(transactionsCol(uid).document(it.id), transactionCreateMap(it))) }

            commitChunked(writeOps)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Serialization Maps (1:1 Parity with firestore.rules, NO "id" key, null filtered) ---
    private fun loanCreateMap(loan: Loan): Map<String, Any> = mapOf(
        "title" to loan.title.trim().take(120),
        "source" to loan.source.trim().take(100),
        "amount" to loan.amount.coerceIn(1L, 1_000_000_000_000L),
        "totalCount" to loan.totalCount.coerceIn(1, 360),
        "startDay" to loan.startDay.coerceIn(1, 31),
        "startMonth" to loan.startMonth.coerceIn(1, 12),
        "startYear" to loan.startYear.coerceIn(1350, 1500),
        "card" to loan.card.trim().take(64),
        "paidIndices" to loan.paidIndices.distinct().sorted().take(360),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun loanUpdateMap(loan: Loan): Map<String, Any> = mapOf(
        "title" to loan.title.trim().take(120),
        "source" to loan.source.trim().take(100),
        "amount" to loan.amount.coerceIn(1L, 1_000_000_000_000L),
        "totalCount" to loan.totalCount.coerceIn(1, 360),
        "startDay" to loan.startDay.coerceIn(1, 31),
        "startMonth" to loan.startMonth.coerceIn(1, 12),
        "startYear" to loan.startYear.coerceIn(1350, 1500),
        "card" to loan.card.trim().take(64),
        "paidIndices" to loan.paidIndices.distinct().sorted().take(360),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun accountCreateMap(account: Account): Map<String, Any> = mapOf(
        "name" to account.name.trim().take(100),
        "initialBalance" to account.initialBalance.coerceIn(-1_000_000_000_000L, 1_000_000_000_000L),
        "cardNumber" to account.cardNumber.trim().take(64),
        "color" to account.color.ifBlank { "#0f7c8a" }.take(16),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun accountUpdateMap(account: Account): Map<String, Any> = mapOf(
        "name" to account.name.trim().take(100),
        "initialBalance" to account.initialBalance.coerceIn(-1_000_000_000_000L, 1_000_000_000_000L),
        "cardNumber" to account.cardNumber.trim().take(64),
        "color" to account.color.ifBlank { "#0f7c8a" }.take(16),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun categoryCreateMap(category: Category): Map<String, Any> = mapOf(
        "name" to category.name.trim().take(100),
        "type" to if (category.type == "income") "income" else "expense",
        "emoji" to category.emoji.ifBlank { "📌" }.take(16),
        "color" to category.color.ifBlank { "#0f7c8a" }.take(16),
        "subcategories" to category.subcategories.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(50),
        "debtCategory" to category.debtCategory,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun categoryUpdateMap(category: Category): Map<String, Any> = mapOf(
        "name" to category.name.trim().take(100),
        "type" to if (category.type == "income") "income" else "expense",
        "emoji" to category.emoji.ifBlank { "📌" }.take(16),
        "color" to category.color.ifBlank { "#0f7c8a" }.take(16),
        "subcategories" to category.subcategories.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(50),
        "debtCategory" to category.debtCategory,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun personCreateMap(person: Person): Map<String, Any> = mapOf(
        "name" to person.name.trim().take(100),
        "note" to person.note.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun transactionCreateMap(tx: Transaction): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "type" to tx.type,
            "amount" to tx.amount.coerceIn(1L, 1_000_000_000_000L),
            "accountId" to tx.accountId?.takeIf { it.isNotBlank() },
            "fromAccountId" to tx.fromAccountId?.takeIf { it.isNotBlank() },
            "toAccountId" to tx.toAccountId?.takeIf { it.isNotBlank() },
            "categoryId" to tx.categoryId?.takeIf { it.isNotBlank() },
            "subcategory" to tx.subcategory.trim().take(100),
            "personId" to tx.personId?.takeIf { it.isNotBlank() },
            "year" to tx.year.coerceIn(1350, 1500),
            "month" to tx.month.coerceIn(1, 12),
            "day" to tx.day.coerceIn(1, 31),
            "note" to tx.note.trim().take(300),
            "linkedLoanId" to tx.linkedLoanId?.takeIf { it.isNotBlank() },
            "linkedInstallmentIndex" to tx.linkedInstallmentIndex,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    private fun transactionUpdateMap(tx: Transaction): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "type" to tx.type,
            "amount" to tx.amount.coerceIn(1L, 1_000_000_000_000L),
            "subcategory" to tx.subcategory.trim().take(100),
            "year" to tx.year.coerceIn(1350, 1500),
            "month" to tx.month.coerceIn(1, 12),
            "day" to tx.day.coerceIn(1, 31),
            "note" to tx.note.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (tx.type == "transfer") {
            map["fromAccountId"] = requireNotNull(tx.fromAccountId?.takeIf { it.isNotBlank() })
            map["toAccountId"] = requireNotNull(tx.toAccountId?.takeIf { it.isNotBlank() })
            map["accountId"] = FieldValue.delete()
            map["categoryId"] = FieldValue.delete()
            map["personId"] = FieldValue.delete()
        } else {
            map["accountId"] = requireNotNull(tx.accountId?.takeIf { it.isNotBlank() })
            map["categoryId"] = requireNotNull(tx.categoryId?.takeIf { it.isNotBlank() })
            map["fromAccountId"] = FieldValue.delete()
            map["toAccountId"] = FieldValue.delete()
            val pid = tx.personId?.takeIf { it.isNotBlank() }
            map["personId"] = pid ?: FieldValue.delete()
        }
        return map
    }

    private sealed interface BatchOp {
        data class Set(val ref: DocumentReference, val data: Map<String, Any>) : BatchOp
        data class Update(val ref: DocumentReference, val data: Map<String, Any>) : BatchOp
        data class Delete(val ref: DocumentReference) : BatchOp
    }

    private suspend fun commitChunked(ops: List<BatchOp>) {
        if (ops.isEmpty()) return
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { op ->
                when (op) {
                    is BatchOp.Set -> batch.set(op.ref, op.data)
                    is BatchOp.Update -> batch.update(op.ref, op.data)
                    is BatchOp.Delete -> batch.delete(op.ref)
                }
            }
            batch.commit().await()
        }
    }
}
