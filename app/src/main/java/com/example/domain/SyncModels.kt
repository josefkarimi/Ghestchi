package com.example.domain

import com.google.firebase.Timestamp

data class CollectionSnapshot<T>(
    val items: List<T>,
    val isFromCache: Boolean,
    val hasPendingWrites: Boolean
)

data class SectionSyncDiff(
    val sectionTitle: String,
    val localCount: Int,
    val serverCount: Int,
    val serverNewerCount: Int,
    val localNewerCount: Int,
    val identicalCount: Int
) {
    val differenceCount: Int get() = serverNewerCount + localNewerCount
}

data class ServerComparisonReport(
    val localPayload: BackupPayload,
    val serverPayload: BackupPayload,
    val sections: List<SectionSyncDiff>,
    val checkedAtMillis: Long
) {
    val totalServerNewer: Int get() = sections.sumOf { it.serverNewerCount }
    val totalLocalNewer: Int get() = sections.sumOf { it.localNewerCount }
    val totalDifferences: Int get() = totalServerNewer + totalLocalNewer
    val isInSync: Boolean get() = totalDifferences == 0
}

enum class SyncBadgeStatus {
    OFFLINE,
    CHECKING,
    PENDING_UPLOAD,
    SERVER_DIFF_FOUND,
    SYNCED
}

data class SyncStatusUiState(
    val isOnline: Boolean = true,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val isCheckingServer: Boolean = false,
    val isResolvingConflict: Boolean = false,
    val lastCheckedAtMillis: Long? = null,
    val lastCheckError: String? = null,
    val comparisonReport: ServerComparisonReport? = null
) {
    val badgeStatus: SyncBadgeStatus
        get() = when {
            isCheckingServer || isResolvingConflict -> SyncBadgeStatus.CHECKING
            !isOnline -> SyncBadgeStatus.OFFLINE
            comparisonReport != null && !comparisonReport.isInSync -> SyncBadgeStatus.SERVER_DIFF_FOUND
            hasPendingWrites || isFromCache -> SyncBadgeStatus.PENDING_UPLOAD
            else -> SyncBadgeStatus.SYNCED
        }
}

object SyncMergeEngine {

