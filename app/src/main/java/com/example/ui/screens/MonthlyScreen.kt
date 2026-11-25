package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FinanceCalculations
import com.example.domain.InstallmentItem
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.domain.Loan
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.VitalsCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.theme.LocalGhestYarColors
import kotlin.math.roundToInt

@Composable
fun MonthlyScreen(
    loans: List<Loan>,
    today: JalaliDate,
    selectedYear: Int,
    selectedMonth: Int,
    isWide: Boolean,
    onChangeMonth: (Int) -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onPayInstallment: (InstallmentItem) -> Unit,
    onUnpayInstallment: (InstallmentItem) -> Unit,
    onCopyCard: (String) -> Unit
) {
    val summary = remember(loans) { FinanceCalculations.getGlobalSummary(loans) }
    val monthItems = remember(loans, selectedYear, selectedMonth) {
        FinanceCalculations.getInstallmentsForMonth(loans, selectedYear, selectedMonth)
    }
    val totalAmount = remember(monthItems) { monthItems.sumOf { it.amount } }
    val paidAmount = remember(monthItems) { monthItems.filter { it.paid }.sumOf { it.amount } }
    val unpaidAmount = (totalAmount - paidAmount).coerceAtLeast(0L)
    val progressFloat = if (totalAmount > 0L) (paidAmount.toFloat() / totalAmount.toFloat()) else 1f
    val progressPercent = (progressFloat * 100f).roundToInt()
    val animatedProgress by animateFloatAsState(targetValue = progressFloat, label = "monthly_progress")
    val isCurrentMonth = selectedYear == today.year && selectedMonth == today.month

    if (isWide) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            LazyColumn(
                modifier = Modifier.weight(0.45f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlobalDebtHeroCard(
                        totalRemainingDebt = summary.totalRemainingDebt,
                        activeLoansCount = summary.activeLoansCount
                    )
                }
                item {
                    MonthProgressCard(
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        isCurrentMonth = isCurrentMonth,
                        progressPercent = progressPercent,
                        animatedProgress = animatedProgress,
                        totalAmount = totalAmount,
                        paidAmount = paidAmount,
                        unpaidAmount = unpaidAmount,
                        onChangeMonth = onChangeMonth,
                        onJumpCurrentMonth = onJumpCurrentMonth
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(0.55f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (monthItems.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Celebration,
                            title = "هیچ قسطی در ${JalaliCalendar.monthName(selectedMonth)} ${JalaliCalendar.toPersianDigits(selectedYear)} ندارید!"
                        )
                    }
                } else {
                    items(monthItems, key = { "${it.loanId}_${it.index}" }) { item ->
                        InstallmentCard(
                            item = item,
                            today = today,
                            onPay = { onPayInstallment(item) },
                            onUnpay = { onUnpayInstallment(item) },
                            onCopyCard = onCopyCard
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlobalDebtHeroCard(
                    totalRemainingDebt = summary.totalRemainingDebt,
                    activeLoansCount = summary.activeLoansCount
                )
            }
            item {
                MonthProgressCard(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    isCurrentMonth = isCurrentMonth,
                    progressPercent = progressPercent,
                    animatedProgress = animatedProgress,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    unpaidAmount = unpaidAmount,
                    onChangeMonth = onChangeMonth,
                    onJumpCurrentMonth = onJumpCurrentMonth
                )
            }
            if (monthItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Celebration,
                        title = "هیچ قسطی در ${JalaliCalendar.monthName(selectedMonth)} ${JalaliCalendar.toPersianDigits(selectedYear)} ندارید!"
                    )
                }
            } else {
                items(monthItems, key = { "${it.loanId}_${it.index}" }) { item ->
                    InstallmentCard(
                        item = item,
                        today = today,
                        onPay = { onPayInstallment(item) },
                        onUnpay = { onUnpayInstallment(item) },
                        onCopyCard = onCopyCard
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalDebtHeroCard(
    totalRemainingDebt: Long,
    activeLoansCount: Int
) {
    StatusAccentCard(accentColor = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کل بدهی باقی‌مانده وام‌ها",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VitalsChip(
                text = "${JalaliCalendar.toPersianDigits(activeLoansCount)} وام فعال",
                tone = VitalsChipTone.INFO
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = JalaliCalendar.formatMoney(totalRemainingDebt),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "تومان",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun MonthProgressCard(
    selectedYear: Int,
    selectedMonth: Int,
    isCurrentMonth: Boolean,
    progressPercent: Int,
    animatedProgress: Float,
    totalAmount: Long,
    paidAmount: Long,
    unpaidAmount: Long,
    onChangeMonth: (Int) -> Unit,
    onJumpCurrentMonth: () -> Unit
) {
    val ext = LocalGhestYarColors.current
    VitalsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                onClick = { onChangeMonth(-1) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("prev_month_button"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "ماه قبل"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${JalaliCalendar.monthName(selectedMonth)} ${JalaliCalendar.toPersianDigits(selectedYear)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isCurrentMonth) {
                    TextButton(
                        onClick = onJumpCurrentMonth,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "بازگشت به ماه جاری",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "ماه جاری",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedIconButton(
                onClick = { onChangeMonth(1) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("next_month_button"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "ماه بعد"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "پیشرفت تسویه ماه",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${JalaliCalendar.toPersianDigits(progressPercent)}٪ پرداخت شده",
                style = MaterialTheme.typography.labelLarge,
                color = ext.positive
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = ext.positive,
            trackColor = ext.canvas
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonthKpiTile(
                label = "مجموع کل ماه",
                value = JalaliCalendar.formatMoney(totalAmount),
                bgColor = ext.canvas,
                textColor = MaterialTheme.colorScheme.onSurface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            MonthKpiTile(
                label = "پرداخت شده",
                value = JalaliCalendar.formatMoney(paidAmount),
                bgColor = ext.positiveBg,
                textColor = ext.positiveText,
                labelColor = ext.positiveText,
                modifier = Modifier.weight(1f)
            )
            MonthKpiTile(
                label = "باقی‌مانده",
                value = JalaliCalendar.formatMoney(unpaidAmount),
                bgColor = ext.warningBg,
                textColor = ext.warningText,
                labelColor = ext.warningText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthKpiTile(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InstallmentCard(
    item: InstallmentItem,
    today: JalaliDate,
    onPay: () -> Unit,
    onUnpay: () -> Unit,
    onCopyCard: (String) -> Unit
) {
    val ext = LocalGhestYarColors.current
    val isToday = FinanceCalculations.isInstallmentToday(item, today)
    val isOverdue = FinanceCalculations.isInstallmentOverdue(item, today)

    val accentColor = when {
        item.paid -> ext.positive
        isOverdue -> MaterialTheme.colorScheme.error
        isToday -> ext.warning
        else -> MaterialTheme.colorScheme.primary
    }

    StatusAccentCard(
        accentColor = accentColor,
        alpha = if (item.paid) 0.82f else 1f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (item.paid) ext.canvas else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = JalaliCalendar.toPersianDigits(item.day),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.paid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = JalaliCalendar.monthName(item.month).take(4),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.paid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.loanTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = if (item.paid) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.paid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    when {
                        item.paid -> VitalsChip("پرداخت شده", VitalsChipTone.POSITIVE)
                        isOverdue -> VitalsChip("سررسید گذشته", VitalsChipTone.ALERT)
                        isToday -> VitalsChip("سررسید امروز", VitalsChipTone.WARN)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VitalsChip(item.loanSource, VitalsChipTone.NEUTRAL)
                    Text(
                        text = "قسط ${JalaliCalendar.toPersianDigits(item.installmentNumber)} از ${JalaliCalendar.toPersianDigits(item.totalCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = JalaliCalendar.formatMoney(item.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تومان",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.loanCard.isNotBlank()) {
                TextButton(
                    onClick = { onCopyCard(item.loanCard) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "کپی شماره کارت",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = JalaliCalendar.toPersianDigits(item.loanCard),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (item.paid) {
                OutlinedButton(
                    onClick = onUnpay,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("لغو پرداخت", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onPay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پرداخت شد", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
