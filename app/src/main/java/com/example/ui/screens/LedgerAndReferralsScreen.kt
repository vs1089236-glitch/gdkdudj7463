package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.data.model.formatInr
import com.example.localization.HoyoStrings
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionRowCard
import com.example.ui.viewmodel.HoyoDashboardData

@Composable
fun LedgerAndReferralsScreen(
    data: HoyoDashboardData,
    strings: HoyoStrings,
    onOpenLogActivity: () -> Unit,
    onOpenAddReferral: () -> Unit,
    onExportCsv: () -> Unit,
    onDeletePendingTx: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = remember(data.transactions, selectedFilter) {
        if (selectedFilter == null) data.transactions
        else data.transactions.filter { it.type == selectedFilter!!.name }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.ledgerTitle, style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.testTag("ledger_export_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = strings.exportCsv)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportCsv)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text(strings.filterAll) }
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.COMMISSION,
                    onClick = { selectedFilter = TransactionType.COMMISSION },
                    label = { Text(strings.filterCommission) }
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.REFERRAL,
                    onClick = { selectedFilter = TransactionType.REFERRAL },
                    label = { Text(strings.filterReferral) }
                )
                FilterChip(
                    selected = selectedFilter == TransactionType.BONUS,
                    onClick = { selectedFilter = TransactionType.BONUS },
                    label = { Text(strings.filterBonus) }
                )
            }
        }

        item {
            Button(
                onClick = onOpenLogActivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ledger_log_activity_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(strings.logCommission)
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(strings.noTransactionsTitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.noTransactionsHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    canDeletePending = true,
                    onDelete = { onDeletePendingTx(tx.id) }
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.referralInvitesTitle, style = MaterialTheme.typography.titleLarge)
                OutlinedButton(
                    onClick = onOpenAddReferral,
                    modifier = Modifier.testTag("ledger_add_referral_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addReferral)
                }
            }
        }

        items(data.referrals, key = { it.id }) { ref ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ref.inviteeName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = listOfNotNull(ref.inviteeContact, ref.referralCodeUsed).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatInr(ref.bonusPaise),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusBadge(ref.status)
                    }
                }
            }
        }
    }
}
