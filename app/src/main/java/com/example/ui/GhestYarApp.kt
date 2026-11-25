package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.Account
import com.example.domain.FinanceCalculations
import com.example.domain.InstallmentItem
import com.example.domain.JalaliCalendar
import com.example.domain.Loan
import com.example.domain.Person
import com.example.domain.Transaction
import com.example.ui.components.VitalsCard
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.ForecastScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.MonthlyScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.sheets.AccountEditorSheet
import com.example.ui.sheets.AccountLedgerSheet
import com.example.ui.sheets.CategoryManagerSheet
import com.example.ui.sheets.LoanEditorSheet
import com.example.ui.sheets.LoanScheduleSheet
import com.example.ui.sheets.PayInstallmentSheet
import com.example.ui.sheets.PersonEditorSheet
import com.example.ui.sheets.PersonLedgerSheet
import com.example.ui.sheets.SettingsBackupSheet
import com.example.ui.sheets.TransactionDraftPreset
import com.example.ui.sheets.TransactionEditorSheet
import com.example.ui.theme.LocalGhestYarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ActiveSheet {
    data class LoanEditor(val loan: Loan? = null) : ActiveSheet
    data class PayInstallment(val item: InstallmentItem) : ActiveSheet
    data class LoanSchedule(val loanId: String) : ActiveSheet
    data class AccountEditor(val account: Account? = null) : ActiveSheet
    data class AccountLedger(val accountId: String) : ActiveSheet
    data class TransactionEditor(
        val tx: Transaction? = null,
        val preset: TransactionDraftPreset? = null
    ) : ActiveSheet
    data object CategoryManager : ActiveSheet
    data object PersonEditor : ActiveSheet
    data class PersonLedger(val personId: String) : ActiveSheet
    data object SettingsBackup : ActiveSheet
}

