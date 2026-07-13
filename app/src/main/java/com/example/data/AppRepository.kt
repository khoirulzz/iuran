package com.example.data

import com.example.domain.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppRepository(
    private val firestore: FirebaseFirestore,
    private val sessionStore: SessionStore
) {

    // ===================== AUTH =====================

    /** Login petugas: ambil dari Firestore, verifikasi hash password PBKDF2 */
    suspend fun loginOfficer(username: String, passwordRaw: String): Result<Officer> = runCatching {
        val snapshot = firestore.collection("officers")
            .whereEqualTo("usernameNormalized", username.trim().lowercase())
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) throw Exception("Username atau password tidak sesuai.")

        val doc = snapshot.documents.first()
        val officer = doc.toObject(Officer::class.java)!!.copy(id = doc.id)

        if (!officer.isActive) throw Exception("Akun petugas sudah dinonaktifkan.")

        // Verifikasi password dengan PBKDF2
        val isPasswordValid = verifyPbkdf2Password(
            password      = passwordRaw,
            storedHash    = officer.passwordHash,
            storedSalt    = officer.passwordSalt,
            iterations    = officer.passwordIterations
        )
        if (!isPasswordValid) throw Exception("Username atau password tidak sesuai.")

        officer
    }

    // ===================== RESIDENTS =====================

    suspend fun getResidents(activeOnly: Boolean = false): List<Resident> = runCatching {
        val snapshot = firestore.collection("residents").get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Resident::class.java)?.copy(id = doc.id)
        }
        .filter { if (activeOnly) it.isActive else true }
        .sortedBy { it.nameNormalized }
    }.getOrDefault(emptyList())

    suspend fun getResident(residentId: String): Resident? = runCatching {
        val doc = firestore.collection("residents").document(residentId).get().await()
        if (doc.exists()) doc.toObject(Resident::class.java)!!.copy(id = doc.id) else null
    }.getOrNull()

    suspend fun saveResident(resident: Resident): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val normalized = resident.name.trim().lowercase()
        val residentId: String
        if (resident.id.isEmpty()) {
            val ref = firestore.collection("residents").document()
            residentId = ref.id
            ref.set(resident.copy(
                id = residentId,
                nameNormalized = normalized,
                createdAt = now,
                updatedAt = now
            )).await()
        } else {
            residentId = resident.id
            firestore.collection("residents").document(residentId)
                .set(resident.copy(nameNormalized = normalized, updatedAt = now))
                .await()
        }

        if (resident.isActive) {
            val activeActivities = getActivities().filter { it.status == ActivityStatus.ACTIVE }
            for (act in activeActivities) {
                val existing = getParticipants(act.id).map { it.residentId }.toSet()
                if (residentId !in existing) {
                    upsertParticipants(act.id, listOf(residentId), act.defaultTargetAmount)
                }
            }
        }
        residentId
    }

    suspend fun deactivateResident(residentId: String): Result<Unit> = runCatching {
        firestore.collection("residents").document(residentId)
            .update("isActive", false, "updatedAt", System.currentTimeMillis())
            .await()
    }

    // ===================== OFFICERS =====================

    suspend fun getOfficers(): List<Officer> = runCatching {
        firestore.collection("officers").orderBy("name").get().await().documents.mapNotNull { doc ->
            doc.toObject(Officer::class.java)?.copy(id = doc.id)
        }
    }.getOrDefault(emptyList())

    suspend fun getOfficer(officerId: String): Officer? = runCatching {
        val doc = firestore.collection("officers").document(officerId).get().await()
        if (doc.exists()) doc.toObject(Officer::class.java)!!.copy(id = doc.id) else null
    }.getOrNull()

    suspend fun createOfficer(
        name: String,
        username: String,
        passwordRaw: String,
        phone: String
    ): Result<String> = runCatching {
        val usernameNorm = username.trim().lowercase()
        // Periksa keunikan username
        val existing = firestore.collection("officers")
            .whereEqualTo("usernameNormalized", usernameNorm).limit(1).get().await()
        if (!existing.isEmpty) throw Exception("Username '$username' sudah digunakan.")

        val (hash, salt) = hashPbkdf2Password(passwordRaw)
        val now = System.currentTimeMillis()
        val ref = firestore.collection("officers").document()
        ref.set(Officer(
            id = ref.id,
            name = name.trim(),
            username = username.trim(),
            usernameNormalized = usernameNorm,
            passwordHash = hash,
            passwordSalt = salt,
            passwordIterations = 120000,
            phone = phone.trim(),
            isActive = true,
            createdAt = now,
            updatedAt = now
        )).await()
        ref.id
    }

    suspend fun updateOfficer(officer: Officer): Result<Unit> = runCatching {
        firestore.collection("officers").document(officer.id)
            .set(officer.copy(updatedAt = System.currentTimeMillis()))
            .await()
    }

    suspend fun resetOfficerPassword(officerId: String, newPasswordRaw: String): Result<Unit> = runCatching {
        val (hash, salt) = hashPbkdf2Password(newPasswordRaw)
        firestore.collection("officers").document(officerId)
            .update(
                "passwordHash", hash,
                "passwordSalt", salt,
                "passwordIterations", 120000,
                "updatedAt", System.currentTimeMillis()
            ).await()
    }

    suspend fun setOfficerActiveStatus(officerId: String, isActive: Boolean): Result<Unit> = runCatching {
        firestore.collection("officers").document(officerId)
            .update("isActive", isActive, "updatedAt", System.currentTimeMillis())
            .await()
    }

    // ===================== ACTIVITIES =====================

    suspend fun getActivities(): List<IuranActivity> = runCatching {
        firestore.collection("activities")
            .orderBy("startAtEpochMs", Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { doc ->
                doc.toObject(IuranActivity::class.java)?.copy(id = doc.id)
            }
    }.getOrDefault(emptyList())

    suspend fun getActivityById(activityId: String): IuranActivity? = runCatching {
        val doc = firestore.collection("activities").document(activityId).get().await()
        if (doc.exists()) doc.toObject(IuranActivity::class.java)!!.copy(id = doc.id) else null
    }.getOrNull()

    suspend fun getActiveActivitiesForOfficer(officerId: String): List<IuranActivity> = runCatching {
        // Coba query dengan string "ACTIVE" (cara Firestore menyimpan enum Kotlin)
        val snapshot = firestore.collection("activities")
            .whereEqualTo("status", ActivityStatus.ACTIVE.name)
            .get().await()
        val fromEnum = snapshot.documents.mapNotNull { doc ->
            doc.toObject(IuranActivity::class.java)?.copy(id = doc.id)
        }
        // Fallback: jika kosong, ambil semua kegiatan dan filter status di sisi klien
        // (menangani kasus dimana status disimpan sebagai objek atau format lain)
        val activities = fromEnum.ifEmpty {
            firestore.collection("activities")
                .get().await().documents.mapNotNull { doc ->
                    doc.toObject(IuranActivity::class.java)?.copy(id = doc.id)
                }.filter { it.status == ActivityStatus.ACTIVE }
        }
        activities.filter { activity ->
            activity.assignedOfficerIds.isEmpty() || activity.assignedOfficerIds.contains(officerId)
        }.sortedByDescending { it.startAtEpochMs }
    }.getOrDefault(emptyList())

    suspend fun saveActivity(activity: IuranActivity): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val activityId: String
        if (activity.id.isEmpty()) {
            val ref = firestore.collection("activities").document()
            activityId = ref.id
            ref.set(activity.copy(id = activityId, createdAt = now, updatedAt = now)).await()
        } else {
            activityId = activity.id
            firestore.collection("activities").document(activityId)
                .set(activity.copy(updatedAt = now)).await()
        }

        val existingParticipants = getParticipants(activityId).map { it.residentId }.toSet()
        val activeResidents = getResidents(activeOnly = true)
        val missingResidentIds = activeResidents.map { it.id }.filter { it !in existingParticipants }
        if (missingResidentIds.isNotEmpty()) {
            upsertParticipants(activityId, missingResidentIds, activity.defaultTargetAmount)
        }
        activityId
    }

    // ===================== PARTICIPANTS =====================

    suspend fun getParticipants(activityId: String): List<ActivityParticipant> = runCatching {
        val snapshot = firestore.collection("activity_participants")
            .whereEqualTo("activityId", activityId)
            .get().await()
        val docs = snapshot.documents.mapNotNull { doc ->
            doc.toObject(ActivityParticipant::class.java)?.copy(id = doc.id)
        }.filter { it.isIncluded }
        if (docs.isNotEmpty()) {
            docs
        } else {
            val activity = getActivityById(activityId)
            val residents = getResidents(activeOnly = true)
            if (activity != null && residents.isNotEmpty()) {
                val residentIds = residents.map { it.id }
                upsertParticipants(activityId, residentIds, activity.defaultTargetAmount)
                residents.map { res ->
                    ActivityParticipant(
                        id = "${activityId}_${res.id}",
                        activityId = activityId,
                        residentId = res.id,
                        targetAmount = activity.defaultTargetAmount,
                        isIncluded = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            } else {
                emptyList()
            }
        }
    }.getOrDefault(emptyList())

    suspend fun saveParticipant(participant: ActivityParticipant): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        if (participant.id.isEmpty()) {
            val ref = firestore.collection("activity_participants").document()
            ref.set(participant.copy(id = ref.id, createdAt = now, updatedAt = now)).await()
            ref.id
        } else {
            firestore.collection("activity_participants").document(participant.id)
                .set(participant.copy(updatedAt = now)).await()
            participant.id
        }
    }

    suspend fun upsertParticipants(
        activityId: String,
        residentIds: List<String>,
        defaultTarget: Long
    ): Result<Unit> = runCatching {
        val batch = firestore.batch()
        val now = System.currentTimeMillis()
        for (residentId in residentIds) {
            val ref = firestore.collection("activity_participants")
                .document("${activityId}_${residentId}")
            batch.set(ref, ActivityParticipant(
                id = "${activityId}_${residentId}",
                activityId = activityId,
                residentId = residentId,
                targetAmount = defaultTarget,
                isIncluded = true,
                createdAt = now,
                updatedAt = now
            ))
        }
        batch.commit().await()
    }

    // ===================== TRANSACTIONS =====================

    suspend fun getTransactions(activityId: String, residentId: String): List<PaymentTransaction> = runCatching {
        firestore.collection("transactions")
            .whereEqualTo("activityId", activityId)
            .get().await().documents.mapNotNull { doc ->
                doc.toObject(PaymentTransaction::class.java)
            }.filter { it.residentId == residentId }
            .sortedByDescending { it.paidAtDeviceEpochMs }
    }.getOrDefault(emptyList())

    suspend fun getAllTransactions(activityId: String? = null): List<PaymentTransaction> = runCatching {
        val list = if (activityId != null) {
            firestore.collection("transactions")
                .whereEqualTo("activityId", activityId)
                .get().await().documents.mapNotNull { doc ->
                    doc.toObject(PaymentTransaction::class.java)
                }
        } else {
            firestore.collection("transactions")
                .limit(200)
                .get().await().documents.mapNotNull { doc ->
                    doc.toObject(PaymentTransaction::class.java)
                }
        }
        list.sortedByDescending { it.paidAtDeviceEpochMs }
    }.getOrDefault(emptyList())

    suspend fun getMyTransactions(officerId: String): List<PaymentTransaction> = runCatching {
        firestore.collection("transactions")
            .whereEqualTo("officerId", officerId)
            .get().await().documents.mapNotNull { doc ->
                doc.toObject(PaymentTransaction::class.java)
            }
            .sortedByDescending { it.paidAtDeviceEpochMs }
            .take(100)
    }.getOrDefault(emptyList())

    suspend fun createPayment(
        activityId: String,
        residentId: String,
        officerId: String,
        amount: Long,
        method: PaymentMethod,
        note: String
    ): Result<String> = runCatching {
        val sequence = sessionStore.reserveNextSequence()
        val deviceId = sessionStore.getDeviceId()
        val transactionId = "${deviceId}_${sequence.toString().padStart(8, '0')}"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
        sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        val dateKey = sdf.format(Date())

        val transaction = PaymentTransaction(
            transactionId = transactionId,
            type = TransactionType.PAYMENT,
            activityId = activityId,
            residentId = residentId,
            officerId = officerId,
            amount = amount,
            paymentMethod = method,
            note = note,
            deviceId = deviceId,
            localSequence = sequence,
            paidAtDeviceEpochMs = System.currentTimeMillis(),
            paidDateKey = dateKey,
            timezoneId = "Asia/Jakarta",
            createdByRole = UserRole.OFFICER,
            schemaVersion = 1
        )

        firestore.collection("transactions")
            .document(transactionId)
            .set(transaction)
            .await()

        transactionId
    }

    suspend fun createReversal(
        originalTransactionId: String,
        originalTransaction: PaymentTransaction,
        reason: String,
        adminId: String
    ): Result<String> = runCatching {
        val sequence = sessionStore.reserveNextSequence()
        val deviceId = sessionStore.getDeviceId()
        val reversalId = "${deviceId}_REV_${sequence.toString().padStart(8, '0')}"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
        sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        val dateKey = sdf.format(Date())

        val reversal = PaymentTransaction(
            transactionId = reversalId,
            type = TransactionType.REVERSAL,
            activityId = originalTransaction.activityId,
            residentId = originalTransaction.residentId,
            officerId = adminId,
            amount = -originalTransaction.amount, // Nilai negatif — ledger append-only
            paymentMethod = originalTransaction.paymentMethod,
            note = "REVERSAL: $reason",
            deviceId = deviceId,
            localSequence = sequence,
            paidAtDeviceEpochMs = System.currentTimeMillis(),
            paidDateKey = dateKey,
            timezoneId = "Asia/Jakarta",
            relatedTransactionId = originalTransactionId,
            createdByRole = UserRole.ADMIN,
            schemaVersion = 1
        )

        firestore.collection("transactions")
            .document(reversalId)
            .set(reversal)
            .await()

        reversalId
    }

    // ===================== REPORT HELPERS =====================

    /** Hitung total terbayar bersih untuk satu warga dalam satu kegiatan */
    suspend fun getResidentTotalPaid(activityId: String, residentId: String): Long {
        return getTransactions(activityId, residentId).sumOf { it.amount }
    }

    /** Bangun ResidentPaymentSummary untuk satu warga */
    suspend fun getResidentSummary(
        activityId: String,
        participant: ActivityParticipant
    ): ResidentPaymentSummary? {
        val resident = getResident(participant.residentId) ?: return null
        val txList = getTransactions(activityId, participant.residentId)
        val totalPaid = txList.sumOf { it.amount }
        val status = when {
            totalPaid <= 0L -> PaymentStatus.UNPAID
            totalPaid >= participant.targetAmount -> if (totalPaid > participant.targetAmount) PaymentStatus.OVERPAID else PaymentStatus.PAID
            else -> PaymentStatus.PARTIAL
        }
        return ResidentPaymentSummary(
            resident = resident,
            participant = participant,
            totalPaid = totalPaid,
            paymentStatus = status,
            recentTransactions = txList.take(3)
        )
    }

    /** Hitung rekapan satu kegiatan */
    suspend fun getActivitySummary(activity: IuranActivity): ActivitySummary = runCatching {
        val participants = getParticipants(activity.id)
        var totalCollected = 0L
        var totalTarget = 0L
        var countPaid = 0; var countPartial = 0; var countUnpaid = 0; var countOverpaid = 0
        for (p in participants) {
            val paid = getResidentTotalPaid(activity.id, p.residentId)
            totalCollected += paid.coerceAtLeast(0L)
            totalTarget += p.targetAmount
            when {
                paid <= 0L -> countUnpaid++
                paid > p.targetAmount -> countOverpaid++
                paid >= p.targetAmount -> countPaid++
                else -> countPartial++
            }
        }
        ActivitySummary(activity, totalCollected, totalTarget, countPaid, countPartial, countUnpaid, countOverpaid)
    }.getOrDefault(ActivitySummary(activity, 0L, 0L, 0, 0, 0, 0))

    // ===================== PASSWORD UTILS =====================

    private fun hashPbkdf2Password(password: String, iterations: Int = 120000): Pair<String, String> {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Pair(
            Base64.getEncoder().encodeToString(hash),
            Base64.getEncoder().encodeToString(salt)
        )
    }

    private fun verifyPbkdf2Password(
        password: String,
        storedHash: String,
        storedSalt: String,
        iterations: Int
    ): Boolean {
        return try {
            val salt = Base64.getDecoder().decode(storedSalt)
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val computedHash = factory.generateSecret(spec).encoded
            val expectedHash = Base64.getDecoder().decode(storedHash)
            computedHash.contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }
}
