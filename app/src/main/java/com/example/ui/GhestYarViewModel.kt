package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreGhestYarRepository
import com.example.data.NetworkConnectivityMonitor
import com.example.data.ThemePreferencesRepository
import com.example.domain.Account
import com.example.domain.BackupJsonCodec
import com.example.domain.BackupPayload
import com.example.domain.Category
import com.example.domain.InitialSeedData
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.domain.Loan
import com.example.domain.Person
import com.example.domain.SyncMergeEngine
import com.example.domain.SyncStatusUiState
import com.example.domain.Transaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GhestYarTab(val label: String, val fabLabel: String) {
    MONTHLY("اقساط ماه", "وام جدید"),
    FORECAST("آینده", "وام جدید"),
    LOANS("وام‌ها", "وام جدید"),
    ACCOUNTS("حساب‌ها", "حساب جدید"),
    TRANSACTIONS("تراکنش‌ها", "تراکنش جدید"),
    DEBTS("قرض و قوله", "شخص جدید")
}

enum class TransactionFilterType(val label: String, val raw: String) {
    ALL("همه", "all"),
    EXPENSE("هزینه", "expense"),
    INCOME("درآمد", "income"),
    TRANSFER("انتقال", "transfer")
}

data class GhestYarDataBundle(
    val loans: List<Loan> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val people: List<Person> = emptyList()
)

sealed interface CloudSyncState {
    data object Loading : CloudSyncState
    data class Ready(val data: GhestYarDataBundle) : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}

data class GhestYarUiControls(
    val activeTab: GhestYarTab = GhestYarTab.MONTHLY,
    val today: JalaliDate = JalaliCalendar.getTodayJalali(),
    val selectedYear: Int = 1405,
    val selectedMonth: Int = 9,
    val txSelectedYear: Int = 1405,
    val txSelectedMonth: Int = 9,
    val txFilterType: TransactionFilterType = TransactionFilterType.ALL,
    val txSelectedCategories: Set<String> = emptySet(),
    val selectedChartCategoryId: String? = null,
    val isSyncBusy: Boolean = false
)

