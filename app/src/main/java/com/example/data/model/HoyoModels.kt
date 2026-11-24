package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.NumberFormat
import java.util.Locale

enum class TransactionType { COMMISSION, REFERRAL, BONUS }
enum class TransactionStatus { PENDING, VERIFIED, REJECTED }
enum class PayoutMethodType { UPI, BANK }
enum class PayoutStatus { PENDING, PROCESSING, COMPLETED, REJECTED }
enum class ReferralStatus { INVITED, JOINED, REWARDED }

inline fun <reified T : Enum<T>> safeEnum(raw: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val kycVerified: Boolean = false,
    val biometricEnabled: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutMethod(
    @DocumentId val id: String = "",
    val userId: String = "",
    val methodType: String = PayoutMethodType.UPI.name,
    val label: String = "",
    val upiId: String? = null,
    val bankName: String? = null,
    val accountNumberLast4: String? = null,
    val ifscCode: String? = null,
    val accountHolderName: String = "",
    val primaryMethod: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val inviteeName: String = "",
    val inviteeContact: String? = null,
    val referralCodeUsed: String = "",
    val bonusPaise: Long = 0L,
    val status: String = ReferralStatus.INVITED.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommissionTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = TransactionType.COMMISSION.name,
    val title: String = "",
    val clientOrOrderRef: String? = null,
    val amountPaise: Long = 0L,
    val commissionRateBps: Long = 0L,
    val status: String = TransactionStatus.PENDING.name,
    val notes: String? = null,
    val verifiedBy: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amountPaise: Long = 0L,
    val methodType: String = PayoutMethodType.UPI.name,
    val destinationDisplay: String = "",
    val status: String = PayoutStatus.PENDING.name,
    val referenceUtr: String? = null,
    val adminNote: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AppNotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Timestamp
)

data class DashboardSummary(
    val verifiedEarningsPaise: Long = 0L,
    val pendingEarningsPaise: Long = 0L,
    val commissionEarningsPaise: Long = 0L,
    val referralEarningsPaise: Long = 0L,
    val bonusEarningsPaise: Long = 0L,
    val totalPaidOutPaise: Long = 0L,
    val reservedPendingPayoutPaise: Long = 0L,
    val availableBalancePaise: Long = 0L
)

data class DailyEarningsPoint(
    val dayLabel: String,
    val amountPaise: Long
)

fun formatInr(paise: Long): String {
    val rupees = paise / 100.0
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(rupees)
}

fun buildReferralCodeForUid(uid: String): String {
    val clean = uid.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    val suffix = if (clean.length >= 6) clean.take(6) else clean.padEnd(6, 'X')
    return "HOYO$suffix"
}
