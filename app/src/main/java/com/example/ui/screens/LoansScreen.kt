package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.EnrichedLoan
import com.example.domain.FinanceCalculations
import com.example.domain.JalaliCalendar
import com.example.domain.Loan
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.theme.LocalGhestYarColors

@Composable
fun LoansScreen(
    loans: List<Loan>,
    onEditLoan: (Loan) -> Unit,
    onDeleteLoan: (Loan) -> Unit,
    onOpenSchedule: (Loan) -> Unit
) {
    val enriched = remember(loans) { FinanceCalculations.getEnrichedLoans(loans) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وام‌ها (به ترتیب نزدیک‌ترین تسویه)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                VitalsChip(
                    text = "${JalaliCalendar.toPersianDigits(loans.size)} مورد",
                    tone = VitalsChipTone.NEUTRAL
                )
            }
        }

        if (enriched.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AddCircleOutline,
                    title = "هنوز وامی ثبت نکرده‌اید",
                    subtitle = "برای افزودن اولین وام روی دکمه «وام جدید» بزنید."
                )
            }
        } else {
            items(enriched, key = { it.loan.id }) { item ->
                LoanOverviewCard(
                    item = item,
                    onEdit = { onEditLoan(item.loan) },
                    onDelete = { onDeleteLoan(item.loan) },
                    onOpenSchedule = { onOpenSchedule(item.loan) }
                )
            }
        }
    }
}

@Composable
private fun LoanOverviewCard(
    item: EnrichedLoan,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenSchedule: () -> Unit
) {
    val ext = LocalGhestYarColors.current
    val loan = item.loan

    StatusAccentCard(
        accentColor = if (item.completed) ext.positive else MaterialTheme.colorScheme.primary,
        alpha = if (item.completed) 0.82f else 1f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loan.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    VitalsChip(loan.source, VitalsChipTone.NEUTRAL)
                    if (item.completed) {
                        VitalsChip("تسویه شده", VitalsChipTone.POSITIVE)
                    }
                }
                Text(
                    text = "روز ${JalaliCalendar.toPersianDigits(loan.startDay)} هر ماه • پایان: ${JalaliCalendar.monthName(item.endDate.month)} ${JalaliCalendar.toPersianDigits(item.endDate.year)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "ویرایش وام",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف وام",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = ext.canvas
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مبلغ هر قسط",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${JalaliCalendar.formatMoney(loan.amount)} تومان",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "بدهی باقی‌مانده",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${JalaliCalendar.formatMoney(item.remainingDebt)} تومان",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${JalaliCalendar.toPersianDigits(item.paidCount)} از ${JalaliCalendar.toPersianDigits(loan.totalCount)} قسط پرداخت شده",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${JalaliCalendar.toPersianDigits(item.progressPercent)}٪",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ext.positive
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = ext.positive,
            trackColor = ext.canvas
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenSchedule,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(
                imageVector = Icons.Rounded.Checklist,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("مشاهده و مدیریت تک‌تک اقساط", style = MaterialTheme.typography.labelLarge)
        }
    }
}
