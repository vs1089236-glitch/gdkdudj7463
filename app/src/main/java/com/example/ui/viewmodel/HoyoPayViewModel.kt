package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotificationItem
import com.example.data.model.CommissionTransaction
import com.example.data.model.DailyEarningsPoint
import com.example.data.model.DashboardSummary
import com.example.data.model.PayoutMethod
import com.example.data.model.PayoutMethodType
import com.example.data.model.PayoutRequest
import com.example.data.model.PayoutStatus
import com.example.data.model.ReferralRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.formatInr
import com.example.data.repository.HoyoPayRepository
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HoyoDashboardData(
    val profile: UserProfile?,
    val transactions: List<CommissionTransaction>,
    val payoutMethods: List<PayoutMethod>,
    val payoutRequests: List<PayoutRequest>,
    val referrals: List<ReferralRecord>,
    val summary: DashboardSummary,
    val sevenDayTrend: List<DailyEarningsPoint>,
    val isAdmin: Boolean,
    val adminTransactions: List<CommissionTransaction>,
    val adminPayoutRequests: List<PayoutRequest>
)

sealed interface UiEvent {
    data class Snackbar(val message: String) : UiEvent
    data class DeviceNotification(val title: String, val body: String) : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HoyoPayViewModel(
    private val repository: HoyoPayRepository,
    private val currentUserId: String,
    private val pendingReferralCode: String,
    private val pendingMobileNumber: String,
    private val onClearPendingAuthInputs: suspend () -> Unit
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notifications: StateFlow<List<AppNotificationItem>> = _notifications.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null && user.uid == currentUserId) {
                val res = repository.ensureUserProfile(
                    user = user,
                    pendingReferralCode = pendingReferralCode,
                    pendingPhone = pendingMobileNumber
                )
                if (res.isSuccess) {
                    onClearPendingAuthInputs()
                }
                _isAdmin.value = repository.checkIsAdmin(user)
            }
        }
    }

    private fun <T> Flow<T>.unwrapFirestoreErrors(fallbackMsg: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMsg, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
            }

    private val profileFlow = repository.observeUserProfile(currentUserId)
        .unwrapFirestoreErrors("Failed to load profile")

    private val transactionsFlow = repository.observeTransactions(currentUserId)
        .unwrapFirestoreErrors("Failed to load transactions")

    private val payoutMethodsFlow = repository.observePayoutMethods(currentUserId)
        .unwrapFirestoreErrors("Failed to load payout methods")

    private val payoutRequestsFlow = repository.observePayoutRequests(currentUserId)
        .unwrapFirestoreErrors("Failed to load payout requests")

    private val referralsFlow = repository.observeReferrals(currentUserId)
        .unwrapFirestoreErrors("Failed to load referrals")

    private val adminTransactionsFlow: Flow<List<CommissionTransaction>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) {
                repository.observeAllTransactionsAdmin()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Admin transactions stream error", firestoreError ?: error)
                        emit(emptyList())
                    }
            } else {
                flowOf(emptyList())
            }
        }

    private val adminPayoutsFlow: Flow<List<PayoutRequest>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) {
                repository.observeAllPayoutRequestsAdmin()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Admin payouts stream error", firestoreError ?: error)
                        emit(emptyList())
                    }
            } else {
                flowOf(emptyList())
            }
        }

    private val userStreamsFlow = combine(
        profileFlow,
        transactionsFlow,
        payoutMethodsFlow,
        payoutRequestsFlow,
        referralsFlow
    ) { profileState, txState, methodsState, payoutsState, referralsState ->
        listOf(profileState, txState, methodsState, payoutsState, referralsState)
    }

    val uiState: StateFlow<UiState<HoyoDashboardData>> = combine(
        userStreamsFlow,
        _isAdmin,
        adminTransactionsFlow,
        adminPayoutsFlow
    ) { states, admin, adminTxs, adminPayouts ->
        val firstError = states.filterIsInstance<UiState.Error>().firstOrNull()
        if (firstError != null) {
            return@combine UiState.Error(firstError.message)
        }
        if (states.any { it is UiState.Loading }) {
            return@combine UiState.Loading
        }
        @Suppress("UNCHECKED_CAST")
        val profile = (states[0] as UiState.Success<UserProfile?>).data
        @Suppress("UNCHECKED_CAST")
        val txs = (states[1] as UiState.Success<List<CommissionTransaction>>).data
        @Suppress("UNCHECKED_CAST")
        val methods = (states[2] as UiState.Success<List<PayoutMethod>>).data
        @Suppress("UNCHECKED_CAST")
        val payouts = (states[3] as UiState.Success<List<PayoutRequest>>).data
        @Suppress("UNCHECKED_CAST")
        val referrals = (states[4] as UiState.Success<List<ReferralRecord>>).data

        val summary = computeSummary(txs, payouts)
        val trend = computeSevenDayTrend(txs)

        UiState.Success(
            HoyoDashboardData(
                profile = profile,
                transactions = txs,
                payoutMethods = methods,
                payoutRequests = payouts,
                referrals = referrals,
                summary = summary,
                sevenDayTrend = trend,
                isAdmin = admin,
                adminTransactions = adminTxs,
                adminPayoutRequests = adminPayouts
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    private fun computeSummary(
        txs: List<CommissionTransaction>,
        payouts: List<PayoutRequest>
    ): DashboardSummary {
        var verified = 0L
        var pending = 0L
        var commissions = 0L
        var referrals = 0L
        var bonuses = 0L

        for (tx in txs) {
            when (tx.status) {
                TransactionStatus.VERIFIED.name -> {
                    verified += tx.amountPaise
                    when (tx.type) {
                        TransactionType.COMMISSION.name -> commissions += tx.amountPaise
                        TransactionType.REFERRAL.name -> referrals += tx.amountPaise
                        TransactionType.BONUS.name -> bonuses += tx.amountPaise
                    }
                }
                TransactionStatus.PENDING.name -> pending += tx.amountPaise
            }
        }

        var paidOut = 0L
        var reservedPending = 0L
        for (p in payouts) {
            when (p.status) {
                PayoutStatus.COMPLETED.name -> paidOut += p.amountPaise
                PayoutStatus.PENDING.name, PayoutStatus.PROCESSING.name -> reservedPending += p.amountPaise
            }
        }

        val available = (verified - paidOut - reservedPending).coerceAtLeast(0L)
        return DashboardSummary(
            verifiedEarningsPaise = verified,
            pendingEarningsPaise = pending,
            commissionEarningsPaise = commissions,
            referralEarningsPaise = referrals,
            bonusEarningsPaise = bonuses,
            totalPaidOutPaise = paidOut,
            reservedPendingPayoutPaise = reservedPending,
            availableBalancePaise = available
        )
    }

    private fun computeSevenDayTrend(txs: List<CommissionTransaction>): List<DailyEarningsPoint> {
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val totalsByKey = mutableMapOf<String, Long>()
        for (tx in txs) {
            if (tx.status == TransactionStatus.REJECTED.name) continue
            val date = tx.createdAt?.toDate() ?: continue
            val key = keyFmt.format(date)
            totalsByKey[key] = (totalsByKey[key] ?: 0L) + tx.amountPaise
        }
        val cal = Calendar.getInstance()
        val points = mutableListOf<DailyEarningsPoint>()
        for (offset in 6 downTo 0) {
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = keyFmt.format(dayCal.time)
            val label = dayFmt.format(dayCal.time)
            points.add(DailyEarningsPoint(dayLabel = label, amountPaise = totalsByKey[key] ?: 0L))
        }
        return points
    }

    fun addTransaction(
        userName: String,
        type: TransactionType,
        title: String,
        clientOrOrderRef: String,
        amountRupeesStr: String,
        ratePercentStr: String,
        notes: String
    ) {
        val amountDouble = amountRupeesStr.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0.0 || amountDouble > 1_000_000.0) {
            emitSnackbar("Enter a valid amount between ₹1 and ₹10,00,000.")
            return
        }
        val ratePercent = ratePercentStr.toDoubleOrNull() ?: 0.0
        if (ratePercent < 0.0 || ratePercent > 100.0) {
            emitSnackbar("Commission rate must be between 0% and 100%.")
            return
        }
        val amountPaise = Math.round(amountDouble * 100.0)
        val rateBps = Math.round(ratePercent * 100.0)

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.addTransaction(
                userId = currentUserId,
                userName = userName,
                type = type,
                title = title,
                clientOrOrderRef = clientOrOrderRef,
                amountPaise = amountPaise,
                commissionRateBps = rateBps,
                notes = notes
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    val body = "${title.trim()} (${formatInr(amountPaise)}) submitted for verification."
                    pushAlert("Activity Logged", body)
                    emitSnackbar(body)
                },
                onFailure = { e ->
                    emitSnackbar(e.localizedMessage ?: "Failed to submit activity")
                }
            )
        }
    }

    fun deletePendingTransaction(txId: String) {
        viewModelScope.launch {
            repository.deletePendingTransaction(currentUserId, txId).fold(
                onSuccess = { emitSnackbar("Pending entry removed") },
                onFailure = { e -> emitSnackbar(e.localizedMessage ?: "Unable to delete entry") }
            )
        }
    }

    fun addPayoutMethod(
        methodType: PayoutMethodType,
        label: String,
        accountHolderName: String,
        upiId: String,
        bankName: String,
        accountLast4: String,
        ifscCode: String,
        primaryMethod: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.addPayoutMethod(
                userId = currentUserId,
                methodType = methodType,
                label = label,
                accountHolderName = accountHolderName,
                upiId = upiId,
                bankName = bankName,
                accountNumberLast4 = accountLast4,
                ifscCode = ifscCode,
                primaryMethod = primaryMethod
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    pushAlert("Payout Account Added", "${label.trim()} saved securely.")
                    emitSnackbar("Payout method saved")
                },
                onFailure = { e ->
                    emitSnackbar(e.localizedMessage ?: "Failed to save payout method")
                }
            )
        }
    }

    fun deletePayoutMethod(methodId: String) {
        viewModelScope.launch {
            repository.deletePayoutMethod(currentUserId, methodId).fold(
                onSuccess = { emitSnackbar("Payout method removed") },
                onFailure = { e -> emitSnackbar(e.localizedMessage ?: "Failed to remove method") }
            )
        }
    }

    fun requestPayout(
        userName: String,
        amountRupeesStr: String,
        method: PayoutMethod?,
        availableBalancePaise: Long
    ) {
        if (method == null) {
            emitSnackbar("Please add and select a UPI or Bank account first.")
            return
        }
        val amountDouble = amountRupeesStr.toDoubleOrNull()
        if (amountDouble == null || amountDouble < 100.0 || amountDouble > 500_000.0) {
            emitSnackbar("Payout amount must be between ₹100 and ₹5,00,000.")
            return
        }
        val amountPaise = Math.round(amountDouble * 100.0)
        if (amountPaise > availableBalancePaise) {
            emitSnackbar("Requested amount exceeds available verified balance (${formatInr(availableBalancePaise)}).")
            return
        }
        val methodType = if (method.methodType == PayoutMethodType.BANK.name) {
            PayoutMethodType.BANK
        } else {
            PayoutMethodType.UPI
        }
        val destination = if (methodType == PayoutMethodType.UPI) {
            method.upiId ?: method.label
        } else {
            "${method.bankName ?: "Bank"} •••• ${method.accountNumberLast4 ?: ""}".trim()
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createPayoutRequest(
                userId = currentUserId,
                userName = userName,
                amountPaise = amountPaise,
                methodType = methodType,
                destinationDisplay = destination,
                availableBalancePaise = availableBalancePaise
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    val body = "${formatInr(amountPaise)} payout requested to $destination."
                    pushAlert("Payout Requested", body)
                    emitSnackbar(body)
                },
                onFailure = { e ->
                    emitSnackbar(e.localizedMessage ?: "Failed to create payout request")
                }
            )
        }
    }

    fun addReferralInvite(inviteeName: String, inviteeContact: String, myReferralCode: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.addReferralInvite(
                userId = currentUserId,
                inviteeName = inviteeName,
                inviteeContact = inviteeContact,
                referralCodeUsed = myReferralCode
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    pushAlert("Referral Tracked", "Invite logged for ${inviteeName.trim()}.")
                    emitSnackbar("Referral invite logged")
                },
                onFailure = { e ->
                    emitSnackbar(e.localizedMessage ?: "Failed to add referral invite")
                }
            )
        }
    }

    fun updateProfile(
        displayName: String,
        phone: String,
        referredByCode: String?,
        biometricEnabled: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateProfile(
                userId = currentUserId,
                displayName = displayName,
                phone = phone,
                referredByCode = referredByCode,
                biometricEnabled = biometricEnabled
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = { emitSnackbar("Profile updated") },
                onFailure = { e -> emitSnackbar(e.localizedMessage ?: "Failed to update profile") }
            )
        }
    }

    fun adminVerifyTransaction(txId: String, status: TransactionStatus, note: String? = null) {
        val email = Firebase.auth.currentUser?.email ?: HoyoPayRepository.ADMIN_EMAIL
        viewModelScope.launch {
            repository.adminVerifyTransaction(txId, status, email, note).fold(
                onSuccess = {
                    pushAlert("Admin Update", "Transaction marked as ${status.name}.")
                    emitSnackbar("Transaction marked ${status.name}")
                },
                onFailure = { e -> emitSnackbar(e.localizedMessage ?: "Admin update failed") }
            )
        }
    }

    fun adminUpdatePayout(
        payoutId: String,
        status: PayoutStatus,
        utr: String?,
        note: String?
    ) {
        viewModelScope.launch {
            repository.adminUpdatePayoutRequest(payoutId, status, utr, note).fold(
                onSuccess = {
                    pushAlert("Payout Updated", "Payout status updated to ${status.name}.")
                    emitSnackbar("Payout marked ${status.name}")
                },
                onFailure = { e -> emitSnackbar(e.localizedMessage ?: "Payout update failed") }
            )
        }
    }

    private fun pushAlert(title: String, body: String) {
        val alert = AppNotificationItem(
            id = "${System.currentTimeMillis()}_${_notifications.value.size}",
            title = title,
            body = body,
            timestamp = Timestamp.now()
        )
        _notifications.value = listOf(alert) + _notifications.value.take(24)
        _events.tryEmit(UiEvent.DeviceNotification(title, body))
    }

    fun emitSnackbar(message: String) {
        _events.tryEmit(UiEvent.Snackbar(message))
    }

    private companion object {
        const val TAG = "HoyoPayVM"
    }
}
