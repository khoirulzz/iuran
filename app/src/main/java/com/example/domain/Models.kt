package com.example.domain

enum class UserRole { ADMIN, OFFICER }
enum class ActivityStatus { DRAFT, ACTIVE, EXPIRED, COMPLETED, ARCHIVED }
enum class PaymentStatus { UNPAID, PARTIAL, PAID, OVERPAID }
enum class TransactionType { PAYMENT, REVERSAL }
enum class PaymentMethod { CASH, TRANSFER, QRIS, OTHER }
enum class SyncStatus { LOCAL_PENDING, SYNCING, SYNCED, FAILED }

data class IuranActivity(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val startAtEpochMs: Long = 0L,
    val deadlineAtEpochMs: Long = 0L,
    val defaultTargetAmount: Long = 0L,
    val allowLatePayment: Boolean = true,
    val status: ActivityStatus = ActivityStatus.DRAFT,
    val assignedOfficerIds: List<String> = emptyList(),
    val participantResidentIds: List<String> = emptyList(),
    val createdBy: String = "ADMIN",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ActivityParticipant(
    val id: String = "",
    val activityId: String = "",
    val residentId: String = "",
    val targetAmount: Long = 0L,
    val isIncluded: Boolean = true,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class PaymentTransaction(
    val transactionId: String = "",
    val type: TransactionType = TransactionType.PAYMENT,
    val activityId: String = "",
    val residentId: String = "",
    val officerId: String = "",
    val amount: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val deviceId: String = "",
    val localSequence: Long = 0L,
    val paidAtDeviceEpochMs: Long = 0L,
    val paidDateKey: String = "",
    val timezoneId: String = "Asia/Jakarta",
    val relatedTransactionId: String? = null,
    val createdByRole: UserRole = UserRole.OFFICER,
    val schemaVersion: Int = 1
)

data class Officer(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val usernameNormalized: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val passwordIterations: Int = 120000,
    val phone: String = "",
    val isActive: Boolean = true,
    val assignedActivityIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class Resident(
    val id: String = "",
    val name: String = "",
    val nameNormalized: String = "",
    val houseNumber: String = "",
    val block: String = "",
    val address: String = "",
    val phone: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

// Computed / UI state models
data class ResidentPaymentSummary(
    val resident: Resident,
    val participant: ActivityParticipant,
    val totalPaid: Long,
    val paymentStatus: PaymentStatus,
    val recentTransactions: List<PaymentTransaction> = emptyList()
) {
    val remaining: Long get() = participant.targetAmount - totalPaid
    val progressFraction: Float get() = if (participant.targetAmount == 0L) 0f
        else (totalPaid.toFloat() / participant.targetAmount.toFloat()).coerceIn(0f, 1f)
}

data class ActivitySummary(
    val activity: IuranActivity,
    val totalCollected: Long,
    val totalTarget: Long,
    val countPaid: Int,
    val countPartial: Int,
    val countUnpaid: Int,
    val countOverpaid: Int
) {
    val progressFraction: Float get() = if (totalTarget == 0L) 0f
        else (totalCollected.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f)
    val progressPercent: Int get() = (progressFraction * 100).toInt()
}
