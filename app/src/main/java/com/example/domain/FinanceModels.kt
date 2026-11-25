package com.example.domain

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserWorkspace(
    @DocumentId val id: String = "",
    val userId: String = "",
    val initialDataSeeded: Boolean = false,
    val categoriesInitialized: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Loan(
    @DocumentId val id: String = "",
    val title: String = "",
    val source: String = "",
    val amount: Long = 0L,
    val totalCount: Int = 1,
    val startDay: Int = 1,
    val startMonth: Int = 1,
    val startYear: Int = 1405,
    val card: String = "",
    val paidIndices: List<Int> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Account(
    @DocumentId val id: String = "",
    val name: String = "",
    val initialBalance: Long = 0L,
    val cardNumber: String = "",
    val color: String = "#0f7c8a",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = "expense", // "expense" | "income"
    val emoji: String = "📌",
    val color: String = "#0f7c8a",
    val subcategories: List<String> = emptyList(),
    val debtCategory: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val type: String = "expense", // "expense" | "income" | "transfer"
    val amount: Long = 0L,
    val accountId: String? = null,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val subcategory: String = "",
    val personId: String? = null,
    val year: Int = 1405,
    val month: Int = 1,
    val day: Int = 1,
    val note: String = "",
    val linkedLoanId: String? = null,
    val linkedInstallmentIndex: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Person(
    @DocumentId val id: String = "",
    val name: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Computed UI domain models
data class InstallmentItem(
    val loanId: String,
    val loanTitle: String,
    val loanSource: String,
    val loanCard: String,
    val amount: Long,
    val installmentNumber: Int,
    val totalCount: Int,
    val index: Int,
    val year: Int,
    val month: Int,
    val day: Int,
    val paid: Boolean
)

data class GlobalLoanSummary(
    val totalRemainingDebt: Long,
    val activeLoansCount: Int
)

data class EnrichedLoan(
    val loan: Loan,
    val paidCount: Int,
    val remainingCount: Int,
    val completed: Boolean,
    val remainingDebt: Long,
    val progressPercent: Int,
    val endDate: JalaliDate,
    val endKey: Int
)

data class ForecastMonth(
    val year: Int,
    val month: Int,
    val label: String,
    val yearLabel: String,
    val total: Long,
    val count: Int
)

data class CategoryExpenseSlice(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val amount: Long,
    val percent: Int
)

data class SubcategoryExpenseItem(
    val name: String,
    val amount: Long,
    val percent: Int
)

data class BackupPayload(
    val loans: List<Loan>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val people: List<Person>
)
