package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Account
import com.example.domain.Category
import com.example.domain.FinanceCalculations
import com.example.domain.InstallmentItem
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.domain.Loan
import com.example.domain.Person
import com.example.domain.Transaction
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.JalaliDatePickerRow
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.components.VitalsDropdownSelector
import com.example.ui.theme.AccountColorPresets
import com.example.ui.theme.LocalGhestYarColors
import com.example.ui.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEditorSheet(
    editingLoan: Loan?,
    today: JalaliDate,
    onDismiss: () -> Unit,
    onSave: (
        editLoanId: String?,
        title: String,
        source: String,
        amount: Long,
        totalCount: Int,
        paidCount: Int,
        startDay: Int,
        startMonth: Int,
        startYear: Int,
        card: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(editingLoan) { mutableStateOf(editingLoan?.title ?: "") }
    var source by remember(editingLoan) { mutableStateOf(editingLoan?.source ?: "") }
    var amountText by remember(editingLoan) {
        mutableStateOf(
            if (editingLoan != null && editingLoan.amount > 0L) {
                JalaliCalendar.formatInputWithCommas(editingLoan.amount.toString())
            } else {
                ""
            }
        )
    }
    var totalCountText by remember(editingLoan) {
        mutableStateOf(editingLoan?.totalCount?.toString() ?: "12")
    }
    var paidCountText by remember(editingLoan) {
        mutableStateOf(editingLoan?.paidIndices?.size?.toString() ?: "0")
    }
    var startDay by remember(editingLoan) { mutableIntStateOf(editingLoan?.startDay ?: today.day) }
    var startMonth by remember(editingLoan) { mutableIntStateOf(editingLoan?.startMonth ?: today.month) }
    var startYear by remember(editingLoan) { mutableIntStateOf(editingLoan?.startYear ?: today.year) }
    var card by remember(editingLoan) { mutableStateOf(editingLoan?.card ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedTotal = JalaliCalendar.toEnglishDigits(totalCountText).toIntOrNull()?.coerceIn(1, 360) ?: 1
    val endDatePreview = remember(startYear, startMonth, startDay, parsedTotal) {
        JalaliCalendar.addJalaliMonths(startYear, startMonth, startDay, (parsedTotal - 1).coerceAtLeast(0))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = if (editingLoan == null) "ثبت وام یا قسط جدید" else "ویرایش وام",
                onDismiss = onDismiss
            )

            if (errorText != null) {
                SheetErrorBanner(errorText.orEmpty())
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorText = null },
                label = { Text("عنوان وام / قسط") },
                placeholder = { Text("مثلاً: وام مسکن، خرید لپ‌تاپ...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_title_input")
            )

            OutlinedTextField(
                value = source,
                onValueChange = { source = it; errorText = null },
                label = { Text("مرجع / بانک / اپلیکیشن") },
                placeholder = { Text("مثلاً: بلوبانک، دیجی‌پی، بانک مهر...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_source_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    amountText = JalaliCalendar.formatInputWithCommas(raw)
                    errorText = null
                },
                label = { Text("مبلغ هر قسط (تومان)") },
                placeholder = { Text("مثلاً: 2,500,000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_amount_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = totalCountText,
                    onValueChange = { raw ->
                        totalCountText = JalaliCalendar.toEnglishDigits(raw).filter { it.isDigit() }.take(3)
                        errorText = null
                    },
                    label = { Text("تعداد کل اقساط") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("loan_total_count_input")
                )
                OutlinedTextField(
                    value = paidCountText,
                    onValueChange = { raw ->
                        paidCountText = JalaliCalendar.toEnglishDigits(raw).filter { it.isDigit() }.take(3)
                        errorText = null
                    },
                    label = { Text("اقساط پرداخت‌شده") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("loan_paid_count_input")
                )
            }

            Text(
                text = "تاریخ سررسید اولین قسط (شمسی)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            JalaliDatePickerRow(
                day = startDay,
                month = startMonth,
                year = startYear,
                onDayChange = { startDay = it },
                onMonthChange = { startMonth = it },
                onYearChange = { startYear = it }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تاریخ تسویه آخرین قسط:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${JalaliCalendar.toPersianDigits(endDatePreview.day)} ${JalaliCalendar.monthName(endDatePreview.month)} ${JalaliCalendar.toPersianDigits(endDatePreview.year)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = card,
                onValueChange = { card = it },
                label = { Text("شماره کارت / حساب مقصد (اختیاری)") },
                placeholder = { Text("مثلاً: 6037-9975-...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_card_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val parsedAmount = JalaliCalendar.parseTomanAmount(amountText) ?: 0L
                    val total = JalaliCalendar.toEnglishDigits(totalCountText).toIntOrNull() ?: 0
                    val paid = JalaliCalendar.toEnglishDigits(paidCountText).toIntOrNull() ?: 0
                    when {
                        title.trim().isEmpty() -> errorText = "عنوان وام را وارد کنید."
                        source.trim().isEmpty() -> errorText = "نام بانک یا مرجع را وارد کنید."
                        parsedAmount <= 0L -> errorText = "مبلغ معتبر برای هر قسط وارد کنید."
                        total !in 1..360 -> errorText = "تعداد اقساط باید بین ۱ تا ۳۶۰ باشد."
                        paid < 0 || paid > total -> errorText = "تعداد اقساط پرداخت‌شده نمی‌تواند بیشتر از کل اقساط باشد."
                        else -> onSave(
                            editingLoan?.id,
                            title,
                            source,
                            parsedAmount,
                            total,
                            paid,
                            startDay,
                            startMonth,
                            startYear,
                            card
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_loan_button")
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (editingLoan == null) "ثبت وام جدید" else "ذخیره تغییرات")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayInstallmentSheet(
    item: InstallmentItem,
    accounts: List<Account>,
    categories: List<Category>,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onConfirmPay: (accountId: String?, categoryId: String?, subcategory: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseCategories = remember(categories) { categories.filter { it.type == "expense" } }
    val defaultCategory = remember(expenseCategories) {
        expenseCategories.find { it.id == "cat-installment" }
            ?: expenseCategories.find { it.name.contains("قسط") || it.name.contains("وام") }
            ?: expenseCategories.firstOrNull()
    }
    val balancesById = remember(accounts, transactions) {
        accounts.associate { it.id to FinanceCalculations.getAccountBalance(it, transactions) }
    }

    var selectedAccountId by remember(accounts) { mutableStateOf<String?>(accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember(defaultCategory) { mutableStateOf<String?>(defaultCategory?.id) }
    val selectedCategory = expenseCategories.find { it.id == selectedCategoryId }
    var selectedSubcategory by remember(selectedCategoryId) {
        mutableStateOf(selectedCategory?.subcategories?.firstOrNull() ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SheetHeader(title = "پرداخت قسط", onDismiss = onDismiss)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.loanTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${item.loanSource} • قسط ${JalaliCalendar.toPersianDigits(item.installmentNumber)} از ${JalaliCalendar.toPersianDigits(item.totalCount)} • سررسید ${JalaliCalendar.toPersianDigits(item.day)} ${JalaliCalendar.monthName(item.month)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "${JalaliCalendar.formatMoney(item.amount)} تومان",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val accountOptions = remember(accounts) { listOf<Account?>(null) + accounts }
            val selectedAccount = accounts.find { it.id == selectedAccountId }
            VitalsDropdownSelector(
                label = "کسر مبلغ از حساب",
                selectedText = if (selectedAccount == null) {
                    "بدون کسر از حساب (فقط ثبت پرداخت قسط)"
                } else {
                    val bal = balancesById[selectedAccount.id] ?: 0L
                    "${selectedAccount.name} (${JalaliCalendar.formatMoney(bal)} تومان)"
                },
                items = accountOptions,
                itemLabel = { acc ->
                    if (acc == null) {
                        "بدون کسر از حساب (فقط تیک پرداخت)"
                    } else {
                        val bal = balancesById[acc.id] ?: 0L
                        "${acc.name} (${JalaliCalendar.formatMoney(bal)} تومان)"
                    }
                },
                onSelect = { acc -> selectedAccountId = acc?.id }
            )

            if (selectedAccountId != null && expenseCategories.isNotEmpty()) {
                VitalsDropdownSelector(
                    label = "دسته‌بندی تراکنش هزینه",
                    selectedText = selectedCategory?.let { "${it.emoji} ${it.name}" } ?: "انتخاب دسته‌بندی",
                    items = expenseCategories,
                    itemLabel = { "${it.emoji} ${it.name}" },
                    onSelect = { cat ->
                        selectedCategoryId = cat.id
                        selectedSubcategory = cat.subcategories.firstOrNull() ?: ""
                    }
                )

                val subs = selectedCategory?.subcategories.orEmpty()
                if (subs.isNotEmpty()) {
                    val subOptions = listOf("") + subs
                    VitalsDropdownSelector(
                        label = "زیردسته (اختیاری)",
                        selectedText = selectedSubcategory.ifBlank { "بدون زیردسته" },
                        items = subOptions,
                        itemLabel = { it.ifBlank { "بدون زیردسته" } },
                        onSelect = { selectedSubcategory = it }
                    )
                }
            }

            Button(
                onClick = {
                    onConfirmPay(
                        selectedAccountId,
                        if (selectedAccountId != null) selectedCategoryId else null,
                        if (selectedAccountId != null) selectedSubcategory else ""
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_pay_installment_button")
            ) {
                Icon(imageVector = Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأیید و ثبت پرداخت قسط")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScheduleSheet(
    loan: Loan,
    onDismiss: () -> Unit,
    onPayClick: (InstallmentItem) -> Unit,
    onUnpayClick: (InstallmentItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalGhestYarColors.current
    val schedule = remember(loan) { FinanceCalculations.getLoanSchedule(loan) }
    val paidCount = loan.paidIndices.distinct().size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetHeader(title = "جدول اقساط «${loan.title}»", onDismiss = onDismiss)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${loan.source} • هر قسط ${JalaliCalendar.formatMoney(loan.amount)} تومان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                VitalsChip(
                    text = "${JalaliCalendar.toPersianDigits(paidCount)} از ${JalaliCalendar.toPersianDigits(loan.totalCount)} پرداخت‌شده",
                    tone = if (paidCount >= loan.totalCount) VitalsChipTone.POSITIVE else VitalsChipTone.INFO
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedule, key = { "${it.loanId}_${it.index}" }) { inst ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = if (inst.paid) ext.positiveBg.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "قسط ${JalaliCalendar.toPersianDigits(inst.installmentNumber)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${JalaliCalendar.toPersianDigits(inst.day)} ${JalaliCalendar.monthName(inst.month)} ${JalaliCalendar.toPersianDigits(inst.year)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (inst.paid) {
                                OutlinedButton(
                                    onClick = { onUnpayClick(inst) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("لغو", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                Button(
                                    onClick = { onPayClick(inst) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("پرداخت", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorSheet(
    editingAccount: Account?,
    onDismiss: () -> Unit,
    onSave: (
        editId: String?,
        name: String,
        initialBalance: Long,
        cardNumber: String,
        colorHex: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editingAccount) { mutableStateOf(editingAccount?.name ?: "") }
    var initialBalanceText by remember(editingAccount) {
        mutableStateOf(
            if (editingAccount != null) {
                val absFmt = JalaliCalendar.formatInputWithCommas(kotlin.math.abs(editingAccount.initialBalance).toString())
                if (editingAccount.initialBalance < 0L) "-$absFmt" else absFmt
            } else {
                "0"
            }
        )
    }
    var cardNumber by remember(editingAccount) { mutableStateOf(editingAccount?.cardNumber ?: "") }
    var selectedColor by remember(editingAccount) {
        mutableStateOf(editingAccount?.color?.ifBlank { AccountColorPresets.first() } ?: AccountColorPresets.first())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = if (editingAccount == null) "افزودن حساب یا کارت جدید" else "ویرایش حساب",
                onDismiss = onDismiss
            )

            if (errorText != null) {
                SheetErrorBanner(errorText.orEmpty())
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("نام حساب یا کارت") },
                placeholder = { Text("مثلاً: بلوبانک، بانک ملی، کیف پول...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_name_input")
            )

            OutlinedTextField(
                value = initialBalanceText,
                onValueChange = { raw ->
                    val eng = JalaliCalendar.toEnglishDigits(raw)
                    val neg = eng.trimStart().startsWith("-")
                    val fmt = JalaliCalendar.formatInputWithCommas(eng)
                    initialBalanceText = if (neg) "-$fmt" else fmt
                    errorText = null
                },
                label = { Text("موجودی اولیه (تومان - منفی مجاز)") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_balance_input")
            )

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("شماره کارت (اختیاری)") },
                placeholder = { Text("مثلاً: 6219-8619-...") },
                leadingIcon = { Icon(Icons.Rounded.CreditCard, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_card_input")
            )

            Text(
                text = "رنگ کارت حساب",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ColorSwatchRow(
                colors = AccountColorPresets,
                selectedHex = selectedColor,
                onSelect = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val parsedBalance = JalaliCalendar.parseSignedTomanAmount(initialBalanceText)
                    when {
                        name.trim().isEmpty() -> errorText = "لطفاً نام حساب را وارد کنید."
                        parsedBalance == null -> errorText = "مقدار موجودی اولیه معتبر نیست."
                        else -> onSave(editingAccount?.id, name, parsedBalance, cardNumber, selectedColor)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_account_button")
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (editingAccount == null) "افزودن حساب" else "ذخیره تغییرات")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountLedgerSheet(
    account: Account,
    accounts: List<Account>,
    categories: List<Category>,
    people: List<Person>,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalGhestYarColors.current
    val balance = remember(account, transactions) {
        FinanceCalculations.getAccountBalance(account, transactions)
    }
    val accountTxs = remember(account.id, transactions) {
        transactions.filter { tx ->
            if (tx.type == "transfer") {
                tx.fromAccountId == account.id || tx.toAccountId == account.id
            } else {
                tx.accountId == account.id
            }
        }.sortedWith(
            compareByDescending<Transaction> { it.year }
                .thenByDescending { it.month }
                .thenByDescending { it.day }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )
    }
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val peopleById = remember(people) { people.associateBy { it.id } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetHeader(title = "گردش حساب «${account.name}»", onDismiss = onDismiss)

            StatusAccentCard(accentColor = parseHexColor(account.color, MaterialTheme.colorScheme.primary)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "موجودی فعلی",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${JalaliCalendar.formatMoney(balance)} تومان",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balance < 0L) MaterialTheme.colorScheme.error else ext.positive
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "موجودی اولیه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${JalaliCalendar.formatMoney(account.initialBalance)} تومان",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (accountTxs.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.AccountBalance,
                    title = "تراکنشی برای این حساب ثبت نشده است",
                    subtitle = "تراکنش‌ها و انتقال‌های مرتبط با این حساب در اینجا نمایش داده می‌شوند."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accountTxs, key = { it.id }) { tx ->
                        TransactionRowCard(
                            tx = tx,
                            accountsById = accountsById,
                            categoriesById = categoriesById,
                            peopleById = peopleById,
                            contextAccountId = account.id,
                            onEdit = { onEditTransaction(tx) },
                            onDelete = { onDeleteTransaction(tx) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun SheetHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "بستن",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SheetErrorBanner(message: String) {
    val ext = LocalGhestYarColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = ext.errorBg
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}
