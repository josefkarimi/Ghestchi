package com.example.domain

import kotlin.math.roundToInt

object FinanceCalculations {

    fun getLoanSchedule(loan: Loan): List<InstallmentItem> {
        val paidSet = loan.paidIndices.toSet()
        return (0 until loan.totalCount).map { i ->
            val due = JalaliCalendar.addJalaliMonths(loan.startYear, loan.startMonth, loan.startDay, i)
            InstallmentItem(
                loanId = loan.id,
                loanTitle = loan.title,
                loanSource = loan.source,
                loanCard = loan.card,
                amount = loan.amount,
                installmentNumber = i + 1,
                totalCount = loan.totalCount,
                index = i,
                year = due.year,
                month = due.month,
                day = due.day,
                paid = paidSet.contains(i)
            )
        }
    }

    fun getInstallmentsForMonth(loans: List<Loan>, year: Int, month: Int): List<InstallmentItem> {
        return loans.flatMap { loan ->
            getLoanSchedule(loan).filter { it.year == year && it.month == month }
        }.sortedWith(compareBy<InstallmentItem> { it.paid }.thenBy { it.day })
    }

    fun getGlobalSummary(loans: List<Loan>): GlobalLoanSummary {
        var totalRemaining = 0L
        var activeCount = 0
        for (loan in loans) {
            val paidCount = loan.paidIndices.distinct().size
            val remainingCount = (loan.totalCount - paidCount).coerceAtLeast(0)
            totalRemaining += remainingCount * loan.amount
            if (remainingCount > 0) activeCount++
        }
        return GlobalLoanSummary(totalRemaining, activeCount)
    }

    fun getEnrichedLoans(loans: List<Loan>): List<EnrichedLoan> {
        return loans.map { loan ->
            val paidCount = loan.paidIndices.distinct().size.coerceAtMost(loan.totalCount)
            val remainingCount = (loan.totalCount - paidCount).coerceAtLeast(0)
            val completed = remainingCount == 0
            val endDate = JalaliCalendar.addJalaliMonths(
                loan.startYear,
                loan.startMonth,
                loan.startDay,
                (loan.totalCount - 1).coerceAtLeast(0)
            )
            val endKey = endDate.year * 10000 + endDate.month * 100 + endDate.day
            val pct = if (loan.totalCount > 0) ((paidCount.toDouble() / loan.totalCount) * 100.0).roundToInt() else 100
            EnrichedLoan(
                loan = loan,
                paidCount = paidCount,
                remainingCount = remainingCount,
                completed = completed,
                remainingDebt = remainingCount * loan.amount,
                progressPercent = pct,
                endDate = endDate,
                endKey = endKey
            )
        }.sortedWith(compareBy<EnrichedLoan> { it.completed }.thenBy { it.endKey })
    }

    fun get12MonthForecast(loans: List<Loan>, baseYear: Int, baseMonth: Int): List<ForecastMonth> {
        return (0 until 12).map { i ->
            val target = JalaliCalendar.addJalaliMonths(baseYear, baseMonth, 1, i)
            val items = getInstallmentsForMonth(loans, target.year, target.month)
            ForecastMonth(
                year = target.year,
                month = target.month,
                label = JalaliCalendar.monthName(target.month),
                yearLabel = JalaliCalendar.toPersianDigits(target.year),
                total = items.sumOf { it.amount },
                count = items.size
            )
        }
    }

    fun getAccountBalance(account: Account, transactions: List<Transaction>): Long {
        var balance = account.initialBalance
        for (tx in transactions) {
            when (tx.type) {
                "transfer" -> {
                    if (tx.fromAccountId == account.id) balance -= tx.amount
                    if (tx.toAccountId == account.id) balance += tx.amount
                }
                "income" -> if (tx.accountId == account.id) balance += tx.amount
                else -> if (tx.accountId == account.id) balance -= tx.amount
            }
        }
        return balance
    }

    fun getPersonNetBalance(personId: String, transactions: List<Transaction>): Long {
        var net = 0L
        for (tx in transactions) {
            if (tx.type != "transfer" && tx.personId == personId) {
                if (tx.type == "expense") net += tx.amount else net -= tx.amount
            }
        }
        return net
    }

    fun isDebtRelatedCategory(category: Category?): Boolean {
        if (category == null) return false
        return category.debtCategory ||
            category.name.contains("قرض") ||
            category.name.contains("طلب") ||
            category.name.contains("بدهی")
    }

    fun getCategoryExpenseSlices(
        expenseTransactions: List<Transaction>,
        categories: List<Category>
    ): List<CategoryExpenseSlice> {
        val totalExpense = expenseTransactions.sumOf { it.amount }
        if (totalExpense <= 0L) return emptyList()
        val totals = LinkedHashMap<String, Long>()
        for (tx in expenseTransactions) {
            val cid = tx.categoryId ?: "uncategorized"
            totals[cid] = (totals[cid] ?: 0L) + tx.amount
        }
        val catById = categories.associateBy { it.id }
        return totals.entries.map { (cid, amt) ->
            val cat = catById[cid]
            CategoryExpenseSlice(
                categoryId = cid,
                name = cat?.name ?: "سایر",
                emoji = cat?.emoji ?: "📦",
                colorHex = cat?.color ?: "#7b9198",
                amount = amt,
                percent = ((amt.toDouble() / totalExpense) * 100.0).roundToInt()
            )
        }.sortedByDescending { it.amount }
    }

    fun getSubcategoryBreakdown(
        expenseTransactions: List<Transaction>,
        categoryId: String
    ): List<SubcategoryExpenseItem> {
        val filtered = expenseTransactions.filter { (it.categoryId ?: "uncategorized") == categoryId }
        val total = filtered.sumOf { it.amount }
        if (total <= 0L) return emptyList()
        val map = LinkedHashMap<String, Long>()
        for (tx in filtered) {
            val key = tx.subcategory.ifBlank { "بدون زیردسته" }
            map[key] = (map[key] ?: 0L) + tx.amount
        }
        return map.entries.map { (name, amt) ->
            SubcategoryExpenseItem(
                name = name,
                amount = amt,
                percent = ((amt.toDouble() / total) * 100.0).roundToInt()
            )
        }.sortedByDescending { it.amount }
    }

    fun isInstallmentOverdue(item: InstallmentItem, today: JalaliDate): Boolean {
        if (item.paid) return false
        return item.year < today.year ||
            (item.year == today.year && item.month < today.month) ||
            (item.year == today.year && item.month == today.month && item.day < today.day)
    }

    fun isInstallmentToday(item: InstallmentItem, today: JalaliDate): Boolean {
        return item.year == today.year && item.month == today.month && item.day == today.day
    }
}
