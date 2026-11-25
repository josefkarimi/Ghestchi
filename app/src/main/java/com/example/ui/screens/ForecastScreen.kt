package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FinanceCalculations
import com.example.domain.ForecastMonth
import com.example.domain.JalaliCalendar
import com.example.domain.Loan
import com.example.ui.components.VitalsCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.theme.LocalGhestYarColors

@Composable
fun ForecastScreen(
    loans: List<Loan>,
    baseYear: Int,
    baseMonth: Int,
    isWide: Boolean,
    onSelectMonth: (year: Int, month: Int) -> Unit
) {
    val forecast = remember(loans, baseYear, baseMonth) {
        FinanceCalculations.get12MonthForecast(loans, baseYear, baseMonth)
    }
    val next12Total = remember(forecast) { forecast.sumOf { it.total } }
    val maxTotal = remember(forecast) { forecast.maxOfOrNull { it.total } ?: 0L }

    if (isWide) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            LazyColumn(
                modifier = Modifier.weight(0.5f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
            ) {
                item {
                    ForecastChartCard(
                        next12Total = next12Total,
                        forecast = forecast,
                        maxTotal = maxTotal,
                        onSelectMonth = onSelectMonth
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(0.5f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "جزئیات پرداختی هر ماه",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                itemsIndexed(forecast, key = { _, m -> "${m.year}_${m.month}" }) { idx, m ->
                    ForecastMonthRowCard(
                        index = idx,
                        month = m,
                        onClick = { onSelectMonth(m.year, m.month) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ForecastChartCard(
                    next12Total = next12Total,
                    forecast = forecast,
                    maxTotal = maxTotal,
                    onSelectMonth = onSelectMonth
                )
            }
            item {
                Text(
                    text = "جزئیات پرداختی هر ماه",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
            itemsIndexed(forecast, key = { _, m -> "${m.year}_${m.month}" }) { idx, m ->
                ForecastMonthRowCard(
                    index = idx,
                    month = m,
                    onClick = { onSelectMonth(m.year, m.month) }
                )
            }
        }
    }
}

@Composable
private fun ForecastChartCard(
    next12Total: Long,
    forecast: List<ForecastMonth>,
    maxTotal: Long,
    onSelectMonth: (Int, Int) -> Unit
) {
    val ext = LocalGhestYarColors.current
    VitalsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مجموع تعهدات ۱۲ ماه آینده",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = JalaliCalendar.formatMoney(next12Total),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            VitalsChip("میلیون تومان", VitalsChipTone.INFO)
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "نمودار تعهدات ماهانه (روی هر ستون بزنید)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            forecast.forEachIndexed { idx, m ->
                val ratio = if (maxTotal > 0L) (m.total.toFloat() / maxTotal.toFloat()) else 0f
                val barHeight = (ratio * 126f).coerceAtLeast(10f).dp
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectMonth(m.year, m.month) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = JalaliCalendar.formatMillions(m.total),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(
                                if (idx == 0) MaterialTheme.colorScheme.primary
                                else ext.positive.copy(alpha = 0.78f)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = m.label.take(3),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastMonthRowCard(
    index: Int,
    month: ForecastMonth,
    onClick: () -> Unit
) {
    val ext = LocalGhestYarColors.current
    VitalsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (index == 0) MaterialTheme.colorScheme.primary else ext.canvas
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = JalaliCalendar.toPersianDigits(index + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${month.label} ${month.yearLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (index == 0) {
                            VitalsChip("ماه جاری", VitalsChipTone.INFO)
                        }
                    }
                    Text(
                        text = "${JalaliCalendar.toPersianDigits(month.count)} قسط فعال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = JalaliCalendar.formatMoney(month.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${JalaliCalendar.formatMillions(month.total)} میلیون تومان",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
