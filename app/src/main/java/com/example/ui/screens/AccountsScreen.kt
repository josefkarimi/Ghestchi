package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Account
import com.example.domain.FinanceCalculations
import com.example.domain.JalaliCalendar
import com.example.domain.Transaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.theme.LocalGhestYarColors
import com.example.ui.theme.parseHexColor

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    transactions: List<Transaction>,
    onTransferClick: () -> Unit,
    onEditAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onOpenAccountLedger: (Account) -> Unit,
    onCopyCard: (String) -> Unit
) {
    val ext = LocalGhestYarColors.current
    val balancesById = remember(accounts, transactions) {
        accounts.associate { it.id to FinanceCalculations.getAccountBalance(it, transactions) }
    }
    val totalBalance = remember(balancesById) { balancesById.values.sum() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatusAccentCard(
                accentColor = if (totalBalance < 0L) MaterialTheme.colorScheme.error else ext.positive
            ) {
                Text(
                    text = "موجودی کل حساب‌ها و کارت‌ها",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = JalaliCalendar.formatMoney(totalBalance),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (totalBalance < 0L) MaterialTheme.colorScheme.error else ext.positive
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accounts_transfer_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انتقال وجه بین حساب‌ها", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (accounts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AccountBalance,
                    title = "هنوز حسابی ثبت نکرده‌اید",
                    subtitle = "برای افزودن اولین حساب بانکی روی دکمه «حساب جدید» بزنید."
                )
            }
        } else {
            items(accounts, key = { it.id }) { acc ->
                val balance = balancesById[acc.id] ?: acc.initialBalance
                val accent = parseHexColor(acc.color, MaterialTheme.colorScheme.primary)
                StatusAccentCard(
                    accentColor = accent,
                    onClick = { onOpenAccountLedger(acc) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            VitalsChip("حساب / کارت", VitalsChipTone.NEUTRAL)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row {
                            IconButton(onClick = { onEditAccount(acc) }, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "ویرایش حساب",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteAccount(acc) }, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف حساب",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (acc.cardNumber.isNotBlank()) {
                        TextButton(
                            onClick = { onCopyCard(acc.cardNumber) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "کپی شماره کارت",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = JalaliCalendar.toPersianDigits(acc.cardNumber),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "موجودی فعلی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = JalaliCalendar.formatMoney(balance),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (balance < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تومان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        VitalsChip("گردش حساب ◂", VitalsChipTone.INFO)
                    }
                }
            }
        }
    }
}
