package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.FinanceCalculations
import com.example.domain.JalaliCalendar
import com.example.domain.Person
import com.example.domain.Transaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.VitalsCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.theme.LocalGhestYarColors
import kotlin.math.abs

@Composable
fun DebtsScreen(
    people: List<Person>,
    transactions: List<Transaction>,
    onAddPerson: () -> Unit,
    onOpenPersonLedger: (Person) -> Unit
) {
    val ext = LocalGhestYarColors.current
    val stats = remember(people, transactions) {
        people.map { person ->
            person to FinanceCalculations.getPersonNetBalance(person.id, transactions)
        }
    }
    val totalReceivable = remember(stats) { stats.filter { it.second > 0L }.sumOf { it.second } }
    val totalPayable = remember(stats) { stats.filter { it.second < 0L }.sumOf { abs(it.second) } }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusAccentCard(
                    accentColor = ext.positive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "مجموع طلب شما",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = JalaliCalendar.formatMoney(totalReceivable),
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.positive
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusAccentCard(
                    accentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "مجموع بدهی شما",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = JalaliCalendar.formatMoney(totalPayable),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست افراد (${JalaliCalendar.toPersianDigits(people.size)})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = onAddPerson,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن شخص", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (stats.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Group,
                    title = "هنوز شخصی برای قرض و قوله ثبت نشده است"
                )
            }
        } else {
            items(stats, key = { it.first.id }) { (person, net) ->
                val (chipText, chipTone, amountColor) = when {
                    net > 0L -> Triple("بدهکار به شما (طلب شما)", VitalsChipTone.POSITIVE, ext.positive)
                    net < 0L -> Triple("طلبکار از شما (بدهی شما)", VitalsChipTone.ALERT, MaterialTheme.colorScheme.error)
                    else -> Triple("تسویه شده", VitalsChipTone.NEUTRAL, MaterialTheme.colorScheme.onSurfaceVariant)
                }

                VitalsCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenPersonLedger(person) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                VitalsChip(chipText, chipTone)
                            }
                            if (person.note.isNotBlank()) {
                                Text(
                                    text = person.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = JalaliCalendar.formatMoney(abs(net)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = amountColor
                            )
                            Text(
                                text = "تومان",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
