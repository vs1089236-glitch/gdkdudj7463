package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotificationItem
import com.example.data.model.CommissionTransaction
import com.example.data.model.DailyEarningsPoint
import com.example.data.model.PayoutMethodType
import com.example.data.model.PayoutRequest
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.formatInr
import com.example.localization.HoyoStrings
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusProcessing
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusVerified
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "VERIFIED", "COMPLETED", "REWARDED" -> StatusVerified.copy(alpha = 0.14f) to StatusVerified
        "PROCESSING", "JOINED" -> StatusProcessing.copy(alpha = 0.14f) to StatusProcessing
        "REJECTED" -> StatusRejected.copy(alpha = 0.14f) to StatusRejected
        else -> StatusPending.copy(alpha = 0.16f) to StatusPending
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: CommissionTransaction,
    canDeletePending: Boolean,
    onDelete: () -> Unit
) {
    val dateStr = remember(tx.createdAt) {
        tx.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
        } ?: "Just now"
    }
    val typeIcon = when (tx.type) {
        TransactionType.REFERRAL.name -> Icons.Default.PersonAdd
        TransactionType.BONUS.name -> Icons.Default.CheckCircle
        else -> Icons.Default.ArrowDownward
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = tx.type,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(tx.type)
                        if (!tx.clientOrOrderRef.isNullOrBlank()) {
                            append(" • ")
                            append(tx.clientOrOrderRef)
                        }
                        append(" • ")
                        append(dateStr)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${formatInr(tx.amountPaise)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = if (tx.status == TransactionStatus.REJECTED.name) StatusRejected else EmeraldPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(tx.status)
                    if (canDeletePending && tx.status == TransactionStatus.PENDING.name) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete pending entry",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PayoutRowCard(payout: PayoutRequest) {
    val dateStr = remember(payout.createdAt) {
        payout.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
        } ?: "Just now"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = payout.methodType,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payout.destinationDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(payout.methodType)
                        append(" • ")
                        append(dateStr)
                        if (!payout.referenceUtr.isNullOrBlank()) {
                            append(" • UTR: ")
                            append(payout.referenceUtr)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatInr(payout.amountPaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                StatusBadge(payout.status)
            }
        }
    }
}

@Composable
fun DailyTrendBarChart(
    points: List<DailyEarningsPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points) { (points.maxOfOrNull { it.amountPaise } ?: 0L).coerceAtLeast(100L) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = GoldAccent
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val barSlotWidth = size.width / points.size
            val barWidth = (barSlotWidth * 0.46f).coerceAtMost(42.dp.toPx())
            val chartHeight = size.height - 12.dp.toPx()

            points.forEachIndexed { index, point ->
                val x = index * barSlotWidth + (barSlotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 4.dp.toPx()),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                val ratio = (point.amountPaise.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val filledHeight = (chartHeight * ratio).coerceAtLeast(if (point.amountPaise > 0) 10.dp.toPx() else 0f)
                if (filledHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(primaryColor, accentColor)),
                        topLeft = Offset(x, 4.dp.toPx() + chartHeight - filledHeight),
                        size = Size(barWidth, filledHeight),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { point ->
                Text(
                    text = point.dayLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SyncStatusPill(synced: Boolean, strings: HoyoStrings) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = Color(0xFFA7F3D0),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (synced) strings.syncedOnline else strings.offlineCached,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
fun LogActivityDialog(
    strings: HoyoStrings,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (TransactionType, String, String, String, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.COMMISSION) }
    var title by remember { mutableStateOf("") }
    var orderRef by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("5") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.logEntryTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.entryType, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = {
                                Text(
                                    when (t) {
                                        TransactionType.COMMISSION -> strings.filterCommission
                                        TransactionType.REFERRAL -> strings.filterReferral
                                        TransactionType.BONUS -> strings.filterBonus
                                    }
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.entryTitleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_title_input")
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(strings.amountInrLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text(strings.commissionRateLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = orderRef,
                        onValueChange = { orderRef = it },
                        label = { Text(strings.orderRefLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedType, title, orderRef, amountStr, rateStr, notes) },
                enabled = !isSubmitting && title.isNotBlank() && amountStr.isNotBlank(),
                modifier = Modifier.testTag("submit_tx_button")
            ) {
                Text(strings.submitEntry)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun AddReferralDialog(
    strings: HoyoStrings,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addReferral, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.inviteeName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("referral_name_input")
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text(strings.inviteeContact) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, contact) },
                enabled = !isSubmitting && name.isNotBlank(),
                modifier = Modifier.testTag("submit_referral_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun AddPayoutMethodDialog(
    strings: HoyoStrings,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (PayoutMethodType, String, String, String, String, String, String, Boolean) -> Unit
) {
    var type by remember { mutableStateOf(PayoutMethodType.UPI) }
    var label by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var primary by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addPayoutMethod, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == PayoutMethodType.UPI,
                        onClick = { type = PayoutMethodType.UPI },
                        leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                        label = { Text("UPI") }
                    )
                    FilterChip(
                        selected = type == PayoutMethodType.BANK,
                        onClick = { type = PayoutMethodType.BANK },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        label = { Text("Bank") }
                    )
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(strings.labelField) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text(strings.accountHolderLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == PayoutMethodType.UPI) {
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text(strings.upiIdLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upi_id_input")
                    )
                } else {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text(strings.bankNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = last4,
                            onValueChange = { if (it.length <= 4) last4 = it },
                            label = { Text(strings.accountLast4Label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ifsc,
                            onValueChange = { ifsc = it.uppercase() },
                            label = { Text(strings.ifscLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Primary payout account", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = primary, onCheckedChange = { primary = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(type, label, holderName, upiId, bankName, last4, ifsc, primary)
                },
                enabled = !isSubmitting && label.isNotBlank() && holderName.isNotBlank(),
                modifier = Modifier.testTag("save_payout_method_button")
            ) {
                Text(strings.saveMethod)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun NotificationsDialog(
    strings: HoyoStrings,
    notifications: List<AppNotificationItem>,
    onDismiss: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text(strings.notificationsTitle) },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    text = strings.noNotifications,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    notifications.take(8).forEachIndexed { idx, item ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    timeFmt.format(item.timestamp.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                item.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (idx < notifications.take(8).lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
