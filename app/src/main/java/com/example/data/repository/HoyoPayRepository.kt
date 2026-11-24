package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CommissionTransaction
import com.example.data.model.PayoutMethod
import com.example.data.model.PayoutMethodType
import com.example.data.model.PayoutRequest
import com.example.data.model.PayoutStatus
import com.example.data.model.ReferralRecord
import com.example.data.model.ReferralStatus
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.buildReferralCodeForUid
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HoyoPayRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    suspend fun checkIsAdmin(user: FirebaseUser?): Boolean {
        if (user == null) return false
        if (user.isEmailVerified && user.email == ADMIN_EMAIL) return true
        return try {
            val snap = db.collection("admins").document(user.uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ensureUserProfile(
        user: FirebaseUser,
        pendingReferralCode: String? = null,
        pendingPhone: String? = null
    ): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val cleanReferral = pendingReferralCode?.trim()?.uppercase()?.take(20)?.takeIf { it.isNotEmpty() }
            val cleanPhone = pendingPhone?.trim()?.take(20)?.takeIf { it.isNotEmpty() }

            if (!existing.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "Hoyo Member").take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to user.email?.take(120),
                    "phone" to cleanPhone,
                    "referralCode" to buildReferralCodeForUid(uid),
                    "referredByCode" to cleanReferral,
                    "kycVerified" to false,
                    "biometricEnabled" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else if (cleanReferral != null || cleanPhone != null) {
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanReferral != null && existing.getString("referredByCode").isNullOrBlank()) {
                    updates["referredByCode"] = cleanReferral
                }
                if (cleanPhone != null && existing.getString("phone").isNullOrBlank()) {
                    updates["phone"] = cleanPhone
                }
                if (updates.size > 1) {
                    docRef.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        phone: String,
        referredByCode: String?,
        biometricEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user profile update." }
            val trimmedName = displayName.trim()
            require(trimmedName.length in 1..80) { "Display name must be 1-80 characters." }

            val updates = mutableMapOf<String, Any>(
                "displayName" to trimmedName,
                "biometricEnabled" to biometricEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanPhone = phone.trim().take(20)
            if (cleanPhone.isNotEmpty()) {
                updates["phone"] = cleanPhone
            }
            val cleanRef = referredByCode?.trim()?.uppercase()?.take(20)
            if (!cleanRef.isNullOrEmpty()) {
                updates["referredByCode"] = cleanRef
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeTransactions(userId: String): Flow<List<CommissionTransaction>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "transactions"
        emitAll(
            db.collection("transactions")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommissionTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllTransactionsAdmin(): Flow<List<CommissionTransaction>> = flow {
        val uid = requireUserId()
        val path = "transactions"
        emitAll(
            db.collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommissionTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun addTransaction(
        userId: String,
        userName: String,
        type: TransactionType,
        title: String,
        clientOrOrderRef: String?,
        amountPaise: Long,
        commissionRateBps: Long,
        notes: String?
    ): Result<Unit> {
        var path = "transactions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user transaction write." }
            val cleanTitle = title.trim()
            require(cleanTitle.length in 1..120) { "Title must be 1-120 characters." }
            require(amountPaise in 1L..100_000_000L) { "Amount must be between ₹0.01 and ₹10,00,000." }
            require(commissionRateBps in 0L..10_000L) { "Commission rate must be 0-100%." }

            val docRef = db.collection("transactions").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "userName" to userName.trim().ifEmpty { "Hoyo Member" }.take(80),
                "type" to type.name,
                "title" to cleanTitle,
                "clientOrOrderRef" to clientOrOrderRef?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
                "amountPaise" to amountPaise,
                "commissionRateBps" to commissionRateBps,
                "status" to TransactionStatus.PENDING.name,
                "notes" to notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePendingTransaction(userId: String, txId: String): Result<Unit> {
        val path = "transactions/$txId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user delete." }
            require(txId.isNotBlank())
            db.collection("transactions").document(txId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminVerifyTransaction(
        txId: String,
        newStatus: TransactionStatus,
        verifierEmail: String,
        notes: String? = null
    ): Result<Unit> {
        val path = "transactions/$txId"
        return try {
            requireUserId()
            require(txId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "verifiedBy" to verifierEmail.take(120),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanNotes = notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() }
            if (cleanNotes != null) {
                updates["notes"] = cleanNotes
            }
            db.collection("transactions").document(txId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePayoutMethods(userId: String): Flow<List<PayoutMethod>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/payoutMethods"
        emitAll(
            db.collection("users").document(userId).collection("payoutMethods")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PayoutMethod::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addPayoutMethod(
        userId: String,
        methodType: PayoutMethodType,
        label: String,
        accountHolderName: String,
        upiId: String?,
        bankName: String?,
        accountNumberLast4: String?,
        ifscCode: String?,
        primaryMethod: Boolean
    ): Result<Unit> {
        var path = "users/$userId/payoutMethods"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanLabel = label.trim()
            val cleanHolder = accountHolderName.trim()
            require(cleanLabel.length in 1..60) { "Label must be 1-60 characters." }
            require(cleanHolder.length in 1..80) { "Account holder name must be 1-80 characters." }
            if (methodType == PayoutMethodType.UPI) {
                require(!upiId.isNullOrBlank() && upiId.contains("@")) { "Enter a valid UPI ID (e.g. name@bank)." }
            } else {
                require(!bankName.isNullOrBlank()) { "Enter bank name." }
                require(!accountNumberLast4.isNullOrBlank() && accountNumberLast4.trim().length <= 8) {
                    "Enter valid last 4 digits of account."
                }
                require(!ifscCode.isNullOrBlank()) { "Enter IFSC code." }
            }

            val docRef = db.collection("users").document(uid).collection("payoutMethods").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "methodType" to methodType.name,
                "label" to cleanLabel,
                "accountHolderName" to cleanHolder,
                "upiId" to upiId?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
                "bankName" to bankName?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
                "accountNumberLast4" to accountNumberLast4?.trim()?.take(8)?.takeIf { it.isNotEmpty() },
                "ifscCode" to ifscCode?.trim()?.uppercase()?.take(20)?.takeIf { it.isNotEmpty() },
                "primaryMethod" to primaryMethod,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePayoutMethod(userId: String, methodId: String): Result<Unit> {
        val path = "users/$userId/payoutMethods/$methodId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("payoutMethods").document(methodId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePayoutRequests(userId: String): Flow<List<PayoutRequest>> = flow {
        require(userId.isNotBlank())
        val path = "payoutRequests"
        emitAll(
            db.collection("payoutRequests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PayoutRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllPayoutRequestsAdmin(): Flow<List<PayoutRequest>> = flow {
        val uid = requireUserId()
        val path = "payoutRequests"
        emitAll(
            db.collection("payoutRequests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PayoutRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createPayoutRequest(
        userId: String,
        userName: String,
        amountPaise: Long,
        methodType: PayoutMethodType,
        destinationDisplay: String,
        availableBalancePaise: Long
    ): Result<Unit> {
        var path = "payoutRequests"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(amountPaise in 10_000L..50_000_000L) {
                "Payout amount must be between ₹100 and ₹5,00,000."
            }
            require(amountPaise <= availableBalancePaise) {
                "Insufficient verified balance available for payout."
            }
            val cleanDest = destinationDisplay.trim()
            require(cleanDest.length in 3..100) { "Select a valid payout destination." }

            val docRef = db.collection("payoutRequests").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "userName" to userName.trim().ifEmpty { "Hoyo Member" }.take(80),
                "amountPaise" to amountPaise,
                "methodType" to methodType.name,
                "destinationDisplay" to cleanDest,
                "status" to PayoutStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdatePayoutRequest(
        payoutId: String,
        newStatus: PayoutStatus,
        referenceUtr: String?,
        adminNote: String?
    ): Result<Unit> {
        val path = "payoutRequests/$payoutId"
        return try {
            requireUserId()
            require(payoutId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanUtr = referenceUtr?.trim()?.take(80)?.takeIf { it.isNotEmpty() }
            if (cleanUtr != null) updates["referenceUtr"] = cleanUtr
            val cleanNote = adminNote?.trim()?.take(200)?.takeIf { it.isNotEmpty() }
            if (cleanNote != null) updates["adminNote"] = cleanNote

            db.collection("payoutRequests").document(payoutId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeReferrals(userId: String): Flow<List<ReferralRecord>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/referrals"
        emitAll(
            db.collection("users").document(userId).collection("referrals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReferralRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addReferralInvite(
        userId: String,
        inviteeName: String,
        inviteeContact: String?,
        referralCodeUsed: String,
        bonusPaise: Long = 5_000L
    ): Result<Unit> {
        var path = "users/$userId/referrals"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = inviteeName.trim()
            require(cleanName.length in 1..80) { "Invitee name must be 1-80 characters." }
            val cleanCode = referralCodeUsed.trim().uppercase()
            require(cleanCode.length in 4..20) { "Invalid referral code." }

            val docRef = db.collection("users").document(uid).collection("referrals").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "inviteeName" to cleanName,
                "inviteeContact" to inviteeContact?.trim()?.take(60)?.takeIf { it.isNotEmpty() },
                "referralCodeUsed" to cleanCode,
                "bonusPaise" to bonusPaise.coerceIn(0L, 1_000_000L),
                "status" to ReferralStatus.INVITED.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    companion object {
        const val ADMIN_EMAIL = "vs1089236@gmail.com"
    }
}
