package com.example.data

import com.example.domain.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppRepository(
    private val firestore: FirebaseFirestore,
    private val sessionStore: SessionStore
) {

    // ===================== OFFLINE-FIRST HELPERS =====================
    private suspend fun Query.getOfflineFirst(): QuerySnapshot {
        val q = this
        // 1. Selalu coba cache dulu
        val cached = try { q.get(Source.CACHE).await() } catch (e: Exception) { null }
        if (cached != null && !cached.isEmpty) return cached

        // 2. Cache kosong/tidak ada — coba server dengan timeout singkat
        return try {
            withTimeout(3000) { q.get(Source.SERVER).await() }
        } catch (e: Exception) {
            // 3. Offline atau timeout — kembalikan cache (meski kosong) daripada crash
            cached ?: try { q.get(Source.CACHE).await() } catch (e2: Exception) { q.get(Source.DEFAULT).await() }
        }
    }

    private suspend fun DocumentReference.getOfflineFirst(): DocumentSnapshot {
        val docRef = this
        // 1. Coba cache dulu
        val cached = try { docRef.get(Source.CACHE).await() } catch (e: Exception) { null }
        if (cached != null && cached.exists()) return cached

        // 2. Cache tidak ada/dokumen tidak exist — coba server
        return try {
            withTimeout(3000) { docRef.get(Source.SERVER).await() }
        } catch (e: Exception) {
            // 3. Offline — kembalikan cache (meski tidak exist) daripada crash
            cached ?: try { docRef.get(Source.CACHE).await() } catch (e2: Exception) { docRef.get(Source.DEFAULT).await() }
        }
    }

    private suspend fun <T : Any> DocumentReference.setOfflineFirst(data: T) {
        val task = this.set(data)
        try {
            withTimeout(1500) { task.await() }
        } catch (e: Exception) {
            // Data sudah tersimpan lokal di SQLite Firestore & disinkron otomatis saat sinyal tersedia
        }
    }

    private suspend fun DocumentReference.updateOfflineFirst(updates: Map<String, Any>) {
        val task = this.update(updates)
        try {
            withTimeout(1500) { task.await() }
        } catch (e: Exception) {
            // Data tersimpan di SQLite lokal Firestore
        }
    }

    private suspend fun DocumentReference.updateOfflineFirst(field: String, value: Any, vararg moreFields: Any) {
        val task = this.update(field, value, *moreFields)
        try {
            withTimeout(1500) { task.await() }
        } catch (e: Exception) {
            // Abaikan timeout offline
        }
    }

    private suspend fun DocumentReference.deleteOfflineFirst() {
        val task = this.delete()
        try {
            withTimeout(1500) { task.await() }
        } catch (e: Exception) {
            // Abaikan timeout offline
        }
    }

    private suspend fun WriteBatch.commitOfflineFirst() {
        val task = this.commit()
        try {
            withTimeout(1500) { task.await() }
        } catch (e: Exception) {
            // Abaikan timeout offline
        }
    }

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
        val snapshot = firestore.collection("residents").getOfflineFirst()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Resident::class.java)?.copy(id = doc.id)
        }
        .filter { if (activeOnly) it.isActive else true }
        .sortedBy { it.nameNormalized }
    }.getOrDefault(emptyList())

    suspend fun getResident(residentId: String): Resident? = runCatching {
        val doc = firestore.collection("residents").document(residentId).getOfflineFirst()
        if (doc.exists()) doc.toObject(Resident::class.java)!!.copy(id = doc.id) else null
    }.getOrNull()

    suspend fun saveResident(resident: Resident): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val uppercaseName = resident.name.trim().uppercase()
        val normalized = uppercaseName.lowercase()
        val residentId: String
        if (resident.id.isEmpty()) {
            val ref = firestore.collection("residents").document()
            residentId = ref.id
            ref.setOfflineFirst(resident.copy(
                id = residentId,
                name = uppercaseName,
                nameNormalized = normalized,
                createdAt = now,
                updatedAt = now
            ))
        } else {
            residentId = resident.id
            firestore.collection("residents").document(residentId)
                .setOfflineFirst(resident.copy(name = uppercaseName, nameNormalized = normalized, updatedAt = now))
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

    suspend fun deleteResident(residentId: String): Result<Unit> = runCatching {
        firestore.collection("residents").document(residentId).deleteOfflineFirst()
    }

    suspend fun deactivateResident(residentId: String): Result<Unit> = runCatching {
        firestore.collection("residents").document(residentId)
            .updateOfflineFirst("isActive", false, "updatedAt", System.currentTimeMillis())
    }

    // ===================== OFFICERS =====================

    suspend fun getOfficers(): List<Officer> = runCatching {
        firestore.collection("officers").orderBy("name").getOfflineFirst().documents.mapNotNull { doc ->
            doc.toObject(Officer::class.java)?.copy(id = doc.id)
        }
    }.getOrDefault(emptyList())

    suspend fun getOfficer(officerId: String): Officer? = runCatching {
        val doc = firestore.collection("officers").document(officerId).getOfflineFirst()
        if (doc.exists()) doc.toObject(Officer::class.java)!!.copy(id = doc.id) else null
    }.getOrNull()

    suspend fun createOfficer(
        name: String,
        username: String,
        passwordRaw: String,
        phone: String
    ): Result<String> = runCatching {
        val usernameNorm = username.trim().lowercase()
        val existing = firestore.collection("officers")
            .whereEqualTo("usernameNormalized", usernameNorm).limit(1).getOfflineFirst()
        if (!existing.isEmpty) throw Exception("Username '$username' sudah digunakan.")

        val (hash, salt) = hashPbkdf2Password(passwordRaw)
        val now = System.currentTimeMillis()
        val ref = firestore.collection("officers").document()
        ref.setOfflineFirst(Officer(
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
        ))
        ref.id
    }

    suspend fun updateOfficer(officer: Officer): Result<Unit> = runCatching {
        firestore.collection("officers").document(officer.id)
            .setOfflineFirst(officer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun resetOfficerPassword(officerId: String, newPasswordRaw: String): Result<Unit> = runCatching {
        val (hash, salt) = hashPbkdf2Password(newPasswordRaw)
        firestore.collection("officers").document(officerId)
            .updateOfflineFirst(
                "passwordHash", hash,
                "passwordSalt", salt,
                "passwordIterations", 120000,
                "updatedAt", System.currentTimeMillis()
            )
    }

    suspend fun setOfficerActiveStatus(officerId: String, isActive: Boolean): Result<Unit> = runCatching {
        firestore.collection("officers").document(officerId)
            .updateOfflineFirst("isActive", isActive, "updatedAt", System.currentTimeMillis())
    }

    // ===================== ACTIVITIES =====================

    suspend fun getActivities(): List<IuranActivity> = runCatching {
        firestore.collection("activities")
            .orderBy("startAtEpochMs", Query.Direction.DESCENDING)
            .getOfflineFirst().documents.mapNotNull { doc ->
                doc.toObject(IuranActivity::class.java)?.copy(id = doc.id)
            }
    }.getOrDefault(emptyList())

    suspend fun getActivityById(activityId: String): IuranActivity? = runCatching {
        val doc = firestore.collection("activities").document(activityId).getOfflineFirst()
        if (doc.exists()) doc.toObject(IuranActivity::class.java)!!.copy(id = doc.id) else null
    }.getOrNull()

    suspend fun getActiveActivitiesForOfficer(officerId: String): List<IuranActivity> = runCatching {
        val snapshot = firestore.collection("activities")
            .whereEqualTo("status", ActivityStatus.ACTIVE.name)
            .getOfflineFirst()
        val fromEnum = snapshot.documents.mapNotNull { doc ->
            doc.toObject(IuranActivity::class.java)?.copy(id = doc.id)
        }
        val activities = fromEnum.ifEmpty {
            firestore.collection("activities")
                .getOfflineFirst().documents.mapNotNull { doc ->
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
            ref.setOfflineFirst(activity.copy(id = activityId, createdAt = now, updatedAt = now))
        } else {
            activityId = activity.id
            firestore.collection("activities").document(activityId)
                .setOfflineFirst(activity.copy(updatedAt = now))
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
            .getOfflineFirst()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(ActivityParticipant::class.java)?.copy(id = doc.id)
        }.filter { it.isIncluded }
    }.getOrDefault(emptyList())

    /** Pastikan seluruh warga aktif terdaftar di kegiatan — panggil sekali saat membuka kegiatan */
    suspend fun ensureAllResidentsEnrolled(activityId: String) = runCatching {
        val activity = getActivityById(activityId) ?: return@runCatching
        val existingIds = getParticipants(activityId).map { it.residentId }.toSet()
        val residents = getResidents(activeOnly = true)
        val missingIds = residents.map { it.id }.filter { it !in existingIds }
        if (missingIds.isNotEmpty()) {
            upsertParticipants(activityId, missingIds, activity.defaultTargetAmount)
        }
    }

    suspend fun saveParticipant(participant: ActivityParticipant): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        if (participant.id.isEmpty()) {
            val ref = firestore.collection("activity_participants").document()
            ref.setOfflineFirst(participant.copy(id = ref.id, createdAt = now, updatedAt = now))
            ref.id
        } else {
            firestore.collection("activity_participants").document(participant.id)
                .setOfflineFirst(participant.copy(updatedAt = now))
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
        batch.commitOfflineFirst()
    }

    // ===================== TRANSACTIONS =====================

    suspend fun getTransactions(activityId: String, residentId: String): List<PaymentTransaction> = runCatching {
        firestore.collection("transactions")
            .whereEqualTo("activityId", activityId)
            .getOfflineFirst().documents.mapNotNull { doc ->
                doc.toObject(PaymentTransaction::class.java)
            }.filter { it.residentId == residentId }
            .sortedByDescending { it.paidAtDeviceEpochMs }
    }.getOrDefault(emptyList())

    suspend fun getAllTransactions(activityId: String? = null): List<PaymentTransaction> = runCatching {
        val list = if (activityId != null) {
            firestore.collection("transactions")
                .whereEqualTo("activityId", activityId)
                .getOfflineFirst().documents.mapNotNull { doc ->
                    doc.toObject(PaymentTransaction::class.java)
                }
        } else {
            firestore.collection("transactions")
                .limit(200)
                .getOfflineFirst().documents.mapNotNull { doc ->
                    doc.toObject(PaymentTransaction::class.java)
                }
        }
        list.sortedByDescending { it.paidAtDeviceEpochMs }
    }.getOrDefault(emptyList())

    suspend fun getMyTransactions(officerId: String): List<PaymentTransaction> = runCatching {
        firestore.collection("transactions")
            .whereEqualTo("officerId", officerId)
            .getOfflineFirst().documents.mapNotNull { doc ->
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
            .setOfflineFirst(transaction)

        transactionId
    }

    suspend fun editTransaction(
        transactionId: String,
        newAmount: Long,
        newMethod: PaymentMethod,
        newNote: String
    ): Result<Unit> = runCatching {
        firestore.collection("transactions").document(transactionId)
            .updateOfflineFirst(
                "amount", newAmount,
                "paymentMethod", newMethod,
                "note", newNote
            )
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
            .setOfflineFirst(reversal)

        reversalId
    }

    /** Sinkronisasi manual dari server (khusus dipanggil saat online / tombol sinkronisasi) */
    suspend fun syncFromServer(): Result<String> = runCatching {
        var count = 0
        val acts = firestore.collection("activities").get(Source.SERVER).await()
        count += acts.size()
        val res = firestore.collection("residents").get(Source.SERVER).await()
        count += res.size()
        val txs = firestore.collection("transactions").limit(200).get(Source.SERVER).await()
        count += txs.size()
        "Sinkronisasi berhasil memuat $count dokumen dari server."
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

    suspend fun deleteActivity(activityId: String): Result<Unit> = runCatching {
        firestore.collection("activities").document(activityId).delete().await()
    }

    fun getResidentsFlow(activeOnly: Boolean = false): Flow<List<Resident>> = flow { emit(getResidents(activeOnly)) }
    fun getOfficersFlow(): Flow<List<Officer>> = flow { emit(getOfficers()) }
    fun getActivitiesFlow(): Flow<List<IuranActivity>> = flow { emit(getActivities()) }
    fun getParticipantsFlow(activityId: String): Flow<List<ActivityParticipant>> = flow { emit(getParticipants(activityId)) }
    fun getTransactionsFlow(activityId: String, residentId: String): Flow<List<PaymentTransaction>> = flow { emit(getTransactions(activityId, residentId)) }
    fun getAllTransactionsFlow(activityId: String? = null): Flow<List<PaymentTransaction>> = flow { emit(getAllTransactions(activityId)) }
    fun getMyTransactionsFlow(officerId: String): Flow<List<PaymentTransaction>> = flow { emit(getMyTransactions(officerId)) }

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
