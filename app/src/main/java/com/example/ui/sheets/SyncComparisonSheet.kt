package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.JalaliCalendar
import com.example.domain.SectionSyncDiff
import com.example.domain.SyncBadgeStatus
import com.example.domain.SyncStatusUiState
import com.example.ui.components.StatusAccentCard
import com.example.ui.components.VitalsCard
import com.example.ui.components.VitalsChip
import com.example.ui.components.VitalsChipTone
import com.example.ui.theme.LocalGhestYarColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncComparisonSheet(
    syncState: SyncStatusUiState,
    onDismiss: () -> Unit,
    onCheckWithServer: () -> Unit,
    onSmartMergeNewest: () -> Unit,
    onUseServerData: () -> Unit,
    onPushLocalData: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalGhestYarColors.current
    val busy = syncState.isCheckingServer || syncState.isResolvingConflict

    val statusColor = when (syncState.badgeStatus) {
        SyncBadgeStatus.OFFLINE -> MaterialTheme.colorScheme.error
        SyncBadgeStatus.PENDING_UPLOAD -> ext.warning
        SyncBadgeStatus.SERVER_DIFF_FOUND -> MaterialTheme.colorScheme.primary
        SyncBadgeStatus.CHECKING -> MaterialTheme.colorScheme.primary
        SyncBadgeStatus.SYNCED -> ext.positive
    }

    val statusTitle = when (syncState.badgeStatus) {
        SyncBadgeStatus.OFFLINE -> "دستگاه آفلاین است"
        SyncBadgeStatus.CHECKING -> "در حال بررسی با سرور..."
        SyncBadgeStatus.PENDING_UPLOAD -> "تغییرات در انتظار ارسال به سرور"
        SyncBadgeStatus.SERVER_DIFF_FOUND -> "اختلاف بین داده‌های دستگاه و سرور یافت شد"
        SyncBadgeStatus.SYNCED -> "متصل به اینترنت و کاملاً به‌روز"
    }

    val statusSubtitle = when (syncState.badgeStatus) {
        SyncBadgeStatus.OFFLINE -> "اینترنت در دسترس نیست. تغییرات شما روی دستگاه ذخیره می‌شود و با اتصال مجدد به‌صورت خودکار با سرور بررسی خواهد شد."
        SyncBadgeStatus.CHECKING -> "در حال دریافت آخرین وضعیت داده‌ها از سرور ابری..."
        SyncBadgeStatus.PENDING_UPLOAD -> "بخشی از داده‌ها هنوز به‌طور کامل روی سرور تأیید نشده‌اند."
        SyncBadgeStatus.SERVER_DIFF_FOUND -> "مقایسه زیر را ببینید و مشخص کنید کدام نسخه ملاک قرار بگیرد."
        SyncBadgeStatus.SYNCED -> "همه وام‌ها، حساب‌ها، تراکنش‌ها و قرض‌ها با سرور ابری همگام هستند."
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
                title = "وضعیت اتصال و همگام‌سازی ابری",
                onDismiss = onDismiss
            )

            // Live status card
            StatusAccentCard(accentColor = statusColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (syncState.badgeStatus) {
                            SyncBadgeStatus.OFFLINE -> Icons.Rounded.WifiOff
                            SyncBadgeStatus.CHECKING -> Icons.Rounded.CloudSync
                            SyncBadgeStatus.PENDING_UPLOAD -> Icons.Rounded.CloudUpload
                            SyncBadgeStatus.SERVER_DIFF_FOUND -> Icons.Rounded.WarningAmber
                            SyncBadgeStatus.SYNCED -> Icons.Rounded.CloudDone
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VitalsChip(
                        text = if (syncState.isOnline) "اینترنت متصل" else "بدون اینترنت",
                        tone = if (syncState.isOnline) VitalsChipTone.POSITIVE else VitalsChipTone.ALERT
                    )
                    if (syncState.hasPendingWrites) {
                        VitalsChip(text = "تغییر ارسال‌نشده", tone = VitalsChipTone.WARN)
                    } else if (syncState.isFromCache) {
                        VitalsChip(text = "حافظه آفلاین", tone = VitalsChipTone.NEUTRAL)
                    }
                    syncState.lastCheckedAtMillis?.let { ts ->
                        VitalsChip(
                            text = "آخرین بررسی: ${formatPersianTime(ts)}",
                            tone = VitalsChipTone.INFO
                        )
                    }
                }
            }

            if (syncState.lastCheckError != null) {
                SheetErrorBanner(syncState.lastCheckError)
            }

            // Manual Server Check Button
            Button(
                onClick = onCheckWithServer,
                enabled = syncState.isOnline && !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("check_server_sync_button")
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (syncState.isResolvingConflict) "در حال اعمال همگام‌سازی..." else "در حال بررسی با سرور...")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بررسی و مقایسه با داده‌های سرور")
                }
            }

            // Comparison Results
            val report = syncState.comparisonReport
            if (report != null) {
                if (report.isInSync) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = ext.positiveBg,
                        border = BorderStroke(1.dp, ext.positive.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = ext.positive,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "داده‌های دستگاه با سرور کاملاً یکسان و به‌روز است",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ext.positiveText
                                )
                                Text(
                                    text = "بررسی‌شده در ساعت ${formatPersianTime(report.checkedAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.positiveText.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                } else {
                    VitalsCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "نتیجه مقایسه دستگاه و سرور",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VitalsChip(
                                text = "${JalaliCalendar.toPersianDigits(report.totalServerNewer)} مورد جدیدتر در سرور",
                                tone = VitalsChipTone.INFO
                            )
                            VitalsChip(
                                text = "${JalaliCalendar.toPersianDigits(report.totalLocalNewer)} مورد جدیدتر در دستگاه",
                                tone = VitalsChipTone.WARN
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.sections.forEach { sec ->
                                SyncDiffSectionRow(sec)
                            }
                        }
                    }

                    // 3 User-Confirmed Resolution Actions
                    Text(
                        text = "نحوه هماهنگ‌سازی را انتخاب کنید:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSmartMergeNewest,
                        enabled = syncState.isOnline && !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sync_smart_merge_button")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ترکیب هوشمند (استفاده از جدیدترین نسخه هر مورد)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onUseServerData,
                            enabled = syncState.isOnline && !busy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sync_use_server_button")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دریافت از سرور", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onPushLocalData,
                            enabled = syncState.isOnline && !busy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sync_push_local_button")
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ارسال این دستگاه", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (syncState.isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "با اتصال مجدد به اینترنت، بررسی با سرور به‌صورت خودکار انجام می‌شود و هر زمان می‌توانید دکمه بالا را بزنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SyncDiffSectionRow(section: SectionSyncDiff) {
    val ext = LocalGhestYarColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = section.sectionTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "دستگاه: ${JalaliCalendar.toPersianDigits(section.localCount)} • سرور: ${JalaliCalendar.toPersianDigits(section.serverCount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (section.differenceCount == 0) {
            VitalsChip(text = "یکسان", tone = VitalsChipTone.POSITIVE)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (section.serverNewerCount > 0) {
                    VitalsChip(
                        text = "سرور +${JalaliCalendar.toPersianDigits(section.serverNewerCount)}",
                        tone = VitalsChipTone.INFO
                    )
                }
                if (section.localNewerCount > 0) {
                    VitalsChip(
                        text = "دستگاه +${JalaliCalendar.toPersianDigits(section.localNewerCount)}",
                        tone = VitalsChipTone.WARN
                    )
                }
            }
        }
    }
}

private fun formatPersianTime(millis: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.US)
    return JalaliCalendar.toPersianDigits(fmt.format(Date(millis)))
}
