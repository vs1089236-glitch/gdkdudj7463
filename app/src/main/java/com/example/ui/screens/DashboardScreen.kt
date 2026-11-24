package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.buildReferralCodeForUid
import com.example.data.model.formatInr
import com.example.localization.HoyoStrings
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.HoyoDashboardData

@Composable
fun DashboardScreen(
    data: HoyoDashboardData,
    strings: HoyoStrings,
    currentUserId: String,
    isOnline: Boolean,
    onOpenLogActivity: () -> Unit,
    onOpenPayoutsTab: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenAddReferral: () -> Unit,
    onDeletePendingTx: (String) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val referralCode = data.profile?.referralCode?.takeIf { it.isNotBlank() }
        ?: buildReferralCodeForUid(currentUserId)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepTeal, Color(0xFF0F5950), EmeraldPrimary)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.availableBalance,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        SyncStatusPill(synced = isOnline, strings = strings)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = formatInr(data.summary.availableBalancePaise),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = Color.White
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroStatPill(
                            label = strings.verifiedEarnings,
                            value = formatInr(data.summary.verifiedEarningsPaise),
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatPill(
                            label = strings.pendingVerification,
                            value = formatInr(data.summary.pendingEarningsPaise),
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatPill(
                            label = strings.totalPaidOut,
                            value = formatInr(data.summary.totalPaidOutPaise),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.yourReferralCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = referralCode,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Referral Code", referralCode))
                                    onNotify(strings.codeCopied)
                                },
                                modifier = Modifier.testTag("copy_referral_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = strings.copyCode, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.copyCode)
                            }
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join Hoyo Pay to track commissions and referrals transparently! Use my referral code: $referralCode"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, strings.shareInvite))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                modifier = Modifier.testTag("share_referral_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = strings.shareInvite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.shareInvite)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Default.AddCircleOutline,
                    label = strings.logCommission,
                    accent = EmeraldPrimary,
                    testTag = "quick_log_activity",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLogActivity
                )
                QuickActionTile(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = strings.requestPayout,
                    accent = GoldAccent,
                    testTag = "quick_request_payout",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPayoutsTab
                )
                QuickActionTile(
                    icon = Icons.Default.GroupAdd,
                    label = strings.addReferral,
                    accent = Color(0xFF0EA5E9),
                    testTag = "quick_add_referral",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAddReferral
                )
                QuickActionTile(
                    icon = Icons.Default.FileDownload,
                    label = strings.exportCsv,
                    accent = Color(0xFF8B5CF6),
                    testTag = "quick_export_csv",
                    modifier = Modifier.weight(1f),
                    onClick = onExportCsv
                )
            }
        }

        item {
            Text(
                text = strings.recentTransactions,
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (data.transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(strings.noTransactionsTitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.noTransactionsHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(data.transactions.take(8), key = { it.id }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    canDeletePending = true,
                    onDelete = { onDeletePendingTx(tx.id) }
                )
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = Color.White,
            maxLines = 1
        )
    }
}