private sealed interface PendingDelete {
    data class LoanDelete(val loan: Loan) : PendingDelete
    data class AccountDelete(val account: Account) : PendingDelete
    data class TransactionDelete(val tx: Transaction) : PendingDelete
    data class PersonDelete(val person: Person) : PendingDelete
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhestYarApp(
    viewModel: GhestYarViewModel,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val cloudState by viewModel.cloudState.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val ext = LocalGhestYarColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val copyToClipboard: (String, String) -> Unit = remember(context, viewModel) {
        { value, toastMsg ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("GhestYar", value))
                viewModel.emitMessage(toastMsg)
            }
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.buildBackupJson()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                            stream.flush()
                        } ?: error("Output stream unavailable")
                    }.isSuccess
                }
                if (ok) {
                    viewModel.emitMessage("فایل پشتیبان با موفقیت ذخیره شد!")
                } else {
                    viewModel.emitMessage("خطا در ذخیره فایل پشتیبان")
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    }.getOrNull()
                }
                if (raw.isNullOrBlank()) {
                    viewModel.emitMessage("خطا در خواندن فایل انتخاب‌شده")
                } else {
                    viewModel.restoreFromJson(raw) {
                        activeSheet = null
                    }
                }
            }
        }
    }

    BackHandler(enabled = activeSheet != null || controls.activeTab != GhestYarTab.MONTHLY) {
        if (activeSheet != null) {
            activeSheet = null
        } else {
            viewModel.selectTab(GhestYarTab.MONTHLY)
        }
    }

    val currentBundle = (cloudState as? CloudSyncState.Ready)?.data ?: GhestYarDataBundle()

    val onFabClick: () -> Unit = {
        when (controls.activeTab) {
            GhestYarTab.MONTHLY, GhestYarTab.FORECAST, GhestYarTab.LOANS -> {
                activeSheet = ActiveSheet.LoanEditor(null)
            }
            GhestYarTab.ACCOUNTS -> {
                activeSheet = ActiveSheet.AccountEditor(null)
            }
            GhestYarTab.TRANSACTIONS -> {
                activeSheet = ActiveSheet.TransactionEditor(
                    tx = null,
                    preset = TransactionDraftPreset(initialType = "expense")
                )
            }
            GhestYarTab.DEBTS -> {
                activeSheet = ActiveSheet.PersonEditor
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.canvas)
    ) {
        val isWide = maxWidth >= 700.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GhestYarTab.entries.forEach { tab ->
                        val selected = controls.activeTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = ext.canvas,
                contentWindowInsets = if (isWide) WindowInsets.safeDrawing else WindowInsets.statusBars,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "قسط‌یار",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = JalaliCalendar.formatTodayHeader(controls.today),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_theme_button")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = if (isDarkTheme) "تم روشن" else "تم تیره",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { activeSheet = ActiveSheet.SettingsBackup },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("open_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "تنظیمات و پشتیبان‌گیری",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWide) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            GhestYarTab.entries.forEach { tab ->
                                val selected = controls.activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon(),
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.shortLabel(),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (cloudState is CloudSyncState.Ready) {
                        ExtendedFloatingActionButton(
                            onClick = onFabClick,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null
                                )
                            },
                            text = { Text(controls.activeTab.fabLabel) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("main_fab")
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (controls.isSyncBusy) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (val state = cloudState) {
                            CloudSyncState.Loading -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "در حال همگام‌سازی اطلاعات مالی با فضای ابری...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is CloudSyncState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VitalsCard(modifier = Modifier.widthIn(max = 440.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = "خطا در دریافت اطلاعات ابری",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is CloudSyncState.Ready -> {
                                val data = state.data
                                val contentWidthModifier = if (isWide) {
                                    Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 1120.dp)
                                } else {
                                    Modifier.fillMaxSize()
                                }

                                Box(modifier = contentWidthModifier) {
                                    when (controls.activeTab) {
                                        GhestYarTab.MONTHLY -> MonthlyScreen(
                                            loans = data.loans,
                                            today = controls.today,
                                            selectedYear = controls.selectedYear,
                                            selectedMonth = controls.selectedMonth,
                                            isWide = isWide,
                                            onChangeMonth = viewModel::changeSelectedMonth,
                                            onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                                            onPayInstallment = { item ->
                                                activeSheet = ActiveSheet.PayInstallment(item)
                                            },
                                            onUnpayInstallment = { item ->
                                                viewModel.unpayInstallment(item.loanId, item.index)
                                            },
                                            onCopyCard = { card ->
                                                copyToClipboard(card, "شماره کارت کپی شد!")
                                            }
                                        )

                                        GhestYarTab.FORECAST -> ForecastScreen(
                                            loans = data.loans,
                                            baseYear = controls.selectedYear,
                                            baseMonth = controls.selectedMonth,
                                            isWide = isWide,
                                            onSelectMonth = { y, m -> viewModel.openForecastMonth(y, m) }
                                        )

                                        GhestYarTab.LOANS -> LoansScreen(
                                            loans = data.loans,
                                            onEditLoan = { loan -> activeSheet = ActiveSheet.LoanEditor(loan) },
                                            onDeleteLoan = { loan -> pendingDelete = PendingDelete.LoanDelete(loan) },
                                            onOpenSchedule = { loan -> activeSheet = ActiveSheet.LoanSchedule(loan.id) }
                                        )

                                        GhestYarTab.ACCOUNTS -> AccountsScreen(
                                            accounts = data.accounts,
                                            transactions = data.transactions,
                                            onTransferClick = {
                                                activeSheet = ActiveSheet.TransactionEditor(
                                                    tx = null,
                                                    preset = TransactionDraftPreset(initialType = "transfer")
                                                )
                                            },
                                            onEditAccount = { acc -> activeSheet = ActiveSheet.AccountEditor(acc) },
                                            onDeleteAccount = { acc -> pendingDelete = PendingDelete.AccountDelete(acc) },
                                            onOpenAccountLedger = { acc -> activeSheet = ActiveSheet.AccountLedger(acc.id) },
                                            onCopyCard = { card ->
                                                copyToClipboard(card, "شماره کارت کپی شد!")
                                            }
                                        )

                                        GhestYarTab.TRANSACTIONS -> TransactionsScreen(
                                            transactions = data.transactions,
                                            accounts = data.accounts,
                                            categories = data.categories,
                                            people = data.people,
                                            selectedYear = controls.txSelectedYear,
                                            selectedMonth = controls.txSelectedMonth,
                                            filterType = controls.txFilterType,
                                            selectedCategories = controls.txSelectedCategories,
                                            selectedChartCategoryId = controls.selectedChartCategoryId,
                                            onChangeMonth = viewModel::changeTxMonth,
                                            onToggleCategoryFilter = viewModel::toggleTxCategoryFilter,
                                            onClearCategoryFilters = viewModel::clearTxCategoryFilters,
                                            onSelectFilterType = viewModel::setTxFilterType,
                                            onSelectChartCategory = viewModel::selectChartCategory,
                                            onNewExpense = {
                                                activeSheet = ActiveSheet.TransactionEditor(
                                                    tx = null,
                                                    preset = TransactionDraftPreset(initialType = "expense")
                                                )
                                            },
                                            onNewTransfer = {
                                                activeSheet = ActiveSheet.TransactionEditor(
                                                    tx = null,
                                                    preset = TransactionDraftPreset(initialType = "transfer")
                                                )
                                            },
                                            onOpenCategories = { activeSheet = ActiveSheet.CategoryManager },
                                            onEditTransaction = { tx ->
                                                activeSheet = ActiveSheet.TransactionEditor(tx = tx, preset = null)
                                            },
                                            onDeleteTransaction = { tx ->
                                                pendingDelete = PendingDelete.TransactionDelete(tx)
                                            }
                                        )

                                        GhestYarTab.DEBTS -> DebtsScreen(
                                            people = data.people,
                                            transactions = data.transactions,
                                            onAddPerson = { activeSheet = ActiveSheet.PersonEditor },
                                            onOpenPersonLedger = { person ->
                                                activeSheet = ActiveSheet.PersonLedger(person.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Active Bottom Sheets ---
    when (val sheet = activeSheet) {
        is ActiveSheet.LoanEditor -> {
            LoanEditorSheet(
                editingLoan = sheet.loan,
                today = controls.today,
                onDismiss = { activeSheet = null },
                onSave = { editId, title, source, amount, totalCount, paidCount, startDay, startMonth, startYear, card ->
                    viewModel.saveLoan(
                        editLoanId = editId,
                        title = title,
                        source = source,
                        amount = amount,
                        totalCount = totalCount,
                        paidCount = paidCount,
                        startDay = startDay,
                        startMonth = startMonth,
                        startYear = startYear,
                        card = card
                    ) {
                        activeSheet = null
                    }
                }
            )
        }

        is ActiveSheet.PayInstallment -> {
            PayInstallmentSheet(
                item = sheet.item,
                accounts = currentBundle.accounts,
                categories = currentBundle.categories,
                transactions = currentBundle.transactions,
                onDismiss = { activeSheet = null },
                onConfirmPay = { accId, catId, sub ->
                    viewModel.payInstallment(
                        loanId = sheet.item.loanId,
                        index = sheet.item.index,
                        accountId = accId,
                        categoryId = catId,
                        subcategory = sub
                    ) {
                        activeSheet = null
                    }
                }
            )
        }

        is ActiveSheet.LoanSchedule -> {
            val liveLoan = currentBundle.loans.find { it.id == sheet.loanId }
            if (liveLoan != null) {
                LoanScheduleSheet(
                    loan = liveLoan,
                    onDismiss = { activeSheet = null },
                    onPayClick = { inst ->
                        activeSheet = ActiveSheet.PayInstallment(inst)
                    },
                    onUnpayClick = { inst ->
                        viewModel.unpayInstallment(inst.loanId, inst.index)
                    }
                )
            } else {
                activeSheet = null
            }
        }

        is ActiveSheet.AccountEditor -> {
            AccountEditorSheet(
                editingAccount = sheet.account,
                onDismiss = { activeSheet = null },
                onSave = { editId, name, initialBalance, cardNumber, colorHex ->
                    viewModel.saveAccount(
                        editId = editId,
                        name = name,
                        initialBalance = initialBalance,
                        cardNumber = cardNumber,
                        colorHex = colorHex
                    ) {
                        activeSheet = null
                    }
                }
            )
        }

        is ActiveSheet.AccountLedger -> {
            val liveAccount = currentBundle.accounts.find { it.id == sheet.accountId }
            if (liveAccount != null) {
                AccountLedgerSheet(
                    account = liveAccount,
                    accounts = currentBundle.accounts,
                    categories = currentBundle.categories,
                    people = currentBundle.people,
                    transactions = currentBundle.transactions,
                    onDismiss = { activeSheet = null },
                    onEditTransaction = { tx ->
                        activeSheet = ActiveSheet.TransactionEditor(tx = tx, preset = null)
                    },
                    onDeleteTransaction = { tx ->
                        pendingDelete = PendingDelete.TransactionDelete(tx)
                    }
                )
            } else {
                activeSheet = null
            }
        }

        is ActiveSheet.TransactionEditor -> {
            TransactionEditorSheet(
                editingTx = sheet.tx,
                preset = sheet.preset,
                accounts = currentBundle.accounts,
                categories = currentBundle.categories,
                people = currentBundle.people,
                today = controls.today,
                onDismiss = { activeSheet = null },
                onQuickAddPerson = { name, onCreated ->
                    viewModel.addPerson(name = name, note = "", onCreated = onCreated)
                },
                onSave = { editId, type, amount, accountId, fromAccountId, toAccountId, categoryId, subcategory, personId, year, month, day, note ->
                    viewModel.saveTransaction(
                        editId = editId,
                        type = type,
                        amount = amount,
                        accountId = accountId,
                        fromAccountId = fromAccountId,
                        toAccountId = toAccountId,
                        categoryId = categoryId,
                        subcategory = subcategory,
                        personId = personId,
                        year = year,
                        month = month,
                        day = day,
                        note = note
                    ) {
                        activeSheet = null
                    }
                }
            )
        }

        ActiveSheet.CategoryManager -> {
            CategoryManagerSheet(
                categories = currentBundle.categories,
                onDismiss = { activeSheet = null },
                onAddCategory = viewModel::addCategory,
                onUpdateCategory = viewModel::updateCategory,
                onDeleteCategory = viewModel::deleteCategory,
                onAddSubcategory = viewModel::addSubcategory,
                onRenameSubcategory = viewModel::renameSubcategory,
                onDeleteSubcategory = viewModel::deleteSubcategory
            )
        }

        ActiveSheet.PersonEditor -> {
            PersonEditorSheet(
                onDismiss = { activeSheet = null },
                onSave = { name, note ->
                    viewModel.addPerson(name = name, note = note) {
                        activeSheet = null
                    }
                }
            )
        }

        is ActiveSheet.PersonLedger -> {
            val livePerson = currentBundle.people.find { it.id == sheet.personId }
            if (livePerson != null) {
                PersonLedgerSheet(
                    person = livePerson,
                    accounts = currentBundle.accounts,
                    categories = currentBundle.categories,
                    people = currentBundle.people,
                    transactions = currentBundle.transactions,
                    onDismiss = { activeSheet = null },
                    onDeletePerson = { p -> pendingDelete = PendingDelete.PersonDelete(p) },
                    onQuickLend = { p ->
                        val debtExpCat = currentBundle.categories.find {
                            it.type == "expense" && FinanceCalculations.isDebtRelatedCategory(it)
                        }
                        activeSheet = ActiveSheet.TransactionEditor(
                            tx = null,
                            preset = TransactionDraftPreset(
                                initialType = "expense",
                                initialPersonId = p.id,
                                initialCategoryId = debtExpCat?.id
                            )
                        )
                    },
                    onQuickBorrow = { p ->
                        val debtIncCat = currentBundle.categories.find {
                            it.type == "income" && FinanceCalculations.isDebtRelatedCategory(it)
                        }
                        activeSheet = ActiveSheet.TransactionEditor(
                            tx = null,
                            preset = TransactionDraftPreset(
                                initialType = "income",
                                initialPersonId = p.id,
                                initialCategoryId = debtIncCat?.id
                            )
                        )
                    },
                    onEditTransaction = { tx ->
                        activeSheet = ActiveSheet.TransactionEditor(tx = tx, preset = null)
                    },
                    onDeleteTransaction = { tx ->
                        pendingDelete = PendingDelete.TransactionDelete(tx)
                    }
                )
            } else {
                activeSheet = null
            }
        }

        ActiveSheet.SettingsBackup -> {
            SettingsBackupSheet(
                userEmail = userEmail,
                data = currentBundle,
                onDismiss = { activeSheet = null },
                onSignOut = {
                    activeSheet = null
                    onSignOut()
                },
                onExportFileClick = {
                    val today = controls.today
                    val fileName = "ghestyar-backup-${today.year}-${today.month}-${today.day}.json"
                    exportFileLauncher.launch(fileName)
                },
                onImportFileClick = {
                    importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onCopyJsonClick = {
                    copyToClipboard(viewModel.buildBackupJson(), "متن پشتیبان JSON کپی شد!")
                },
                onRestoreJsonText = { rawJson ->
                    viewModel.restoreFromJson(rawJson) {
                        activeSheet = null
                    }
                },
                onResetAllData = {
                    viewModel.resetAllData {
                        activeSheet = null
                    }
                }
            )
        }

        null -> Unit
    }

    // --- Destructive Confirmation Dialog ---
    when (val del = pendingDelete) {
        is PendingDelete.LoanDelete -> {
            ConfirmDeleteDialog(
                title = "حذف وام",
                message = "آیا از حذف وام «${del.loan.title}» مطمئن هستید؟",
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    viewModel.deleteLoan(del.loan.id)
                    pendingDelete = null
                }
            )
        }

        is PendingDelete.AccountDelete -> {
            ConfirmDeleteDialog(
                title = "حذف حساب",
                message = "آیا از حذف حساب «${del.account.name}» مطمئن هستید؟",
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    viewModel.deleteAccount(del.account.id)
                    if ((activeSheet as? ActiveSheet.AccountLedger)?.accountId == del.account.id) {
                        activeSheet = null
                    }
                    pendingDelete = null
                }
            )
        }

        is PendingDelete.TransactionDelete -> {
            ConfirmDeleteDialog(
                title = "حذف تراکنش",
                message = "آیا از حذف این تراکنش به مبلغ ${JalaliCalendar.formatMoney(del.tx.amount)} تومان مطمئن هستید؟",
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    viewModel.deleteTransaction(del.tx.id)
                    pendingDelete = null
                }
            )
        }

        is PendingDelete.PersonDelete -> {
            ConfirmDeleteDialog(
                title = "حذف شخص",
                message = "آیا از حذف «${del.person.name}» از لیست قرض و قوله مطمئن هستید؟",
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    viewModel.deletePerson(del.person.id) {
                        if ((activeSheet as? ActiveSheet.PersonLedger)?.personId == del.person.id) {
                            activeSheet = null
                        }
                    }
                    pendingDelete = null
                }
            )
        }

        null -> Unit
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("حذف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

private fun GhestYarTab.icon(): ImageVector = when (this) {
    GhestYarTab.MONTHLY -> Icons.AutoMirrored.Rounded.EventNote
    GhestYarTab.FORECAST -> Icons.Rounded.BarChart
    GhestYarTab.LOANS -> Icons.Rounded.AccountBalanceWallet
    GhestYarTab.ACCOUNTS -> Icons.Rounded.CreditCard
    GhestYarTab.TRANSACTIONS -> Icons.AutoMirrored.Rounded.ReceiptLong
    GhestYarTab.DEBTS -> Icons.Rounded.Group
}

private fun GhestYarTab.shortLabel(): String = when (this) {
    GhestYarTab.MONTHLY -> "اقساط"
    GhestYarTab.FORECAST -> "آینده"
    GhestYarTab.LOANS -> "وام‌ها"
    GhestYarTab.ACCOUNTS -> "حساب‌ها"
    GhestYarTab.TRANSACTIONS -> "تراکنش"
    GhestYarTab.DEBTS -> "قرض"
}
