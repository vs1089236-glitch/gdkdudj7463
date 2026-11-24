package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PayoutStatus
import com.example.data.model.TransactionStatus
import com.example.data.model.formatInr
import com.example.data.preferences.AppPreferences
import com.example.localization.AppLanguage
import com.example.localization.HoyoStrings
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusVerified
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.HoyoDashboardData

@Composable
fun AdminMonitorScreen(
    data: HoyoDashboardData,
    strings: HoyoStrings,
    onVerifyTransaction: (String, TransactionStatus) -> Unit,
    onUpdatePayout: (String, PayoutStatus, String?) -> Unit
) {
    var utrMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(strings.adminTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        strings.adminSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(strings.pendingTransactionsSection, style = MaterialTheme.typography.titleLarge)
        }

        items(data.adminTransactions, key = { it.id }) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${tx.userName} • ${tx.type}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatInr(tx.amountPaise),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                            )
                            StatusBadge(tx.status)
                        }
                    }
                    if (tx.status == TransactionStatus.PENDING.name) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onVerifyTransaction(tx.id, TransactionStatus.VERIFIED) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusVerified),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.approve)
                            }
                            OutlinedButton(
                                onClick = { onVerifyTransaction(tx.id, TransactionStatus.REJECTED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.reject)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(strings.pendingPayoutsSection, style = MaterialTheme.typography.titleLarge)
        }

        items(data.adminPayoutRequests, key = { it.id }) { payout ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(payout.destinationDisplay, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${payout.userName} • ${payout.methodType}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatInr(payout.amountPaise),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                            )
                            StatusBadge(payout.status)
                        }
                    }
                    if (payout.status == PayoutStatus.PENDING.name || payout.status == PayoutStatus.PROCESSING.name) {
                        OutlinedTextField(
                            value = utrMap[payout.id] ?: "",
                            onValueChange = { utrMap = utrMap + (payout.id to it) },
                            label = { Text(strings.utrHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onUpdatePayout(
                                        payout.id,
                                        PayoutStatus.COMPLETED,
                                        utrMap[payout.id]
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusVerified),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.markCompleted)
                            }
                            OutlinedButton(
                                onClick = {
                                    onUpdatePayout(
                                        payout.id,
                                        PayoutStatus.REJECTED,
                                        utrMap[payout.id]
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.reject)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndSupportScreen(
    data: HoyoDashboardData,
    prefs: AppPreferences,
    strings: HoyoStrings,
    onSaveProfile: (String, String, String?, Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenTutorial: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(data.profile?.displayName) { mutableStateOf(data.profile?.displayName ?: "") }
    var phone by remember(data.profile?.phone) { mutableStateOf(data.profile?.phone ?: "") }
    var referredBy by remember(data.profile?.referredByCode) { mutableStateOf(data.profile?.referredByCode ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(strings.settingsTitle, style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.accountProfile, style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.mobileOptional) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = referredBy,
                        onValueChange = { referredBy = it.uppercase() },
                        label = { Text(strings.referralCodeOptional) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSaveProfile(name, phone, referredBy.ifBlank { null }, prefs.biometricLockEnabled)
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                    ) {
                        Text(strings.updateProfile)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> strings.themeSystem
                                            ThemeMode.LIGHT -> strings.themeLight
                                            ThemeMode.DARK -> strings.themeDark
                                        }
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.biometricLockLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.biometricLockDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.biometricLockEnabled,
                            onCheckedChange = onBiometricToggle
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.pushAlertsLabel, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.pushAlertsDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = onNotificationsToggle
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenTutorial,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.School, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.viewTutorial)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.supportTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        strings.supportDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sainishaab9076"))
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_support_button")
                    ) {
                        Text(strings.contactTelegramBtn)
                    }
                    Text(
                        text = strings.transparencyNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.signOut)
            }
        }
    }
}
