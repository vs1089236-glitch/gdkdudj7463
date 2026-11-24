package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionStatus
import com.example.data.model.formatInr
import com.example.localization.HoyoStrings
import com.example.ui.components.DailyTrendBarChart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.HoyoDashboardData

@Composable
fun AnalyticsScreen(
    data: HoyoDashboardData,
    strings: HoyoStrings
) {
    val totalVerified = data.summary.verifiedEarningsPaise.coerceAtLeast(1L)
    val verifiedCount = data.transactions.count { it.status == TransactionStatus.VERIFIED.name }
    val totalCount = data.transactions.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(strings.analyticsTitle, style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.sevenDayTrend, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(14.dp))
                    DailyTrendBarChart(points = data.sevenDayTrend)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(strings.earningsBreakdown, style = MaterialTheme.typography.titleLarge)
                    BreakdownProgressRow(
                        label = strings.commissionsLabel,
                        amountText = formatInr(data.summary.commissionEarningsPaise),
                        progress = data.summary.commissionEarningsPaise.toFloat() / totalVerified.toFloat(),
                        color = EmeraldPrimary
                    )
                    BreakdownProgressRow(
                        label = strings.referralsLabel,
                        amountText = formatInr(data.summary.referralEarningsPaise),
                        progress = data.summary.referralEarningsPaise.toFloat() / totalVerified.toFloat(),
                        color = GoldAccent
                    )
                    BreakdownProgressRow(
                        label = strings.bonusesLabel,
                        amountText = formatInr(data.summary.bonusEarningsPaise),
                        progress = data.summary.bonusEarningsPaise.toFloat() / totalVerified.toFloat(),
                        color = Color(0xFF0EA5E9)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings.verificationRate, style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$verifiedCount / $totalCount entries verified",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (totalCount == 0) "0%" else "${(verifiedCount * 100) / totalCount}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = EmeraldPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (totalCount == 0) 0f else verifiedCount.toFloat() / totalCount.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownProgressRow(
    label: String,
    amountText: String,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                amountText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}