class GhestYarViewModel(
    private val repository: FirestoreGhestYarRepository,
    private val themePrefs: ThemePreferencesRepository,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val currentUserId: String
) : ViewModel() {

    private val initialToday = JalaliCalendar.getTodayJalali()
    private val defaultYear = if (initialToday.year >= 1405) initialToday.year else 1405
    private val defaultMonth = if (initialToday.year >= 1405) initialToday.month else 9

    private val _controls = MutableStateFlow(
        GhestYarUiControls(
            today = initialToday,
            selectedYear = defaultYear,
            selectedMonth = defaultMonth,
            txSelectedYear = defaultYear,
            txSelectedMonth = defaultMonth
        )
    )
    val controls: StateFlow<GhestYarUiControls> = _controls

    val isDarkTheme: StateFlow<Boolean> = themePrefs.isDarkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false
    )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _syncStatus = MutableStateFlow(
        SyncStatusUiState(isOnline = networkMonitor.isCurrentlyOnline())
    )
    val syncStatus: StateFlow<SyncStatusUiState> = _syncStatus

    val cloudState: StateFlow<CloudSyncState> = combine(
        repository.observeLoans(currentUserId),
        repository.observeAccounts(currentUserId),
        repository.observeCategories(currentUserId),
        repository.observeTransactions(currentUserId),
        repository.observePeople(currentUserId)
    ) { loansSnap, accountsSnap, categoriesSnap, transactionsSnap, peopleSnap ->
        val anyFromCache = loansSnap.isFromCache ||
            accountsSnap.isFromCache ||
            categoriesSnap.isFromCache ||
            transactionsSnap.isFromCache ||
            peopleSnap.isFromCache
        val anyPending = loansSnap.hasPendingWrites ||
            accountsSnap.hasPendingWrites ||
            categoriesSnap.hasPendingWrites ||
            transactionsSnap.hasPendingWrites ||
            peopleSnap.hasPendingWrites

        _syncStatus.update { cur ->
            cur.copy(
                isFromCache = anyFromCache,
                hasPendingWrites = anyPending
            )
        }

        CloudSyncState.Ready(
            GhestYarDataBundle(
                loans = loansSnap.items,
                accounts = accountsSnap.items,
                categories = categoriesSnap.items,
                transactions = transactionsSnap.items,
                people = peopleSnap.items
            )
        ) as CloudSyncState
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Firestore data", rootError)
            emit(CloudSyncState.Error(rootError.localizedMessage ?: "خطا در همگام‌سازی با فضای ابری"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudSyncState.Loading
        )

    init {
        viewModelScope.launch {
            _controls.update { it.copy(isSyncBusy = true) }
            val result = repository.ensureInitialDataSeeded(currentUserId)
            _controls.update { it.copy(isSyncBusy = false) }
            result.onSuccess { seeded ->
                if (seeded) emitMessage("اطلاعات اولیه در فضای ابری ذخیره شد")
            }.onFailure { err ->
                emitMessage("خطا در بارگذاری اولیه: ${err.localizedMessage ?: "نامشخص"}")
            }
        }

        viewModelScope.launch {
            var previouslyOnline = networkMonitor.isCurrentlyOnline()
            networkMonitor.isOnline.collect { online ->
                _syncStatus.update { cur ->
                    cur.copy(
                        isOnline = online,
                        lastCheckError = if (online) null else cur.lastCheckError
                    )
                }
                if (online && !previouslyOnline) {
                    delay(1200L)
                    checkWithServer(autoTriggered = true)
                }
                previouslyOnline = online
            }
        }
    }

    private fun currentData(): GhestYarDataBundle =
        (cloudState.value as? CloudSyncState.Ready)?.data ?: GhestYarDataBundle()

    private fun GhestYarDataBundle.toBackupPayload(): BackupPayload = BackupPayload(
        loans = loans,
        accounts = accounts,
        categories = categories,
        transactions = transactions,
        people = people
    )

    fun checkWithServer(autoTriggered: Boolean = false) {
        if (!_syncStatus.value.isOnline) {
            if (!autoTriggered) {
                _syncStatus.update {
                    it.copy(lastCheckError = "اتصال اینترنت برقرار نیست. لطفاً پس از اتصال دوباره بررسی کنید.")
                }
            }
            return
        }
        if (_syncStatus.value.isCheckingServer || _syncStatus.value.isResolvingConflict) return

        viewModelScope.launch {
            _syncStatus.update { it.copy(isCheckingServer = true, lastCheckError = null) }
            val localPayload = currentData().toBackupPayload()
            val result = repository.fetchServerPayload(currentUserId)
            result.onSuccess { serverPayload ->
                val now = System.currentTimeMillis()
                val report = SyncMergeEngine.compare(localPayload, serverPayload, now)
                _syncStatus.update {
                    it.copy(
                        isCheckingServer = false,
                        lastCheckedAtMillis = now,
                        lastCheckError = null,
                        comparisonReport = report
                    )
                }
                if (autoTriggered) {
                    if (!report.isInSync) {
                        emitMessage("اینترنت متصل شد؛ تفاوت‌هایی با سرور پیدا شد. برای بررسی روی نشانگر وضعیت بزنید.")
                    }
                } else {
                    if (report.isInSync) {
                        emitMessage("داده‌های دستگاه با سرور کاملاً یکسان و به‌روز است")
                    } else {
                        emitMessage("مقایسه با سرور انجام شد؛ تفاوت‌ها آماده تصمیم‌گیری هستند")
                    }
                }
            }.onFailure { err ->
                _syncStatus.update {
                    it.copy(
                        isCheckingServer = false,
                        lastCheckError = if (autoTriggered) null else "خطا در ارتباط با سرور: ${err.localizedMessage ?: "نامشخص"}"
                    )
                }
                if (!autoTriggered) {
                    emitMessage("خطا در بررسی اطلاعات سرور")
                }
            }
        }
    }

    fun resolveWithSmartMerge(onDone: () -> Unit = {}) {
        val report = _syncStatus.value.comparisonReport ?: return
        if (!_syncStatus.value.isOnline || _syncStatus.value.isResolvingConflict) return
        viewModelScope.launch {
            _syncStatus.update { it.copy(isResolvingConflict = true, lastCheckError = null) }
            val merged = SyncMergeEngine.smartMergeNewest(report.localPayload, report.serverPayload)
            repository.reconcileWithTarget(currentUserId, merged, report.serverPayload)
                .onSuccess {
                    val now = System.currentTimeMillis()
                    val cleanReport = SyncMergeEngine.compare(merged, merged, now)
                    _syncStatus.update {
                        it.copy(
                            isResolvingConflict = false,
                            lastCheckedAtMillis = now,
                            comparisonReport = cleanReport
                        )
                    }
                    emitMessage("ترکیب هوشمند با جدیدترین نسخه هر مورد با موفقیت انجام شد!")
                    onDone()
                }
                .onFailure { err ->
                    _syncStatus.update {
                        it.copy(
                            isResolvingConflict = false,
                            lastCheckError = "خطا در ترکیب هوشمند: ${err.localizedMessage ?: "نامشخص"}"
                        )
                    }
                }
        }
    }

    fun resolveWithServerData(onDone: () -> Unit = {}) {
        val report = _syncStatus.value.comparisonReport ?: return
        if (!_syncStatus.value.isOnline || _syncStatus.value.isResolvingConflict) return
        viewModelScope.launch {
            _syncStatus.update { it.copy(isResolvingConflict = true, lastCheckError = null) }
            val currentLocal = currentData().toBackupPayload()
            repository.reconcileWithTarget(currentUserId, report.serverPayload, currentLocal)
                .onSuccess {
                    repository.fetchServerPayload(currentUserId)
                    val now = System.currentTimeMillis()
                    val cleanReport = SyncMergeEngine.compare(report.serverPayload, report.serverPayload, now)
                    _syncStatus.update {
                        it.copy(
                            isResolvingConflict = false,
                            lastCheckedAtMillis = now,
                            comparisonReport = cleanReport
                        )
                    }
                    emitMessage("داده‌های دستگاه با آخرین نسخه سرور هماهنگ شد!")
                    onDone()
                }
                .onFailure { err ->
                    _syncStatus.update {
                        it.copy(
                            isResolvingConflict = false,
                            lastCheckError = "خطا در دریافت نسخه سرور: ${err.localizedMessage ?: "نامشخص"}"
                        )
                    }
                }
        }
    }

    fun resolveWithLocalData(onDone: () -> Unit = {}) {
        val report = _syncStatus.value.comparisonReport ?: return
        if (!_syncStatus.value.isOnline || _syncStatus.value.isResolvingConflict) return
        viewModelScope.launch {
            _syncStatus.update { it.copy(isResolvingConflict = true, lastCheckError = null) }
            repository.reconcileWithTarget(currentUserId, report.localPayload, report.serverPayload)
                .onSuccess {
                    val now = System.currentTimeMillis()
                    val cleanReport = SyncMergeEngine.compare(report.localPayload, report.localPayload, now)
                    _syncStatus.update {
                        it.copy(
                            isResolvingConflict = false,
                            lastCheckedAtMillis = now,
                            comparisonReport = cleanReport
                        )
                    }
                    emitMessage("داده‌های این دستگاه به عنوان آخرین نسخه در سرور ثبت شدند!")
                    onDone()
                }
                .onFailure { err ->
                    _syncStatus.update {
                        it.copy(
                            isResolvingConflict = false,
                            lastCheckError = "خطا در ارسال داده‌های دستگاه: ${err.localizedMessage ?: "نامشخص"}"
                        )
                    }
                }
        }
    }

    fun emitMessage(text: String) {
        _messages.tryEmit(text)
    }

    fun toggleTheme() {
        viewModelScope.launch {
            themePrefs.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun selectTab(tab: GhestYarTab) {
        _controls.update { it.copy(activeTab = tab) }
    }

    fun changeSelectedMonth(delta: Int) {
        _controls.update { cur ->
            val next = JalaliCalendar.addJalaliMonths(cur.selectedYear, cur.selectedMonth, 1, delta)
            cur.copy(selectedYear = next.year, selectedMonth = next.month)
        }
    }

    fun jumpToCurrentMonth() {
        _controls.update { it.copy(selectedYear = defaultYear, selectedMonth = defaultMonth) }
    }

    fun openForecastMonth(year: Int, month: Int) {
        _controls.update {
            it.copy(
                selectedYear = year,
                selectedMonth = month,
                activeTab = GhestYarTab.MONTHLY
            )
        }
    }

    fun changeTxMonth(delta: Int) {
        _controls.update { cur ->
            val next = JalaliCalendar.addJalaliMonths(cur.txSelectedYear, cur.txSelectedMonth, 1, delta)
            cur.copy(
                txSelectedYear = next.year,
                txSelectedMonth = next.month,
                selectedChartCategoryId = null
            )
        }
    }

    fun toggleTxCategoryFilter(catId: String) {
        _controls.update { cur ->
            val updated = if (catId in cur.txSelectedCategories) {
                cur.txSelectedCategories - catId
            } else {
                cur.txSelectedCategories + catId
            }
            cur.copy(txSelectedCategories = updated, selectedChartCategoryId = null)
        }
    }

    fun clearTxCategoryFilters() {
        _controls.update { it.copy(txSelectedCategories = emptySet(), selectedChartCategoryId = null) }
    }

    fun setTxFilterType(type: TransactionFilterType) {
        _controls.update { it.copy(txFilterType = type) }
    }

    fun selectChartCategory(catId: String?) {
        _controls.update { cur ->
            val next = if (cur.selectedChartCategoryId == catId) null else catId
            cur.copy(selectedChartCategoryId = next)
        }
    }

    // --- Mutations ---
    fun saveLoan(
        editLoanId: String?,
        title: String,
        source: String,
        amount: Long,
        totalCount: Int,
        paidCount: Int,
        startDay: Int,
        startMonth: Int,
        startYear: Int,
        card: String,
        onDone: () -> Unit
    ) {
        val cleanTitle = title.trim()
        val cleanSource = source.trim()
        if (cleanTitle.isEmpty() || cleanSource.isEmpty() || amount <= 0L || totalCount !in 1..360) {
            emitMessage("لطفاً فیلدهای ضروری وام را به‌درستی تکمیل کنید")
            return
        }
        val clampedPaid = paidCount.coerceIn(0, totalCount)
        val existing = editLoanId?.let { id -> currentData().loans.find { it.id == id } }
        val paidIndices = if (existing != null && existing.paidIndices.size == clampedPaid) {
            existing.paidIndices
        } else {
            (0 until clampedPaid).toList()
        }
        val loan = Loan(
            id = existing?.id ?: "loan-${System.currentTimeMillis()}",
            title = cleanTitle,
            source = cleanSource,
            amount = amount,
            totalCount = totalCount,
            startDay = startDay.coerceIn(1, 31),
            startMonth = startMonth.coerceIn(1, 12),
            startYear = startYear.coerceIn(1350, 1500),
            card = card.trim(),
            paidIndices = paidIndices
        )
        viewModelScope.launch {
            repository.saveLoan(currentUserId, loan, isEdit = existing != null)
                .onSuccess {
                    emitMessage(if (existing != null) "وام ویرایش شد" else "وام جدید ثبت شد!")
                    onDone()
                }
                .onFailure { emitMessage("خطا در ذخیره وام: ${it.localizedMessage ?: "نامشخص"}") }
        }
    }

    fun deleteLoan(loanId: String) {
        viewModelScope.launch {
            repository.deleteLoan(currentUserId, loanId)
                .onSuccess { emitMessage("وام حذف شد") }
                .onFailure { emitMessage("خطا در حذف وام") }
        }
    }

    fun payInstallment(
        loanId: String,
        index: Int,
        accountId: String?,
        categoryId: String?,
        subcategory: String,
        onDone: () -> Unit
    ) {
        val loan = currentData().loans.find { it.id == loanId } ?: return
        viewModelScope.launch {
            repository.payInstallment(currentUserId, loan, index, accountId, categoryId, subcategory)
                .onSuccess {
                    emitMessage("پرداخت قسط ثبت شد!")
                    onDone()
                }
                .onFailure { emitMessage("خطا در ثبت پرداخت قسط") }
        }
    }

    fun unpayInstallment(loanId: String, index: Int) {
        val data = currentData()
        val loan = data.loans.find { it.id == loanId } ?: return
        val linkedIds = data.transactions
            .filter { it.linkedLoanId == loanId && it.linkedInstallmentIndex == index }
            .map { it.id }
        viewModelScope.launch {
            repository.unpayInstallment(currentUserId, loan, index, linkedIds)
                .onSuccess { emitMessage("پرداخت قسط لغو شد") }
                .onFailure { emitMessage("خطا در لغو پرداخت") }
        }
    }

    fun saveAccount(
        editId: String?,
        name: String,
        initialBalance: Long,
        cardNumber: String,
        colorHex: String,
        onDone: () -> Unit
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            emitMessage("نام حساب را وارد کنید")
            return
        }
        val account = Account(
            id = editId?.takeIf { it.isNotBlank() } ?: "acc-${System.currentTimeMillis()}",
            name = cleanName,
            initialBalance = initialBalance,
            cardNumber = cardNumber.trim(),
            color = colorHex.ifBlank { "#0f7c8a" }
        )
        viewModelScope.launch {
            repository.saveAccount(currentUserId, account, isEdit = !editId.isNullOrBlank())
                .onSuccess {
                    emitMessage(if (!editId.isNullOrBlank()) "حساب ویرایش شد" else "حساب جدید اضافه شد")
                    onDone()
                }
                .onFailure { emitMessage("خطا در ذخیره حساب") }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(currentUserId, accountId)
                .onSuccess { emitMessage("حساب حذف شد") }
                .onFailure { emitMessage("خطا در حذف حساب") }
        }
    }

    fun saveTransaction(
        editId: String?,
        type: String,
        amount: Long,
        accountId: String?,
        fromAccountId: String?,
        toAccountId: String?,
        categoryId: String?,
        subcategory: String,
        personId: String?,
        year: Int,
        month: Int,
        day: Int,
        note: String,
        onDone: () -> Unit
    ) {
        if (amount <= 0L) {
            emitMessage("مبلغ معتبر وارد کنید")
            return
        }
        if (type == "transfer") {
            if (fromAccountId.isNullOrBlank() || toAccountId.isNullOrBlank() || fromAccountId == toAccountId) {
                emitMessage("حساب مبدأ و مقصد باید متفاوت باشند")
                return
            }
        } else {
            if (accountId.isNullOrBlank() || categoryId.isNullOrBlank()) {
                emitMessage("حساب و دسته‌بندی را انتخاب کنید")
                return
            }
        }
        val tx = Transaction(
            id = editId?.takeIf { it.isNotBlank() } ?: "tx-${System.currentTimeMillis()}",
            type = type,
            amount = amount,
            accountId = if (type == "transfer") null else accountId,
            fromAccountId = if (type == "transfer") fromAccountId else null,
            toAccountId = if (type == "transfer") toAccountId else null,
            categoryId = if (type == "transfer") null else categoryId,
            subcategory = if (type == "transfer") "" else subcategory.trim(),
            personId = if (type == "transfer") null else personId?.takeIf { it.isNotBlank() },
            year = year,
            month = month,
            day = day,
            note = note.trim()
        )
        viewModelScope.launch {
            repository.saveTransaction(currentUserId, tx, isEdit = !editId.isNullOrBlank())
                .onSuccess {
                    emitMessage(
                        when {
                            !editId.isNullOrBlank() -> "تراکنش ویرایش شد"
                            type == "transfer" -> "انتقال وجه با موفقیت ثبت شد"
                            else -> "تراکنش با موفقیت ثبت شد"
                        }
                    )
                    onDone()
                }
                .onFailure { emitMessage("خطا در ذخیره تراکنش") }
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(currentUserId, txId)
                .onSuccess { emitMessage("تراکنش حذف شد") }
                .onFailure { emitMessage("خطا در حذف تراکنش") }
        }
    }

    fun addCategory(
        name: String,
        type: String,
        emoji: String,
        colorHex: String,
        rawSubcategories: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            emitMessage("نام دسته‌بندی را وارد کنید")
            return
        }
        val subs = rawSubcategories.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val isDebt = cleanName.contains("قرض") || cleanName.contains("طلب") || cleanName.contains("بدهی")
        val cat = Category(
            id = "cat-${System.currentTimeMillis()}",
            name = cleanName,
            type = if (type == "income") "income" else "expense",
            emoji = emoji.trim().ifEmpty { "📌" },
            color = colorHex.ifBlank { "#0f7c8a" },
            subcategories = subs,
            debtCategory = isDebt
        )
        viewModelScope.launch {
            repository.saveCategory(currentUserId, cat, isEdit = false)
                .onSuccess { emitMessage("دسته‌بندی «$cleanName» اضافه شد") }
                .onFailure { emitMessage("خطا در افزودن دسته‌بندی") }
        }
    }

    fun updateCategory(category: Category, newName: String, newEmoji: String, newColor: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) {
            emitMessage("نام دسته‌بندی نمی‌تواند خالی باشد")
            return
        }
        val updated = category.copy(
            name = cleanName,
            emoji = newEmoji.trim().ifEmpty { category.emoji },
            color = newColor.ifBlank { category.color }
        )
        viewModelScope.launch {
            repository.saveCategory(currentUserId, updated, isEdit = true)
                .onSuccess { emitMessage("دسته‌بندی به‌روزرسانی شد") }
                .onFailure { emitMessage("خطا در ویرایش دسته‌بندی") }
        }
    }

    fun deleteCategory(categoryId: String) {
        _controls.update { it.copy(txSelectedCategories = it.txSelectedCategories - categoryId) }
        viewModelScope.launch {
            repository.deleteCategory(currentUserId, categoryId)
                .onSuccess { emitMessage("دسته‌بندی حذف شد") }
                .onFailure { emitMessage("خطا در حذف دسته‌بندی") }
        }
    }

    fun addSubcategory(category: Category, subName: String) {
        val clean = subName.trim()
        if (clean.isEmpty() || clean in category.subcategories) return
        viewModelScope.launch {
            repository.updateSubcategoriesAndCascade(
                userId = currentUserId,
                categoryId = category.id,
                newSubcategories = category.subcategories + clean,
                renamedFromTo = null,
                affectedTransactionIds = emptyList()
            ).onSuccess { emitMessage("زیردسته اضافه شد") }
        }
    }

    fun renameSubcategory(category: Category, oldIndex: Int, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || oldIndex !in category.subcategories.indices) return
        val oldName = category.subcategories[oldIndex]
        val updatedList = category.subcategories.toMutableList().apply { set(oldIndex, clean) }
        val affectedIds = currentData().transactions
            .filter { it.categoryId == category.id && it.subcategory == oldName }
            .map { it.id }
        viewModelScope.launch {
            repository.updateSubcategoriesAndCascade(
                userId = currentUserId,
                categoryId = category.id,
                newSubcategories = updatedList,
                renamedFromTo = oldName to clean,
                affectedTransactionIds = affectedIds
            ).onSuccess { emitMessage("زیردسته و تراکنش‌های مرتبط ویرایش شدند") }
        }
    }

    fun deleteSubcategory(category: Category, subIndex: Int) {
        if (subIndex !in category.subcategories.indices) return
        val updatedList = category.subcategories.toMutableList().apply { removeAt(subIndex) }
        viewModelScope.launch {
            repository.updateSubcategoriesAndCascade(
                userId = currentUserId,
                categoryId = category.id,
                newSubcategories = updatedList,
                renamedFromTo = null,
                affectedTransactionIds = emptyList()
            ).onSuccess { emitMessage("زیردسته حذف شد") }
        }
    }

    fun addPerson(name: String, note: String, onCreated: (String) -> Unit = {}) {
        val clean = name.trim()
        if (clean.isEmpty()) {
            emitMessage("نام شخص را وارد کنید")
            return
        }
        val person = Person(
            id = "person-${System.currentTimeMillis()}",
            name = clean,
            note = note.trim()
        )
        viewModelScope.launch {
            repository.savePerson(currentUserId, person)
                .onSuccess { newId ->
                    emitMessage("«$clean» به لیست افراد اضافه شد")
                    onCreated(newId)
                }
                .onFailure { emitMessage("خطا در ثبت شخص") }
        }
    }

    fun deletePerson(personId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePerson(currentUserId, personId)
                .onSuccess {
                    emitMessage("شخص حذف شد")
                    onDone()
                }
                .onFailure { emitMessage("خطا در حذف شخص") }
        }
    }

    fun buildBackupJson(): String {
        val data = currentData()
        return BackupJsonCodec.buildBackupJson(
            BackupPayload(
                loans = data.loans,
                accounts = data.accounts,
                categories = data.categories,
                transactions = data.transactions,
                people = data.people
            )
        )
    }

    fun restoreFromJson(rawJson: String, onDone: () -> Unit) {
        val parsed = BackupJsonCodec.parseBackupJson(rawJson).getOrElse {
            emitMessage("فرمت فایل یا متن پشتیبان معتبر نیست")
            return
        }
        viewModelScope.launch {
            _controls.update { it.copy(isSyncBusy = true) }
            repository.replaceAllData(currentUserId, parsed)
                .onSuccess {
                    emitMessage("تمام اطلاعات با موفقیت بازیابی و در ابر ذخیره شدند!")
                    onDone()
                }
                .onFailure { emitMessage("خطا در بازیابی اطلاعات: ${it.localizedMessage ?: "نامشخص"}") }
            _controls.update { it.copy(isSyncBusy = false) }
        }
    }

    fun resetAllData(onDone: () -> Unit) {
        val cleanPayload = BackupPayload(
            loans = emptyList(),
            accounts = listOf(
                Account(
                    id = "acc-${System.currentTimeMillis()}",
                    name = "حساب اصلی",
                    initialBalance = 0L,
                    cardNumber = "",
                    color = "#0f7c8a"
                )
            ),
            categories = InitialSeedData.defaultCategories(),
            transactions = emptyList(),
            people = emptyList()
        )
        viewModelScope.launch {
            _controls.update { it.copy(isSyncBusy = true) }
            repository.replaceAllData(currentUserId, cleanPayload)
                .onSuccess {
                    emitMessage("تمام اطلاعات پاک شد")
                    onDone()
                }
                .onFailure { emitMessage("خطا در پاکسازی اطلاعات") }
            _controls.update { it.copy(isSyncBusy = false) }
        }
    }

    companion object {
        private const val TAG = "GhestYarVM"
    }
}
