package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.AdminBotConfig
import com.example.data.model.ReportStatus
import com.example.data.model.UserProfile
import com.example.data.repository.LapkinRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LapkinViewModel(
    private val repository: LapkinRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", root)
                emit(UiState.Error(root.message ?: "Gagal memuat profil ASN"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val adminConfigState: StateFlow<AdminBotConfig> =
        repository.observeAdminBotConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AdminBotConfig()
            )

    val myReportsState: StateFlow<UiState<List<ActivityReport>>> =
        repository.observeMyReports(currentUserId)
            .map<List<ActivityReport>, UiState<List<ActivityReport>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing reports", root)
                emit(UiState.Error(root.message ?: "Gagal memuat daftar laporan"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val reviewQueueState: StateFlow<UiState<List<ActivityReport>>> =
        repository.observeReviewQueue(currentUserId, currentUserEmail)
            .map<List<ActivityReport>, UiState<List<ActivityReport>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing review queue", root)
                emit(UiState.Error(root.message ?: "Gagal memuat antrean verifikasi"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _selectedClassificationFilter = MutableStateFlow<ActivityClassification?>(null)
    val selectedClassificationFilter: StateFlow<ActivityClassification?> = _selectedClassificationFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ReportStatus?>(null)
    val selectedStatusFilter: StateFlow<ReportStatus?> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun setClassificationFilter(classification: ActivityClassification?) {
        _selectedClassificationFilter.value = classification
    }

    fun setStatusFilter(status: ReportStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun saveUserProfile(profile: UserProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveUserProfile(currentUserId, profile)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Profil ASN & Konfigurasi Telegram berhasil disimpan."
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "Gagal menyimpan profil ASN."
                }
            )
        }
    }

    fun saveAdminBotConfig(config: AdminBotConfig, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveAdminBotConfig(config)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Pengaturan Admin Bot Telegram berhasil disimpan ke Cloud."
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.value = e.localizedMessage
                        ?: "Pengaturan lokal tersimpan. Untuk sinkronisasi Cloud Global, pastikan jabatan Anda Administrator/Kepala Dinas."
                }
            )
        }
    }

    fun saveOrSubmitReport(
        report: ActivityReport,
        isSubmit: Boolean,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveOrSubmitReport(currentUserId, report, isSubmit)
            _isBusy.value = false
            result.fold(
                onSuccess = { reportId ->
                    _feedbackMessage.value = if (isSubmit) {
                        "Laporan kegiatan berhasil dikirim berjenjang."
                    } else {
                        "Laporan kegiatan disimpan sebagai draf."
                    }
                    onSuccess(reportId)
                },
                onFailure = { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "Gagal menyimpan laporan."
                }
            )
        }
    }

    fun reviewReport(
        report: ActivityReport,
        reviewerName: String,
        approve: Boolean,
        note: String,
        forwardToNextTier: Boolean = false,
        forceFinalApproveAsAdmin: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.reviewReport(
                report = report,
                reviewerName = reviewerName,
                approve = approve,
                note = note,
                forwardToNextTier = forwardToNextTier,
                forceFinalApproveAsAdmin = forceFinalApproveAsAdmin
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = when {
                        forceFinalApproveAsAdmin && approve ->
                            "Laporan disahkan final oleh Admin / Kepala Dinas."
                        approve && forwardToNextTier ->
                            "Laporan telah disahkan & diteruskan ke jenjang pejabat berikutnya."
                        approve ->
                            "Laporan telah resmi disahkan pada jenjang ini."
                        else ->
                            "Laporan dikembalikan untuk revisi pelapor."
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "Gagal memproses pengesahan laporan."
                }
            )
        }
    }

    fun deleteDraft(reportId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteDraftReport(currentUserId, reportId)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Draf laporan dihapus."
                    onSuccess()
                },
                onFailure = { e ->
                    _feedbackMessage.value = e.localizedMessage ?: "Gagal menghapus draf."
                }
            )
        }
    }

    private companion object {
        const val TAG = "LapkinVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
