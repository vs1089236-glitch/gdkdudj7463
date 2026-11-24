package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.CommissionTransaction
import com.example.data.model.PayoutRequest
import com.example.data.model.formatInr
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun exportLedgerCsv(
        context: Context,
        transactions: List<CommissionTransaction>,
        payouts: List<PayoutRequest>
    ): Result<Intent> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "hoyo_pay_statement_${System.currentTimeMillis()}.csv"
            val file = File(exportDir, fileName)

            val sb = StringBuilder()
            sb.appendLine("Category,ID,Title/Destination,Type,Amount (INR),Rate (%),Status,Reference,Date")
            for (tx in transactions) {
                val dateStr = tx.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "-"
                val ratePercent = String.format(Locale.US, "%.2f", tx.commissionRateBps / 100.0)
                sb.appendLine(
                    listOf(
                        "LEDGER",
                        csvEscape(tx.id),
                        csvEscape(tx.title),
                        csvEscape(tx.type),
                        csvEscape(formatInr(tx.amountPaise)),
                        ratePercent,
                        csvEscape(tx.status),
                        csvEscape(tx.clientOrOrderRef ?: "-"),
                        csvEscape(dateStr)
                    ).joinToString(",")
                )
            }
            for (payout in payouts) {
                val dateStr = payout.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "-"
                sb.appendLine(
                    listOf(
                        "PAYOUT",
                        csvEscape(payout.id),
                        csvEscape(payout.destinationDisplay),
                        csvEscape(payout.methodType),
                        csvEscape(formatInr(payout.amountPaise)),
                        "-",
                        csvEscape(payout.status),
                        csvEscape(payout.referenceUtr ?: "-"),
                        csvEscape(dateStr)
                    ).joinToString(",")
                )
            }

            file.writeText(sb.toString(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Hoyo Pay Statement (${dateFormatter.format(Date())})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Result.success(Intent.createChooser(shareIntent, "Export Hoyo Pay Statement"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