    fun compare(
        local: BackupPayload,
        server: BackupPayload,
        checkedAtMillis: Long = System.currentTimeMillis()
    ): ServerComparisonReport {
        val sections = listOf(
            compareCollection(
                title = "وام‌ها",
                localItems = local.loans,
                serverItems = server.loans,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> loanSame(a, b) }
            ),
            compareCollection(
                title = "حساب‌ها و کارت‌ها",
                localItems = local.accounts,
                serverItems = server.accounts,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> accountSame(a, b) }
            ),
            compareCollection(
                title = "دسته‌بندی‌ها",
                localItems = local.categories,
                serverItems = server.categories,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> categorySame(a, b) }
            ),
            compareCollection(
                title = "تراکنش‌ها",
                localItems = local.transactions,
                serverItems = server.transactions,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> txSame(a, b) }
            ),
            compareCollection(
                title = "افراد (قرض و قوله)",
                localItems = local.people,
                serverItems = server.people,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> personSame(a, b) }
            )
        )
        return ServerComparisonReport(
            localPayload = local,
            serverPayload = server,
            sections = sections,
            checkedAtMillis = checkedAtMillis
        )
    }

    fun smartMergeNewest(local: BackupPayload, server: BackupPayload): BackupPayload {
        return BackupPayload(
            loans = mergeCollection(
                local.loans,
                server.loans,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> loanSame(a, b) }
            ),
            accounts = mergeCollection(
                local.accounts,
                server.accounts,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> accountSame(a, b) }
            ),
            categories = mergeCollection(
                local.categories,
                server.categories,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> categorySame(a, b) }
            ),
            transactions = mergeCollection(
                local.transactions,
                server.transactions,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> txSame(a, b) }
            ),
            people = mergeCollection(
                local.people,
                server.people,
                idOf = { it.id },
                timestampOf = { tsMillis(it.updatedAt, it.createdAt) },
                sameContent = { a, b -> personSame(a, b) }
            )
        )
    }

    fun loanSame(a: Loan, b: Loan): Boolean =
        a.title.trim() == b.title.trim() &&
            a.source.trim() == b.source.trim() &&
            a.amount == b.amount &&
            a.totalCount == b.totalCount &&
            a.startDay == b.startDay &&
            a.startMonth == b.startMonth &&
            a.startYear == b.startYear &&
            a.card.trim() == b.card.trim() &&
            a.paidIndices.distinct().sorted() == b.paidIndices.distinct().sorted()

    fun accountSame(a: Account, b: Account): Boolean =
        a.name.trim() == b.name.trim() &&
            a.initialBalance == b.initialBalance &&
            a.cardNumber.trim() == b.cardNumber.trim() &&
            a.color.equals(b.color, ignoreCase = true)

    fun categorySame(a: Category, b: Category): Boolean =
        a.name.trim() == b.name.trim() &&
            a.type == b.type &&
            a.emoji == b.emoji &&
            a.color.equals(b.color, ignoreCase = true) &&
            a.subcategories == b.subcategories &&
            a.debtCategory == b.debtCategory

    fun txSame(a: Transaction, b: Transaction): Boolean =
        a.type == b.type &&
            a.amount == b.amount &&
            (a.accountId ?: "") == (b.accountId ?: "") &&
            (a.fromAccountId ?: "") == (b.fromAccountId ?: "") &&
            (a.toAccountId ?: "") == (b.toAccountId ?: "") &&
            (a.categoryId ?: "") == (b.categoryId ?: "") &&
            a.subcategory.trim() == b.subcategory.trim() &&
            (a.personId ?: "") == (b.personId ?: "") &&
            a.year == b.year &&
            a.month == b.month &&
            a.day == b.day &&
            a.note.trim() == b.note.trim() &&
            (a.linkedLoanId ?: "") == (b.linkedLoanId ?: "") &&
            a.linkedInstallmentIndex == b.linkedInstallmentIndex

    fun personSame(a: Person, b: Person): Boolean =
        a.name.trim() == b.name.trim() &&
            a.note.trim() == b.note.trim()

    private fun tsMillis(updatedAt: Timestamp?, createdAt: Timestamp?): Long {
        val ts = updatedAt ?: createdAt ?: return 0L
        return ts.seconds * 1000L + (ts.nanoseconds / 1_000_000L)
    }

    private fun <T> compareCollection(
        title: String,
        localItems: List<T>,
        serverItems: List<T>,
        idOf: (T) -> String,
        timestampOf: (T) -> Long,
        sameContent: (T, T) -> Boolean
    ): SectionSyncDiff {
        val localMap = localItems.associateBy(idOf)
        val serverMap = serverItems.associateBy(idOf)
        val allIds = (localMap.keys + serverMap.keys)

        var serverNewer = 0
        var localNewer = 0
        var identical = 0

        for (id in allIds) {
            val l = localMap[id]
            val s = serverMap[id]
            when {
                l == null && s != null -> serverNewer++
                l != null && s == null -> localNewer++
                l != null && s != null -> {
                    if (sameContent(l, s)) {
                        identical++
                    } else if (timestampOf(l) > timestampOf(s)) {
                        localNewer++
                    } else {
                        serverNewer++
                    }
                }
            }
        }

        return SectionSyncDiff(
            sectionTitle = title,
            localCount = localItems.size,
            serverCount = serverItems.size,
            serverNewerCount = serverNewer,
            localNewerCount = localNewer,
            identicalCount = identical
        )
    }

    private fun <T> mergeCollection(
        localItems: List<T>,
        serverItems: List<T>,
        idOf: (T) -> String,
        timestampOf: (T) -> Long,
        sameContent: (T, T) -> Boolean
    ): List<T> {
        val localMap = localItems.associateBy(idOf)
        val serverMap = serverItems.associateBy(idOf)
        val orderedIds = LinkedHashSet<String>().apply {
            addAll(serverMap.keys)
            addAll(localMap.keys)
        }
        val merged = ArrayList<T>(orderedIds.size)
        for (id in orderedIds) {
            val l = localMap[id]
            val s = serverMap[id]
            val chosen = when {
                l == null -> s!!
                s == null -> l
                sameContent(l, s) -> s
                timestampOf(l) > timestampOf(s) -> l
                else -> s
            }
            merged.add(chosen)
        }
        return merged
    }
}
