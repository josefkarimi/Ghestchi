package com.example.domain

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BackupJsonCodec {

    private val validIdRegex = Regex("^[a-zA-Z0-9_-]{1,128}$")

    fun sanitizeId(raw: String?, prefix: String, fallbackIndex: Int): String {
        val trimmed = raw?.trim().orEmpty()
        if (validIdRegex.matches(trimmed)) return trimmed
        val cleaned = trimmed.replace(Regex("[^a-zA-Z0-9_-]"), "").take(100)
        return if (cleaned.isNotEmpty()) cleaned else "${prefix}-${System.currentTimeMillis()}-$fallbackIndex"
    }

    fun buildBackupJson(payload: BackupPayload): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val root = JSONObject().apply {
            put("app", "GhestYar")
            put("version", 4)
            put("exportedAt", isoFormat.format(Date()))
            put("categoriesInitialized", true)
            put("loans", JSONArray().apply {
                payload.loans.forEach { loan ->
                    put(JSONObject().apply {
                        put("id", loan.id)
                        put("title", loan.title)
                        put("source", loan.source)
                        put("amount", loan.amount)
                        put("totalCount", loan.totalCount)
                        put("startDay", loan.startDay)
                        put("startMonth", loan.startMonth)
                        put("startYear", loan.startYear)
                        put("card", loan.card)
                        put("paidIndices", JSONArray(loan.paidIndices))
                    })
                }
            })
            put("accounts", JSONArray().apply {
                payload.accounts.forEach { acc ->
                    put(JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("initialBalance", acc.initialBalance)
                        put("cardNumber", acc.cardNumber)
                        put("color", acc.color)
                    })
                }
            })
            put("categories", JSONArray().apply {
                payload.categories.forEach { cat ->
                    put(JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("type", cat.type)
                        put("emoji", cat.emoji)
                        put("color", cat.color)
                        put("subcategories", JSONArray(cat.subcategories))
                        if (cat.debtCategory) put("isDebtCategory", true)
                    })
                }
            })
            put("transactions", JSONArray().apply {
                payload.transactions.forEach { tx ->
                    put(JSONObject().apply {
                        put("id", tx.id)
                        put("type", tx.type)
                        put("amount", tx.amount)
                        if (!tx.accountId.isNullOrBlank()) put("accountId", tx.accountId) else put("accountId", JSONObject.NULL)
                        if (!tx.fromAccountId.isNullOrBlank()) put("fromAccountId", tx.fromAccountId)
                        if (!tx.toAccountId.isNullOrBlank()) put("toAccountId", tx.toAccountId)
                        if (!tx.categoryId.isNullOrBlank()) put("categoryId", tx.categoryId) else put("categoryId", JSONObject.NULL)
                        put("subcategory", tx.subcategory)
                        if (!tx.personId.isNullOrBlank()) put("personId", tx.personId) else put("personId", JSONObject.NULL)
                        put("year", tx.year)
                        put("month", tx.month)
                        put("day", tx.day)
                        put("note", tx.note)
                        if (!tx.linkedLoanId.isNullOrBlank()) put("linkedLoanId", tx.linkedLoanId)
                        if (tx.linkedInstallmentIndex != null) put("linkedInstallmentIndex", tx.linkedInstallmentIndex)
                    })
                }
            })
            put("people", JSONArray().apply {
                payload.people.forEach { person ->
                    put(JSONObject().apply {
                        put("id", person.id)
                        put("name", person.name)
                        put("note", person.note)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun parseBackupJson(rawJson: String): Result<BackupPayload> = runCatching {
        val root = JSONObject(rawJson.trim())
        val loans = mutableListOf<Loan>()
        val loansArr = root.optJSONArray("loans")
        if (loansArr != null) {
            for (i in 0 until loansArr.length()) {
                val obj = loansArr.optJSONObject(i) ?: continue
                val paidArr = obj.optJSONArray("paidIndices")
                val paidIndices = mutableListOf<Int>()
                if (paidArr != null) {
                    for (j in 0 until paidArr.length()) {
                        paidIndices.add(paidArr.optInt(j, 0))
                    }
                }
                val title = obj.optString("title", "").trim().take(120)
                val source = obj.optString("source", "").trim().take(100)
                val amount = obj.optLong("amount", 0L).coerceIn(1L, 1_000_000_000_000L)
                if (title.isEmpty() || source.isEmpty()) continue
                loans.add(
                    Loan(
                        id = sanitizeId(obj.optString("id"), "loan", i),
                        title = title,
                        source = source,
                        amount = amount,
                        totalCount = obj.optInt("totalCount", 1).coerceIn(1, 360),
                        startDay = obj.optInt("startDay", 1).coerceIn(1, 31),
                        startMonth = obj.optInt("startMonth", 1).coerceIn(1, 12),
                        startYear = obj.optInt("startYear", 1405).coerceIn(1350, 1500),
                        card = obj.optString("card", "").take(64),
                        paidIndices = paidIndices.distinct().take(360)
                    )
                )
            }
        }

        val accounts = mutableListOf<Account>()
        val accArr = root.optJSONArray("accounts")
        if (accArr != null) {
            for (i in 0 until accArr.length()) {
                val obj = accArr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim().take(100)
                if (name.isEmpty()) continue
                accounts.add(
                    Account(
                        id = sanitizeId(obj.optString("id"), "acc", i),
                        name = name,
                        initialBalance = obj.optLong("initialBalance", 0L).coerceIn(-1_000_000_000_000L, 1_000_000_000_000L),
                        cardNumber = obj.optString("cardNumber", "").take(64),
                        color = obj.optString("color", "#0f7c8a").ifBlank { "#0f7c8a" }.take(16)
                    )
                )
            }
        }

        val categories = mutableListOf<Category>()
        val catArr = root.optJSONArray("categories")
        if (catArr != null) {
            for (i in 0 until catArr.length()) {
                val obj = catArr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim().take(100)
                if (name.isEmpty()) continue
                val subArr = obj.optJSONArray("subcategories")
                val subs = mutableListOf<String>()
                if (subArr != null) {
                    for (j in 0 until subArr.length()) {
                        val s = subArr.optString(j, "").trim()
                        if (s.isNotEmpty()) subs.add(s.take(100))
                    }
                }
                val type = if (obj.optString("type") == "income") "income" else "expense"
                val debtFlag = obj.optBoolean("isDebtCategory", obj.optBoolean("debtCategory", false))
                categories.add(
                    Category(
                        id = sanitizeId(obj.optString("id"), "cat", i),
                        name = name,
                        type = type,
                        emoji = obj.optString("emoji", "📌").ifBlank { "📌" }.take(16),
                        color = obj.optString("color", "#0f7c8a").ifBlank { "#0f7c8a" }.take(16),
                        subcategories = subs.take(50),
                        debtCategory = debtFlag
                    )
                )
            }
        }

        val people = mutableListOf<Person>()
        val peopleArr = root.optJSONArray("people")
        if (peopleArr != null) {
            for (i in 0 until peopleArr.length()) {
                val obj = peopleArr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim().take(100)
                if (name.isEmpty()) continue
                people.add(
                    Person(
                        id = sanitizeId(obj.optString("id"), "person", i),
                        name = name,
                        note = obj.optString("note", "").take(300)
                    )
                )
            }
        }

        val transactions = mutableListOf<Transaction>()
        val txArr = root.optJSONArray("transactions")
        if (txArr != null) {
            for (i in 0 until txArr.length()) {
                val obj = txArr.optJSONObject(i) ?: continue
                val type = when (obj.optString("type")) {
                    "income" -> "income"
                    "transfer" -> "transfer"
                    else -> "expense"
                }
                val amount = obj.optLong("amount", 0L).coerceIn(1L, 1_000_000_000_000L)
                val accountId = obj.optNullableString("accountId")
                val fromAccountId = obj.optNullableString("fromAccountId")
                val toAccountId = obj.optNullableString("toAccountId")
                val categoryId = obj.optNullableString("categoryId")
                if (type == "transfer") {
                    if (fromAccountId.isNullOrBlank() || toAccountId.isNullOrBlank() || fromAccountId == toAccountId) continue
                } else {
                    if (accountId.isNullOrBlank() || categoryId.isNullOrBlank()) continue
                }
                val linkedIdx = if (obj.has("linkedInstallmentIndex") && !obj.isNull("linkedInstallmentIndex")) {
                    obj.optInt("linkedInstallmentIndex", 0).coerceIn(0, 360)
                } else null
                transactions.add(
                    Transaction(
                        id = sanitizeId(obj.optString("id"), "tx", i),
                        type = type,
                        amount = amount,
                        accountId = if (type == "transfer") null else accountId,
                        fromAccountId = if (type == "transfer") fromAccountId else null,
                        toAccountId = if (type == "transfer") toAccountId else null,
                        categoryId = if (type == "transfer") null else categoryId,
                        subcategory = obj.optString("subcategory", "").take(100),
                        personId = if (type == "transfer") null else obj.optNullableString("personId"),
                        year = obj.optInt("year", 1405).coerceIn(1350, 1500),
                        month = obj.optInt("month", 1).coerceIn(1, 12),
                        day = obj.optInt("day", 1).coerceIn(1, 31),
                        note = obj.optString("note", "").take(300),
                        linkedLoanId = obj.optNullableString("linkedLoanId"),
                        linkedInstallmentIndex = linkedIdx
                    )
                )
            }
        }

        BackupPayload(
            loans = loans,
            accounts = accounts,
            categories = if (categories.isNotEmpty()) categories else InitialSeedData.defaultCategories(),
            transactions = transactions,
            people = people
        )
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "").trim()
        return if (v.isEmpty() || v == "null") null else v.take(128)
    }
}
