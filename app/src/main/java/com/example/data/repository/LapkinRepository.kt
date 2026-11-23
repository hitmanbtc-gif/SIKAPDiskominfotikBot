package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ActivityReport
import com.example.data.model.AdminBotConfig
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.ReportStatus
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

class LapkinRepository(
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
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeAdminBotConfig(): Flow<AdminBotConfig> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(AdminBotConfig())
            return@flow
        }
        val path = "admin_config/global"
        emitAll(
            db.collection("admin_config").document("global")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            AdminBotConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: AdminBotConfig()
                    } else {
                        AdminBotConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    emit(AdminBotConfig())
                }
        )
    }

    suspend fun saveAdminBotConfig(config: AdminBotConfig): Result<Unit> {
        val docPath = "admin_config/global"
        return try {
            requireUserId()
            val cleanKadisName = config.kadisFullName.trim()
                .ifBlank { DiskominfotikCatalog.DEFAULT_KADIS_NAME }
                .take(100)
            require(cleanKadisName.length >= 2) { "Nama Kepala Dinas minimal 2 karakter." }

            val cleanBotUsername = config.botUsername.trim()
                .ifBlank { DiskominfotikCatalog.DEFAULT_BOT_USERNAME }
                .let { if (it.startsWith("@")) it else "@$it" }
                .take(100)

            val payload = mapOf(
                "botUsername" to cleanBotUsername,
                "defaultGroupChatId" to config.defaultGroupChatId.trim().take(60),
                "pengawasChatId" to config.pengawasChatId.trim().take(60),
                "adminChatId" to config.adminChatId.trim().take(60),
                "kadisChatId" to config.kadisChatId.trim().take(60),
                "kadisFullName" to cleanKadisName,
                "kadisNip" to config.kadisNip.trim().take(30),
                "broadcastAnnouncement" to config.broadcastAnnouncement.trim().take(500),
                "requireGpsLocation" to config.requireGpsLocation,
                "requirePhotoEvidence" to config.requirePhotoEvidence,
                "autoSendPdfToTelegram" to config.autoSendPdfToTelegram,
                "autoSendGpsPin" to config.autoSendGpsPin,
                "updatedByEmail" to (auth.currentUser?.email ?: config.updatedByEmail).trim().lowercase().take(120),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("admin_config").document("global").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(userId: String, profile: UserProfile): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna berubah; penyimpanan dibatalkan." }
            val cleanName = profile.fullName.trim()
            require(cleanName.length in 2..100) { "Nama lengkap harus antara 2 hingga 100 karakter." }
            val cleanBidang = profile.unitBidang.trim()
            require(cleanBidang.length in 2..120) { "Unit Bidang/Sekretariat wajib dipilih." }

            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()

            if (!existingSnap.exists()) {
                val createMap = mapOf(
                    "userId" to uid,
                    "fullName" to cleanName,
                    "nip" to profile.nip.trim().take(30),
                    "jabatanLevel" to profile.levelEnum().name,
                    "jabatanTitle" to profile.jabatanTitle.trim().take(100),
                    "unitBidang" to cleanBidang.take(120),
                    "email" to (auth.currentUser?.email ?: profile.email).trim().lowercase().take(120),
                    "pengawasEmail" to profile.pengawasEmail.trim().lowercase().take(120),
                    "adminEmail" to profile.adminEmail.trim().lowercase().take(120),
                    "kadisEmail" to profile.kadisEmail.trim().lowercase().take(120),
                    "myPhone" to profile.myPhone.trim().take(60),
                    "pengawasPhone" to profile.pengawasPhone.trim().take(60),
                    "adminPhone" to profile.adminPhone.trim().take(60),
                    "kadisPhone" to profile.kadisPhone.trim().take(60),
                    "profilePhotoBase64" to profile.profilePhotoBase64.take(350_000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createMap).await()
            } else {
                val updateMap = mapOf(
                    "fullName" to cleanName,
                    "nip" to profile.nip.trim().take(30),
                    "jabatanLevel" to profile.levelEnum().name,
                    "jabatanTitle" to profile.jabatanTitle.trim().take(100),
                    "unitBidang" to cleanBidang.take(120),
                    "email" to (auth.currentUser?.email ?: profile.email).trim().lowercase().take(120),
                    "pengawasEmail" to profile.pengawasEmail.trim().lowercase().take(120),
                    "adminEmail" to profile.adminEmail.trim().lowercase().take(120),
                    "kadisEmail" to profile.kadisEmail.trim().lowercase().take(120),
                    "myPhone" to profile.myPhone.trim().take(60),
                    "pengawasPhone" to profile.pengawasPhone.trim().take(60),
                    "adminPhone" to profile.adminPhone.trim().take(60),
                    "kadisPhone" to profile.kadisPhone.trim().take(60),
                    "profilePhotoBase64" to profile.profilePhotoBase64.take(350_000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeMyReports(userId: String): Flow<List<ActivityReport>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "activity_reports"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActivityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<ActivityReport> { it.activityDate }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeReviewQueue(userId: String, userEmail: String): Flow<List<ActivityReport>> = flow {
        val uid = auth.currentUser?.uid
        val normalizedEmail = userEmail.trim().lowercase()
        if (uid.isNullOrBlank() || uid != userId || normalizedEmail.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "activity_reports"
        val isBootstrappedAdmin =
            normalizedEmail == DiskominfotikCatalog.ADMIN_EMAIL && auth.currentUser?.isEmailVerified == true
        val query = if (isBootstrappedAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereArrayContains("reviewerEmails", normalizedEmail)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActivityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<ActivityReport> { it.activityDate }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveOrSubmitReport(
        userId: String,
        report: ActivityReport,
        isSubmit: Boolean
    ): Result<String> {
        var docPath = "activity_reports/${report.id.ifBlank { "new" }}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna tidak cocok." }

            val cleanSummary = report.summary.trim()
            require(cleanSummary.length in 3..300) { "Ringkasan kegiatan minimal 3 karakter (maks 300)." }
            val cleanBidang = report.unitBidang.trim()
            require(cleanBidang.length in 2..120) { "Bidang / Sekretariat wajib dipilih." }
            val cleanSubKegiatan = report.subKegiatan.trim()
            require(cleanSubKegiatan.length in 2..250) { "Sub Kegiatan wajib dipilih atau diisi." }
            val cleanLocation = report.locationName.trim()
            require(cleanLocation.length in 2..200) { "Lokasi / tempat kegiatan minimal 2 karakter." }
            require(report.activityDate.length in 8..20) { "Tanggal kegiatan tidak valid." }
            require(report.startTime.length in 4..10 && report.endTime.length in 4..10) {
                "Waktu mulai dan selesai wajib diisi."
            }
            require(report.photoBase64.length <= 350_000) {
                "Ukuran foto bukti terlalu besar; pilih foto lain atau kompres ulang."
            }

            val authorTier = JabatanLevel.fromRaw(report.authorTier)
            val (targetStatus, targetTier) = if (!isSubmit) {
                ReportStatus.DRAFT.name to authorTier.name
            } else {
                when (authorTier) {
                    JabatanLevel.PELAKSANA ->
                        ReportStatus.SUBMITTED_PENGAWAS.name to JabatanLevel.PENGAWAS_FUNGSIONAL.name
                    JabatanLevel.PENGAWAS_FUNGSIONAL ->
                        ReportStatus.SUBMITTED_ADMINISTRATOR.name to JabatanLevel.ADMINISTRATOR.name
                    JabatanLevel.ADMINISTRATOR ->
                        ReportStatus.SUBMITTED_KADIS.name to JabatanLevel.KEPALA_DINAS.name
                    JabatanLevel.KEPALA_DINAS ->
                        ReportStatus.SUBMITTED_KADIS.name to JabatanLevel.KEPALA_DINAS.name
                }
            }

            val normalizedEmail = (auth.currentUser?.email ?: report.authorEmail).trim().lowercase()
            val pEmail = report.pengawasEmail.trim().lowercase().take(120)
            val aEmail = report.adminEmail.trim().lowercase().take(120)
            val kEmail = report.kadisEmail.trim().lowercase().take(120)
            val reviewers = listOf(pEmail, aEmail, kEmail, normalizedEmail, DiskominfotikCatalog.ADMIN_EMAIL)
                .filter { it.isNotBlank() }
                .distinct()
                .take(5)

            val isCreate = report.id.isBlank()
            val docRef = if (isCreate) {
                db.collection("activity_reports").document()
            } else {
                db.collection("activity_reports").document(report.id)
            }
            docPath = docRef.path

            if (isCreate) {
                val payload = mapOf(
                    "userId" to uid,
                    "authorName" to report.authorName.trim().ifBlank { "ASN Diskominfotik" }.take(100),
                    "authorNip" to report.authorNip.trim().take(30),
                    "authorJabatan" to report.authorJabatan.trim().take(100),
                    "authorTier" to authorTier.name,
                    "authorEmail" to normalizedEmail.take(120),
                    "activityDate" to report.activityDate.trim(),
                    "startTime" to report.startTime.trim(),
                    "endTime" to report.endTime.trim(),
                    "summary" to cleanSummary,
                    "description" to report.description.trim().take(2000),
                    "classification" to report.classificationEnum().name,
                    "unitBidang" to cleanBidang,
                    "subKegiatan" to cleanSubKegiatan,
                    "locationName" to cleanLocation,
                    "gpsLatitude" to report.gpsLatitude.coerceIn(-90.0, 90.0),
                    "gpsLongitude" to report.gpsLongitude.coerceIn(-180.0, 180.0),
                    "gpsAccuracyMeters" to report.gpsAccuracyMeters.coerceIn(0.0, 100000.0),
                    "gpsAddress" to report.gpsAddress.trim().take(250),
                    "gpsCaptured" to report.gpsCaptured,
                    "photoBase64" to report.photoBase64,
                    "photoCaption" to report.photoCaption.trim().take(200),
                    "status" to targetStatus,
                    "currentTier" to targetTier,
                    "reviewerEmails" to reviewers,
                    "pengawasEmail" to pEmail,
                    "adminEmail" to aEmail,
                    "kadisEmail" to kEmail,
                    "pengawasReviewerName" to "",
                    "pengawasNote" to "",
                    "pengawasReviewedAtMillis" to 0L,
                    "adminReviewerName" to "",
                    "adminNote" to "",
                    "adminReviewedAtMillis" to 0L,
                    "kadisReviewerName" to "",
                    "kadisNote" to "",
                    "kadisReviewedAtMillis" to 0L,
                    "lastRevisionNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "authorName" to report.authorName.trim().ifBlank { "ASN Diskominfotik" }.take(100),
                    "authorNip" to report.authorNip.trim().take(30),
                    "authorJabatan" to report.authorJabatan.trim().take(100),
                    "authorTier" to authorTier.name,
                    "authorEmail" to normalizedEmail.take(120),
                    "activityDate" to report.activityDate.trim(),
                    "startTime" to report.startTime.trim(),
                    "endTime" to report.endTime.trim(),
                    "summary" to cleanSummary,
                    "description" to report.description.trim().take(2000),
                    "classification" to report.classificationEnum().name,
                    "unitBidang" to cleanBidang,
                    "subKegiatan" to cleanSubKegiatan,
                    "locationName" to cleanLocation,
                    "gpsLatitude" to report.gpsLatitude.coerceIn(-90.0, 90.0),
                    "gpsLongitude" to report.gpsLongitude.coerceIn(-180.0, 180.0),
                    "gpsAccuracyMeters" to report.gpsAccuracyMeters.coerceIn(0.0, 100000.0),
                    "gpsAddress" to report.gpsAddress.trim().take(250),
                    "gpsCaptured" to report.gpsCaptured,
                    "photoBase64" to report.photoBase64,
                    "photoCaption" to report.photoCaption.trim().take(200),
                    "status" to targetStatus,
                    "currentTier" to targetTier,
                    "reviewerEmails" to reviewers,
                    "pengawasEmail" to pEmail,
                    "adminEmail" to aEmail,
                    "kadisEmail" to kEmail,
                    "lastRevisionNote" to if (isSubmit) "" else report.lastRevisionNote.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (report.id.isBlank()) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun reviewReport(
        report: ActivityReport,
        reviewerName: String,
        approve: Boolean,
        note: String,
        forwardToNextTier: Boolean = false,
        forceFinalApproveAsAdmin: Boolean = false
    ): Result<Unit> {
        val docPath = "activity_reports/${report.id}"
        return try {
            requireUserId()
            require(report.id.isNotBlank()) { "ID laporan tidak valid." }
            val currentStatus = report.statusEnum()
            val canSelectiveForward = approve && forwardToNextTier &&
                (report.canPengawasForwardToAdmin() || report.canAdminForwardToKadis())
            require(
                forceFinalApproveAsAdmin ||
                    canSelectiveForward ||
                    currentStatus == ReportStatus.SUBMITTED_PENGAWAS ||
                    currentStatus == ReportStatus.SUBMITTED_ADMINISTRATOR ||
                    currentStatus == ReportStatus.SUBMITTED_KADIS
            ) {
                "Laporan tidak berada dalam antrean pengesahan atau penerusan aktif."
            }

            val cleanNote = note.trim().take(500)
            val cleanReviewer = reviewerName.trim().ifBlank {
                auth.currentUser?.displayName ?: "Pejabat Pengesah"
            }.take(100)
            val nowMillis = System.currentTimeMillis()

            val updateFields = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!approve) {
                require(cleanNote.isNotBlank()) { "Catatan revisi wajib diisi saat mengembalikan laporan." }
                updateFields["status"] = ReportStatus.REVISION.name
                updateFields["currentTier"] = JabatanLevel.PELAKSANA.name
                updateFields["lastRevisionNote"] = cleanNote
                when (currentStatus) {
                    ReportStatus.SUBMITTED_PENGAWAS -> {
                        updateFields["pengawasReviewerName"] = cleanReviewer
                        updateFields["pengawasNote"] = cleanNote
                        updateFields["pengawasReviewedAtMillis"] = nowMillis
                    }
                    ReportStatus.SUBMITTED_ADMINISTRATOR -> {
                        updateFields["adminReviewerName"] = cleanReviewer
                        updateFields["adminNote"] = cleanNote
                        updateFields["adminReviewedAtMillis"] = nowMillis
                    }
                    else -> {
                        updateFields["kadisReviewerName"] = cleanReviewer
                        updateFields["kadisNote"] = cleanNote
                        updateFields["kadisReviewedAtMillis"] = nowMillis
                    }
                }
            } else if (forceFinalApproveAsAdmin) {
                updateFields["status"] = ReportStatus.APPROVED_FINAL.name
                updateFields["currentTier"] = "COMPLETED"
                updateFields["kadisReviewerName"] = cleanReviewer
                updateFields["kadisNote"] = cleanNote.ifBlank { "Disahkan langsung melalui Panel Admin Bot Telegram" }
                updateFields["kadisReviewedAtMillis"] = nowMillis
            } else if (report.canPengawasForwardToAdmin() && forwardToNextTier) {
                // Selective forwarding of an already Pengawas-approved report to Pejabat Administrator
                updateFields["status"] = ReportStatus.SUBMITTED_ADMINISTRATOR.name
                updateFields["currentTier"] = JabatanLevel.ADMINISTRATOR.name
                updateFields["pengawasReviewerName"] = cleanReviewer
                updateFields["pengawasNote"] = cleanNote.ifBlank {
                    "Disahkan Pengawas/Fungsional & Dipilih untuk Diteruskan ke Pejabat Administrator"
                }
                updateFields["pengawasReviewedAtMillis"] = nowMillis
            } else if (report.canAdminForwardToKadis() && forwardToNextTier) {
                // Selective forwarding of an already Administrator-approved report to Kepala Dinas
                updateFields["status"] = ReportStatus.SUBMITTED_KADIS.name
                updateFields["currentTier"] = JabatanLevel.KEPALA_DINAS.name
                updateFields["adminReviewerName"] = cleanReviewer
                updateFields["adminNote"] = cleanNote.ifBlank {
                    "Disahkan Pejabat Administrator & Dipilih untuk Diteruskan ke Kepala Dinas"
                }
                updateFields["adminReviewedAtMillis"] = nowMillis
            } else {
                when (currentStatus) {
                    ReportStatus.SUBMITTED_PENGAWAS -> {
                        if (forwardToNextTier) {
                            updateFields["status"] = ReportStatus.SUBMITTED_ADMINISTRATOR.name
                            updateFields["currentTier"] = JabatanLevel.ADMINISTRATOR.name
                            updateFields["pengawasReviewerName"] = cleanReviewer
                            updateFields["pengawasNote"] = cleanNote.ifBlank {
                                "Disahkan Pejabat Pengawas/Fungsional & Diteruskan ke Pejabat Administrator"
                            }
                            updateFields["pengawasReviewedAtMillis"] = nowMillis
                        } else {
                            // Direct approval by Pejabat Pengawas / Fungsional for PPPK / Pelaksana reports
                            updateFields["status"] = ReportStatus.APPROVED_FINAL.name
                            updateFields["currentTier"] = JabatanLevel.PENGAWAS_FUNGSIONAL.name
                            updateFields["pengawasReviewerName"] = cleanReviewer
                            updateFields["pengawasNote"] = cleanNote.ifBlank {
                                "Disahkan oleh Pejabat Pengawas / Fungsional"
                            }
                            updateFields["pengawasReviewedAtMillis"] = nowMillis
                        }
                    }
                    ReportStatus.SUBMITTED_ADMINISTRATOR -> {
                        if (forwardToNextTier) {
                            updateFields["status"] = ReportStatus.SUBMITTED_KADIS.name
                            updateFields["currentTier"] = JabatanLevel.KEPALA_DINAS.name
                            updateFields["adminReviewerName"] = cleanReviewer
                            updateFields["adminNote"] = cleanNote.ifBlank {
                                "Disahkan Pejabat Administrator & Diteruskan ke Kepala Dinas"
                            }
                            updateFields["adminReviewedAtMillis"] = nowMillis
                        } else {
                            // Direct approval by Pejabat Administrator for Pengawas/Fungsional reports
                            updateFields["status"] = ReportStatus.APPROVED_FINAL.name
                            updateFields["currentTier"] = JabatanLevel.ADMINISTRATOR.name
                            updateFields["adminReviewerName"] = cleanReviewer
                            updateFields["adminNote"] = cleanNote.ifBlank {
                                "Disahkan oleh Pejabat Administrator"
                            }
                            updateFields["adminReviewedAtMillis"] = nowMillis
                        }
                    }
                    ReportStatus.SUBMITTED_KADIS -> {
                        updateFields["status"] = ReportStatus.APPROVED_FINAL.name
                        updateFields["currentTier"] = "COMPLETED"
                        updateFields["kadisReviewerName"] = cleanReviewer
                        updateFields["kadisNote"] = cleanNote.ifBlank { "Disahkan oleh Kepala Dinas" }
                        updateFields["kadisReviewedAtMillis"] = nowMillis
                    }
                    else -> {}
                }
            }

            db.collection("activity_reports").document(report.id).update(updateFields).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDraftReport(userId: String, reportId: String): Result<Unit> {
        val docPath = "activity_reports/$reportId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Sesi pengguna tidak cocok." }
            require(reportId.isNotBlank()) { "ID laporan kosong." }
            db.collection("activity_reports").document(reportId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
