package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.Account
import com.example.domain.Category
import com.example.domain.JalaliCalendar
import com.example.domain.Person
import com.example.domain.Transaction
import com.example.ui.theme.LocalGhestYarColors
import com.example.ui.theme.parseHexColor

enum class VitalsChipTone {
    POSITIVE, INFO, WARN, ALERT, NEUTRAL
}

@Composable
fun VitalsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun StatusAccentCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val baseModifier = if (onClick != null) {
        modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = baseModifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val barWidth = 4.5.dp.toPx()
                    val startX = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(barWidth, size.height)
                    )
                }
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            content = content
        )
    }
}

@Composable
fun VitalsChip(
    text: String,
    tone: VitalsChipTone = VitalsChipTone.POSITIVE,
    modifier: Modifier = Modifier
) {
    val ext = LocalGhestYarColors.current
    val (bg, fg) = when (tone) {
        VitalsChipTone.POSITIVE -> ext.positiveBg to ext.positiveText
        VitalsChipTone.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        VitalsChipTone.WARN -> ext.warningBg to ext.warningText
        VitalsChipTone.ALERT -> ext.errorBg to MaterialTheme.colorScheme.error
        VitalsChipTone.NEUTRAL -> ext.canvas to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    VitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun <T> VitalsDropdownSelector(
    label: String,
    selectedText: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { expanded = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JalaliDatePickerRow(
    day: Int,
    month: Int,
    year: Int,
    onDayChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = remember { (1..31).toList() }
    val months = remember { (1..12).toList() }
    val years = remember { (1398..1412).toList() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VitalsDropdownSelector(
            label = "روز",
            selectedText = JalaliCalendar.toPersianDigits(day),
            items = days,
            itemLabel = { JalaliCalendar.toPersianDigits(it) },
            onSelect = onDayChange,
            modifier = Modifier.weight(0.9f)
        )
        VitalsDropdownSelector(
            label = "ماه",
            selectedText = JalaliCalendar.monthName(month),
            items = months,
            itemLabel = { JalaliCalendar.monthName(it) },
            onSelect = onMonthChange,
            modifier = Modifier.weight(1.3f)
        )
        VitalsDropdownSelector(
            label = "سال",
            selectedText = JalaliCalendar.toPersianDigits(year),
            items = years,
            itemLabel = { JalaliCalendar.toPersianDigits(it) },
            onSelect = onYearChange,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
fun ColorSwatchRow(
    colors: List<String>,
    selectedHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { hex ->
            val selected = hex.equals(selectedHex, ignoreCase = true)
            val color = parseHexColor(hex)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: Transaction,
    accountsById: Map<String, Account>,
    categoriesById: Map<String, Category>,
    peopleById: Map<String, Person>,
    contextAccountId: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalGhestYarColors.current
    val dateText = "${JalaliCalendar.toPersianDigits(tx.day)} ${JalaliCalendar.monthName(tx.month)}"

    VitalsCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tx.type == "transfer") {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "انتقال داخلی",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                val fromName = accountsById[tx.fromAccountId]?.name ?: "حساب حذف‌شده"
                val toName = accountsById[tx.toAccountId]?.name ?: "حساب حذف‌شده"

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "انتقال داخلی",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    VitalsChip(
                        text = "$fromName ← $toName",
                        tone = VitalsChipTone.INFO
                    )
                    Text(
                        text = buildString {
                            append("$dateText ${JalaliCalendar.toPersianDigits(tx.year)}")
                            if (tx.note.isNotBlank()) append(" • ${tx.note}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val (sign, color) = when (contextAccountId) {
                    tx.fromAccountId -> "-" to MaterialTheme.colorScheme.error
                    tx.toAccountId -> "+" to ext.positive
                    else -> "⇄ " to MaterialTheme.colorScheme.primary
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$sign${JalaliCalendar.formatMoney(tx.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val cat = categoriesById[tx.categoryId]
                val catColor = parseHexColor(cat?.color, MaterialTheme.colorScheme.primary)
                val accName = accountsById[tx.accountId]?.name ?: "حساب نامشخص"
                val personName = tx.personId?.let { peopleById[it]?.name }
                val isIncome = tx.type == "income"

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(catColor.copy(alpha = 0.14f))
                        .border(1.dp, catColor.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat?.emoji ?: "💸",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = cat?.name ?: "دسته‌بندی‌نشده",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tx.subcategory.isNotBlank() || !personName.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tx.subcategory.isNotBlank()) {
                                VitalsChip(text = tx.subcategory, tone = VitalsChipTone.NEUTRAL)
                            }
                            if (!personName.isNullOrBlank()) {
                                VitalsChip(text = "👤 $personName", tone = VitalsChipTone.WARN)
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append("$dateText • $accName")
                            if (tx.note.isNotBlank()) append(" • ${tx.note}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isIncome) "+" else "-"}${JalaliCalendar.formatMoney(tx.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isIncome) ext.positive else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "تومان",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "ویرایش تراکنش",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف تراکنش",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
