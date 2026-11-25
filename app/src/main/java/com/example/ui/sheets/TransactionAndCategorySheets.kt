package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Account
import com.example.domain.Category
import com.example.domain.FinanceCalculations
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.domain.Person
import com.example.domain.SyncStatusUiState
import com.example.domain.Transaction
import com.example.ui.GhestYarDataBundle
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.JalaliDatePickerRow
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.VitalsCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.components.VitalsDropdownSelector
import com.example.ui.theme.CategoryColorPresets
import com.example.ui.theme.LocalGhestYarColors
import com.example.ui.theme.parseHexColor
import kotlin.math.abs

data class TransactionDraftPreset(
    val initialType: String = "expense",
    val initialPersonId: String? = null,
    val initialCategoryId: String? = null,
    val initialNote: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorSheet(
    editingTx: Transaction?,
    preset: TransactionDraftPreset?,
    accounts: List<Account>,
    categories: List<Category>,
    people: List<Person>,
    today: JalaliDate,
    onDismiss: () -> Unit,
    onQuickAddPerson: (name: String, onCreated: (String) -> Unit) -> Unit,
    onSave: (
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
        note: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialType = editingTx?.type ?: preset?.initialType ?: "expense"

    var txType by remember(editingTx, preset) { mutableStateOf(initialType) }
    var amountText by remember(editingTx, preset) {
        mutableStateOf(
            if (editingTx != null && editingTx.amount > 0L) {
                JalaliCalendar.formatInputWithCommas(editingTx.amount.toString())
            } else {
                ""
            }
        )
    }
    var accountId by remember(editingTx, preset, accounts) {
        mutableStateOf(editingTx?.accountId ?: accounts.firstOrNull()?.id)
    }
    var fromAccountId by remember(editingTx, preset, accounts) {
        mutableStateOf(editingTx?.fromAccountId ?: accounts.firstOrNull()?.id)
    }
    var toAccountId by remember(editingTx, preset, accounts) {
        mutableStateOf(editingTx?.toAccountId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id)
    }

    val matchingCategories = remember(categories, txType) {
        if (txType == "transfer") emptyList() else categories.filter { it.type == txType }
    }

    var categoryId by remember(editingTx, preset) {
        mutableStateOf(editingTx?.categoryId ?: preset?.initialCategoryId)
    }
    var subcategory by remember(editingTx, preset) {
        mutableStateOf(editingTx?.subcategory ?: "")
    }
    var personId by remember(editingTx, preset) {
        mutableStateOf(editingTx?.personId ?: preset?.initialPersonId)
    }
    var day by remember(editingTx) { mutableIntStateOf(editingTx?.day ?: today.day) }
    var month by remember(editingTx) { mutableIntStateOf(editingTx?.month ?: today.month) }
    var year by remember(editingTx) { mutableIntStateOf(editingTx?.year ?: today.year) }
    var note by remember(editingTx, preset) {
        mutableStateOf(editingTx?.note ?: preset?.initialNote ?: "")
    }

    var showInlineAddPerson by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(txType, matchingCategories) {
        if (txType != "transfer") {
            val stillValid = matchingCategories.any { it.id == categoryId }
            if (!stillValid) {
                val debtPreferred = if (personId != null) {
                    matchingCategories.find { FinanceCalculations.isDebtRelatedCategory(it) }
                } else {
                    null
                }
                val fallback = debtPreferred ?: matchingCategories.firstOrNull()
                categoryId = fallback?.id
                subcategory = ""
            }
        }
    }

    val selectedCategory = matchingCategories.find { it.id == categoryId }
    val showPersonPicker = txType != "transfer" && (
        FinanceCalculations.isDebtRelatedCategory(selectedCategory) || !personId.isNullOrBlank()
    )

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
                title = if (editingTx == null) "ثبت تراکنش جدید" else "ویرایش تراکنش",
                onDismiss = onDismiss
            )

            if (errorText != null) {
                SheetErrorBanner(errorText.orEmpty())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    "expense" to "هزینه",
                    "income" to "درآمد",
                    "transfer" to "انتقال"
                )
                types.forEach { (raw, label) ->
                    FilterChip(
                        selected = txType == raw,
                        onClick = {
                            txType = raw
                            errorText = null
                        },
                        label = { Text(label) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_type_$raw")
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    amountText = JalaliCalendar.formatInputWithCommas(raw)
                    errorText = null
                },
                label = { Text("مبلغ (تومان)") },
                placeholder = { Text("مثلاً: 450,000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            if (txType == "transfer") {
                val fromAcc = accounts.find { it.id == fromAccountId }
                val toAcc = accounts.find { it.id == toAccountId }
                VitalsDropdownSelector(
                    label = "از حساب (مبدأ)",
                    selectedText = fromAcc?.name ?: "انتخاب حساب مبدأ",
                    items = accounts,
                    itemLabel = { it.name },
                    onSelect = { fromAccountId = it.id; errorText = null }
                )
                VitalsDropdownSelector(
                    label = "به حساب (مقصد)",
                    selectedText = toAcc?.name ?: "انتخاب حساب مقصد",
                    items = accounts,
                    itemLabel = { it.name },
                    onSelect = { toAccountId = it.id; errorText = null }
                )
            } else {
                val selectedAcc = accounts.find { it.id == accountId }
                VitalsDropdownSelector(
                    label = "حساب / کارت",
                    selectedText = selectedAcc?.name ?: "انتخاب حساب",
                    items = accounts,
                    itemLabel = { it.name },
                    onSelect = { accountId = it.id; errorText = null }
                )

                VitalsDropdownSelector(
                    label = "دسته‌بندی",
                    selectedText = selectedCategory?.let { "${it.emoji} ${it.name}" } ?: "انتخاب دسته‌بندی",
                    items = matchingCategories,
                    itemLabel = { "${it.emoji} ${it.name}" },
                    onSelect = { cat ->
                        categoryId = cat.id
                        subcategory = ""
                        if (!FinanceCalculations.isDebtRelatedCategory(cat)) {
                            personId = null
                        }
                        errorText = null
                    }
                )

                val subs = selectedCategory?.subcategories.orEmpty()
                if (subs.isNotEmpty()) {
                    val subOptions = listOf("") + subs
                    VitalsDropdownSelector(
                        label = "زیردسته (اختیاری)",
                        selectedText = subcategory.ifBlank { "بدون زیردسته" },
                        items = subOptions,
                        itemLabel = { it.ifBlank { "بدون زیردسته" } },
                        onSelect = { subcategory = it }
                    )
                }

                if (showPersonPicker) {
                    val personOptions = listOf<Person?>(null) + people
                    val selectedPerson = people.find { it.id == personId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        VitalsDropdownSelector(
                            label = "شخص مرتبط (قرض و قوله)",
                            selectedText = selectedPerson?.name ?: "انتخاب شخص",
                            items = personOptions,
                            itemLabel = { it?.name ?: "بدون شخص" },
                            onSelect = { personId = it?.id },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showInlineAddPerson = !showInlineAddPerson },
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("جدید")
                        }
                    }

                    if (showInlineAddPerson) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPersonName,
                                onValueChange = { newPersonName = it },
                                label = { Text("نام شخص جدید") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clean = newPersonName.trim()
                                    if (clean.isNotEmpty()) {
                                        onQuickAddPerson(clean) { createdId ->
                                            personId = createdId
                                            newPersonName = ""
                                            showInlineAddPerson = false
                                        }
                                    }
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("ثبت")
                            }
                        }
                    }
                }
            }

            Text(
                text = "تاریخ تراکنش (شمسی)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            JalaliDatePickerRow(
                day = day,
                month = month,
                year = year,
                onDayChange = { day = it },
                onMonthChange = { month = it },
                onYearChange = { year = it }
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("توضیحات (اختیاری)") },
                placeholder = { Text("توضیح کوتاه برای یادآوری...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val parsedAmount = JalaliCalendar.parseTomanAmount(amountText) ?: 0L
                    when {
                        parsedAmount <= 0L -> errorText = "لطفاً مبلغ معتبر وارد کنید."
                        txType == "transfer" && (fromAccountId.isNullOrBlank() || toAccountId.isNullOrBlank()) ->
                            errorText = "حساب مبدأ و مقصد را انتخاب کنید."
                        txType == "transfer" && fromAccountId == toAccountId ->
                            errorText = "حساب مبدأ و مقصد نمی‌توانند یکسان باشند."
                        txType != "transfer" && accountId.isNullOrBlank() ->
                            errorText = "ابتدا یک حساب انتخاب کنید."
                        txType != "transfer" && categoryId.isNullOrBlank() ->
                            errorText = "دسته‌بندی تراکنش را انتخاب کنید."
                        else -> onSave(
                            editingTx?.id,
                            txType,
                            parsedAmount,
                            if (txType == "transfer") null else accountId,
                            if (txType == "transfer") fromAccountId else null,
                            if (txType == "transfer") toAccountId else null,
                            if (txType == "transfer") null else categoryId,
                            if (txType == "transfer") "" else subcategory,
                            if (txType == "transfer") null else personId,
                            year,
                            month,
                            day,
                            note
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_tx_button")
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (editingTx == null) "ثبت تراکنش" else "ذخیره تغییرات")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryManagerSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddCategory: (name: String, type: String, emoji: String, colorHex: String, rawSubs: String) -> Unit,
    onUpdateCategory: (category: Category, newName: String, newEmoji: String, newColor: String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddSubcategory: (Category, String) -> Unit,
    onRenameSubcategory: (Category, Int, String) -> Unit,
    onDeleteSubcategory: (Category, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newType by remember { mutableStateOf("expense") }
    var newName by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("📌") }
    var newColor by remember { mutableStateOf(CategoryColorPresets.first()) }
    var newSubs by remember { mutableStateOf("") }

    var editingCatId by remember { mutableStateOf<String?>(null) }
    var editCatName by remember { mutableStateOf("") }
    var editCatEmoji by remember { mutableStateOf("") }
    var editCatColor by remember { mutableStateOf(CategoryColorPresets.first()) }

    var addingSubCatId by remember { mutableStateOf<String?>(null) }
    var addingSubText by remember { mutableStateOf("") }

    var renamingSubKey by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var renamingSubText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SheetHeader(title = "مدیریت دسته‌بندی‌ها و زیردسته‌ها", onDismiss = onDismiss)
            }

            item {
                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "افزودن دسته‌بندی جدید",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newType == "expense",
                            onClick = { newType = "expense" },
                            label = { Text("هزینه") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = newType == "income",
                            onClick = { newType = "income" },
                            label = { Text("درآمد") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newEmoji,
                            onValueChange = { newEmoji = it.take(4) },
                            label = { Text("آیکون") },
                            singleLine = true,
                            modifier = Modifier.width(84.dp)
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("نام دسته‌بندی") },
                            placeholder = { Text("مثلاً: سفر، ورزش...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_category_name_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSubs,
                        onValueChange = { newSubs = it },
                        label = { Text("زیردسته‌ها (با ویرگول جدا کنید)") },
                        placeholder = { Text("مثلاً: بلیط، هتل، غذا") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ColorSwatchRow(
                        colors = CategoryColorPresets,
                        selectedHex = newColor,
                        onSelect = { newColor = it }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newName.trim().isNotEmpty()) {
                                onAddCategory(newName, newType, newEmoji, newColor, newSubs)
                                newName = ""
                                newEmoji = "📌"
                                newSubs = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_category_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن دسته‌بندی")
                    }
                }
            }

            item {
                Text(
                    text = "دسته‌بندی‌های موجود (${JalaliCalendar.toPersianDigits(categories.size)})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(categories, key = { it.id }) { cat ->
                val catColor = parseHexColor(cat.color, MaterialTheme.colorScheme.primary)
                val isEditingThis = editingCatId == cat.id
                VitalsCard(modifier = Modifier.fillMaxWidth()) {
                    if (isEditingThis) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editCatEmoji,
                                    onValueChange = { editCatEmoji = it.take(4) },
                                    label = { Text("آیکون") },
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )
                                OutlinedTextField(
                                    value = editCatName,
                                    onValueChange = { editCatName = it },
                                    label = { Text("نام دسته‌بندی") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            ColorSwatchRow(
                                colors = CategoryColorPresets,
                                selectedHex = editCatColor,
                                onSelect = { editCatColor = it }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { editingCatId = null }) { Text("انصراف") }
                                Button(
                                    onClick = {
                                        onUpdateCategory(cat, editCatName, editCatEmoji, editCatColor)
                                        editingCatId = null
                                    }
                                ) { Text("ذخیره") }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(catColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = cat.emoji, style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (cat.type == "income") "درآمد" else "هزینه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingCatId = cat.id
                                    editCatName = cat.name
                                    editCatEmoji = cat.emoji
                                    editCatColor = cat.color
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "ویرایش دسته‌بندی", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onDeleteCategory(cat.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف دسته‌بندی",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (cat.subcategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cat.subcategories.forEachIndexed { idx, sub ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = sub,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = {
                                                    renamingSubKey = cat.id to idx
                                                    renamingSubText = sub
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Edit,
                                                    contentDescription = "ویرایش زیردسته",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteSubcategory(cat, idx) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "حذف زیردسته",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (renamingSubKey?.first == cat.id) {
                            val idx = renamingSubKey!!.second
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = renamingSubText,
                                    onValueChange = { renamingSubText = it },
                                    label = { Text("نام جدید زیردسته") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        onRenameSubcategory(cat, idx, renamingSubText)
                                        renamingSubKey = null
                                    }
                                ) { Text("ثبت") }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        if (addingSubCatId == cat.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = addingSubText,
                                    onValueChange = { addingSubText = it },
                                    label = { Text("زیردسته جدید") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (addingSubText.trim().isNotEmpty()) {
                                            onAddSubcategory(cat, addingSubText)
                                            addingSubText = ""
                                            addingSubCatId = null
                                        }
                                    }
                                ) { Text("افزودن") }
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    addingSubCatId = cat.id
                                    addingSubText = ""
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن زیردسته", style = MaterialTheme.typography.labelMedium)
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
fun PersonEditorSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(title = "افزودن شخص جدید (قرض و قوله)", onDismiss = onDismiss)

            if (errorText != null) {
                SheetErrorBanner(errorText.orEmpty())
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("نام و نام خانوادگی / عنوان") },
                placeholder = { Text("مثلاً: علی، همکار، برادر...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("person_name_input")
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("توضیحات یا شماره کارت (اختیاری)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("person_note_input")
            )

            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorText = "نام شخص را وارد کنید."
                    } else {
                        onSave(name, note)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_person_button")
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ثبت شخص")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonLedgerSheet(
    person: Person,
    accounts: List<Account>,
    categories: List<Category>,
    people: List<Person>,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onDeletePerson: (Person) -> Unit,
    onQuickLend: (Person) -> Unit,
    onQuickBorrow: (Person) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalGhestYarColors.current
    val net = remember(person.id, transactions) {
        FinanceCalculations.getPersonNetBalance(person.id, transactions)
    }
    val personTxs = remember(person.id, transactions) {
        transactions.filter { it.type != "transfer" && it.personId == person.id }
            .sortedWith(
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
            SheetHeader(title = "حساب قرض و قوله «${person.name}»", onDismiss = onDismiss)

            val accent = when {
                net > 0L -> ext.positive
                net < 0L -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            }
            val statusLabel = when {
                net > 0L -> "طلب شما از ${person.name}"
                net < 0L -> "بدهی شما به ${person.name}"
                else -> "حساب تسویه شده است"
            }

            StatusAccentCard(accentColor = accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${JalaliCalendar.formatMoney(abs(net))} تومان",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                        if (person.note.isNotBlank()) {
                            Text(
                                text = person.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onDeletePerson(person) }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف شخص",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onQuickLend(person) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("quick_lend_button")
                ) {
                    Text("+ قرض دادن / بازپرداخت", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { onQuickBorrow(person) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("quick_borrow_button")
                ) {
                    Text("+ قرض گرفتن / دریافت", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (personTxs.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Group,
                    title = "تراکنشی با این شخص ثبت نشده است",
                    subtitle = "از دکمه‌های بالا برای ثبت قرض دادن، قرض گرفتن یا تسویه استفاده کنید."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(personTxs, key = { it.id }) { tx ->
                        TransactionRowCard(
                            tx = tx,
                            accountsById = accountsById,
                            categoriesById = categoriesById,
                            peopleById = peopleById,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupSheet(
    userEmail: String?,
    data: GhestYarDataBundle,
    syncStatus: SyncStatusUiState,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSyncSheet: () -> Unit,
    onExportFileClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onCopyJsonClick: () -> Unit,
    onRestoreJsonText: (String) -> Unit,
    onResetAllData: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalGhestYarColors.current
    var showPasteBox by remember { mutableStateOf(false) }
    var pastedJson by remember { mutableStateOf("") }
    var confirmResetStep by remember { mutableStateOf(false) }

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
            SheetHeader(title = "تنظیمات، حساب ابری و پشتیبان‌گیری", onDismiss = onDismiss)

            // Cloud Account Card
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (syncStatus.isOnline) Icons.Rounded.CloudDone else Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = if (syncStatus.isOnline) ext.positive else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (syncStatus.isOnline) "حساب ابری متصل است" else "حالت آفلاین (بدون اینترنت)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userEmail ?: "کاربر گوگل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("خروج")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenSyncSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_open_sync_button")
                ) {
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بررسی و مقایسه داده‌ها با سرور")
                }
            }

            // Live Counts Summary
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "آمار داده‌های همگام‌شده شما",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VitalsChip(
                        text = "${JalaliCalendar.toPersianDigits(data.loans.size)} وام",
                        tone = VitalsChipTone.INFO
                    )
                    VitalsChip(
                        text = "${JalaliCalendar.toPersianDigits(data.accounts.size)} حساب",
                        tone = VitalsChipTone.POSITIVE
                    )
                    VitalsChip(
                        text = "${JalaliCalendar.toPersianDigits(data.transactions.size)} تراکنش",
                        tone = VitalsChipTone.NEUTRAL
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VitalsChip(
                        text = "${JalaliCalendar.toPersianDigits(data.categories.size)} دسته‌بندی",
                        tone = VitalsChipTone.NEUTRAL
                    )
                    VitalsChip(
                        text = "${JalaliCalendar.toPersianDigits(data.people.size)} شخص",
                        tone = VitalsChipTone.WARN
                    )
                }
            }

            // Backup & Restore Actions
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "پشتیبان‌گیری و بازیابی (سازگار با نسخه وب)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onExportFileClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_json_file_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ذخیره فایل پشتیبان (JSON)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onImportFileClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_json_file_button")
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بازیابی از فایل پشتیبان (JSON)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyJsonClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_json_button")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کپی متن JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showPasteBox = !showPasteBox },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_paste_json_button")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جای‌گذاری JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (showPasteBox) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pastedJson,
                        onValueChange = { pastedJson = it },
                        label = { Text("متن JSON پشتیبان را اینجا جای‌گذاری کنید") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_json_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (pastedJson.isNotBlank()) {
                                onRestoreJsonText(pastedJson)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_paste_restore_button")
                    ) {
                        Text("بازیابی اطلاعات از متن بالا")
                    }
                }
            }

            // Danger Zone: Reset All Data
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (!confirmResetStep) {
                OutlinedButton(
                    onClick = { confirmResetStep = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_all_data_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پاکسازی کامل تمام اطلاعات")
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = ext.errorBg
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "آیا مطمئن هستید؟ تمام وام‌ها، تراکنش‌ها و افراد از فضای ابری پاک می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { confirmResetStep = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("انصراف")
                            }
                            Button(
                                onClick = onResetAllData,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("confirm_reset_all_data_button")
                            ) {
                                Text("بله، پاک کن")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
