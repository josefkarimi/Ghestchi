package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.Account
import com.example.domain.Category
import com.example.domain.CategoryExpenseSlice
import com.example.domain.FinanceCalculations
import com.example.domain.JalaliCalendar
import com.example.domain.Person
import com.example.domain.SubcategoryExpenseItem
import com.example.domain.Transaction
import com.example.ui.TransactionFilterType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.VitalsCard
import com.example.ui.theme.LocalGhestYarColors
import com.example.ui.theme.parseHexColor

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
    people: List<Person>,
    selectedYear: Int,
    selectedMonth: Int,
    filterType: TransactionFilterType,
    selectedCategories: Set<String>,
    selectedChartCategoryId: String?,
    onChangeMonth: (Int) -> Unit,
    onToggleCategoryFilter: (String) -> Unit,
    onClearCategoryFilters: () -> Unit,
    onSelectFilterType: (TransactionFilterType) -> Unit,
    onSelectChartCategory: (String?) -> Unit,
    onNewExpense: () -> Unit,
    onNewTransfer: () -> Unit,
    onOpenCategories: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val ext = LocalGhestYarColors.current
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val peopleById = remember(people) { people.associateBy { it.id } }

    val monthTxs = remember(transactions, selectedYear, selectedMonth) {
        transactions.filter { it.year == selectedYear && it.month == selectedMonth }
    }
    val nonTransfer = remember(monthTxs, selectedCategories) {
        monthTxs.filter { tx ->
            tx.type != "transfer" && (selectedCategories.isEmpty() || tx.categoryId in selectedCategories)
        }
    }
    val totalIncome = remember(nonTransfer) { nonTransfer.filter { it.type == "income" }.sumOf { it.amount } }
    val expenseTxs = remember(nonTransfer) { nonTransfer.filter { it.type == "expense" } }
    val totalExpense = remember(expenseTxs) { expenseTxs.sumOf { it.amount } }

    val slices = remember(expenseTxs, categories) {
        FinanceCalculations.getCategoryExpenseSlices(expenseTxs, categories)
    }
    val subcatBreakdown = remember(expenseTxs, selectedChartCategoryId) {
        selectedChartCategoryId?.let { FinanceCalculations.getSubcategoryBreakdown(expenseTxs, it) }.orEmpty()
    }

    val visibleTxs = remember(monthTxs, selectedCategories, filterType) {
        monthTxs.filter { tx ->
            val catOk = if (selectedCategories.isEmpty()) true
            else tx.type != "transfer" && tx.categoryId in selectedCategories
            val typeOk = filterType == TransactionFilterType.ALL || tx.type == filterType.raw
            catOk && typeOk
        }.sortedWith(compareByDescending<Transaction> { it.day }.thenByDescending { it.id })
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNewExpense,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_new_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تراکنش جدید", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onNewTransfer,
                    modifier = Modifier.testTag("tx_transfer_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("انتقال", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onOpenCategories,
                    modifier = Modifier.testTag("tx_categories_button"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("دسته‌ها", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Month + Category Filter + Income/Expense summary
        item {
            VitalsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = { onChangeMonth(-1) },
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "ماه قبل")
                    }
                    Text(
                        text = "${JalaliCalendar.monthName(selectedMonth)} ${JalaliCalendar.toPersianDigits(selectedYear)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedIconButton(
                        onClick = { onChangeMonth(1) },
                        modifier = Modifier.size(44.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "ماه بعد")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فیلتر دسته‌بندی‌ها (چندانتخابی):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedCategories.isNotEmpty()) {
                        TextButton(
                            onClick = onClearCategoryFilters,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "پاک کردن",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat.id in selectedCategories
                        val catColor = parseHexColor(cat.color, MaterialTheme.colorScheme.primary)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleCategoryFilter(cat.id) },
                            label = { Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        color = ext.positiveBg
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("مجموع درآمد", style = MaterialTheme.typography.labelSmall, color = ext.positiveText)
                            Text(
                                text = "+${JalaliCalendar.formatMoney(totalIncome)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ext.positiveText
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        color = ext.errorBg
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("مجموع هزینه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            Text(
                                text = "-${JalaliCalendar.formatMoney(totalExpense)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Interactive Expense Donut Chart + Subcategory Drilldown
        item {
            ExpenseDonutCard(
                slices = slices,
                totalExpense = totalExpense,
                selectedChartCategoryId = selectedChartCategoryId,
                selectedCategoryName = selectedChartCategoryId?.let { cid ->
                    categoriesById[cid]?.let { "${it.emoji} ${it.name}" }
                },
                subcatBreakdown = subcatBreakdown,
                onSelectChartCategory = onSelectChartCategory
            )
        }

        // Type Filter Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تراکنش‌ها (${JalaliCalendar.toPersianDigits(visibleTxs.size)})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransactionFilterType.entries.forEach { ft ->
                    FilterChip(
                        selected = filterType == ft,
                        onClick = { onSelectFilterType(ft) },
                        label = { Text(ft.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }

        if (visibleTxs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = "تراکنشی مطابق فیلتر انتخابی یافت نشد."
                )
            }
        } else {
            items(visibleTxs, key = { it.id }) { tx ->
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
}

@Composable
private fun ExpenseDonutCard(
    slices: List<CategoryExpenseSlice>,
    totalExpense: Long,
    selectedChartCategoryId: String?,
    selectedCategoryName: String?,
    subcatBreakdown: List<SubcategoryExpenseItem>,
    onSelectChartCategory: (String?) -> Unit
) {
    val ext = LocalGhestYarColors.current
    val fallbackTrack = MaterialTheme.colorScheme.outlineVariant

    VitalsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نمودار مخارج بر اساس دسته‌بندی",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "برای جزئیات بزنید",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (slices.isEmpty()) {
            Text(
                text = "هزینه‌ای برای نمایش وجود ندارد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(116.dp)) {
                        val strokeWidth = 22.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
                        if (totalExpense <= 0L) {
                            drawArc(
                                color = fallbackTrack,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        } else {
                            var startAngle = -90f
                            slices.forEach { slice ->
                                val sweep = (slice.amount.toFloat() / totalExpense.toFloat()) * 360f
                                drawArc(
                                    color = parseHexColor(slice.colorHex),
                                    startAngle = startAngle,
                                    sweepAngle = sweep.coerceAtLeast(1f),
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "کل خرج",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${JalaliCalendar.formatMillions(totalExpense)} م",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    slices.forEach { slice ->
                        val isSelected = selectedChartCategoryId == slice.categoryId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent
                                )
                                .clickable { onSelectChartCategory(slice.categoryId) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(slice.colorHex))
                                )
                                Text(
                                    text = "${slice.emoji} ${slice.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${JalaliCalendar.toPersianDigits(slice.percent)}٪",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (selectedChartCategoryId != null && subcatBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = ext.canvas
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ریز زیردسته‌های ${selectedCategoryName.orEmpty()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { onSelectChartCategory(null) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "بستن", modifier = Modifier.size(16.dp))
                            }
                        }
                        subcatBreakdown.forEach { sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${sub.name} (${JalaliCalendar.toPersianDigits(sub.percent)}٪)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${JalaliCalendar.formatMoney(sub.amount)} تومان",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
