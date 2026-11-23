package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.AdminBotConfig
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.ReportStatus
import com.example.data.model.UserProfile
import com.example.ui.components.Base64ImagePreview
import com.example.ui.viewmodel.UiState
import com.example.util.ImageHelper
import com.example.util.LocationHelper
import com.example.util.PdfReportGenerator
import com.example.util.TelegramBotService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TgHeaderBlue = Color(0xFF155A96)
private val TgAccentBlue = Color(0xFF0B3D70)
private val TgWallpaperBlue = Color(0xFFD6E6F2)
private val TgOutgoingBubble = Color(0xFFEFFDDE)
private val TgInlineButtonBg = Color(0xFF0B3D70)
private val TgInlineButtonBorder = Color(0xFF1E6BB8)

enum class TgBotStep {
    IDLE,
    ASK_CLASSIFICATION,
    ASK_BIDANG,
    ASK_SUB_KEGIATAN,
    ASK_SUMMARY,
    ASK_DESCRIPTION,
    ASK_LOCATION,
    ASK_GPS,
    ASK_PHOTO,
    READY_CONFIRM
}

data class TgInlineButton(
    val code: String,
    val label: String
)

data class TgChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val timestamp: String,
    val photoBase64: String = "",
    val gpsSummary: String = "",
    val inlineButtons: List<TgInlineButton> = emptyList(),
    val reportAttachment: ActivityReport? = null
)

@Composable
fun TelegramBotScreen(
    profileState: UiState<UserProfile?>,
    reportsState: UiState<List<ActivityReport>>,
    reviewQueueState: UiState<List<ActivityReport>>,
    adminConfig: AdminBotConfig,
    currentUserEmail: String,
    currentUserDisplayName: String,
    isBusy: Boolean,
    onSaveOrSubmitReport: (ActivityReport, Boolean, (String) -> Unit) -> Unit,
    onReviewDecision: (ActivityReport, Boolean, String) -> Unit,
    onExportPdf: (List<ActivityReport>, String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenProfileSetup: () -> Unit,
    onOpenAdminSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile = (profileState as? UiState.Success)?.data
    val myReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val queueReports = (reviewQueueState as? UiState.Success)?.data.orEmpty()
    val pendingQueue = remember(queueReports) {
        queueReports.filter {
            val st = it.statusEnum()
            st == ReportStatus.SUBMITTED_PENGAWAS ||
                st == ReportStatus.SUBMITTED_ADMINISTRATOR ||
                st == ReportStatus.SUBMITTED_KADIS
        }
    }

    val botApiConfigured = remember(context, adminConfig) {
        TelegramBotService.isBotApiConfigured(context)
    }
    val tgAppInstalled = remember(context) {
        TelegramBotService.isTelegramAppInstalled(context)
    }
    val activeBotHandle = adminConfig.botUsername.ifBlank {
        TelegramBotService.getSavedLocalBotUsername(context)
            .ifBlank { DiskominfotikCatalog.DEFAULT_BOT_USERNAME }
    }

    val messages = remember { mutableStateListOf<TgChatMessage>() }
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(TgBotStep.IDLE) }
    var msgCounter by remember { mutableIntStateOf(1) }

    // Draft report state inside conversational wizard
    var draftClassification by remember { mutableStateOf(ActivityClassification.RUTIN) }
    var draftBidang by remember {
        mutableStateOf(
            profile?.unitBidang?.ifBlank { DiskominfotikCatalog.units.first().name }
                ?: DiskominfotikCatalog.units.first().name
        )
    }
    var draftSubKegiatan by remember {
        mutableStateOf(DiskominfotikCatalog.subKegiatansFor(draftBidang).first())
    }
    var draftSummary by remember { mutableStateOf("") }
    var draftDescription by remember { mutableStateOf("") }
    var draftLocation by remember { mutableStateOf("") }
    var draftGpsLat by remember { mutableDoubleStateOf(0.0) }
    var draftGpsLng by remember { mutableDoubleStateOf(0.0) }
    var draftGpsAcc by remember { mutableDoubleStateOf(0.0) }
    var draftGpsAddr by remember { mutableStateOf("") }
    var draftGpsCaptured by remember { mutableStateOf(false) }
    var draftPhotoBase64 by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturingGps by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun addBotMessage(
        text: String,
        buttons: List<TgInlineButton> = emptyList(),
        photoBase64: String = "",
        gpsSummary: String = "",
        reportAttachment: ActivityReport? = null
    ) {
        messages.add(
            TgChatMessage(
                id = "tg_bot_${msgCounter++}_${System.currentTimeMillis()}",
                fromUser = false,
                text = text,
                timestamp = nowTime(),
                photoBase64 = photoBase64,
                gpsSummary = gpsSummary,
                inlineButtons = buttons,
                reportAttachment = reportAttachment
            )
        )
    }

    fun addUserMessage(
        text: String,
        photoBase64: String = "",
        gpsSummary: String = ""
    ) {
        messages.add(
            TgChatMessage(
                id = "tg_usr_${msgCounter++}_${System.currentTimeMillis()}",
                fromUser = true,
                text = text,
                timestamp = nowTime(),
                photoBase64 = photoBase64,
                gpsSummary = gpsSummary
            )
        )
    }

    fun mainMenuButtons(): List<TgInlineButton> = listOf(
        TgInlineButton("/lapor", "➕ /lapor — Catat Kegiatan Baru"),
        TgInlineButton("/tg_pdf", "📄 /tg_pdf — Kirim Dokumen PDF ke Telegram"),
        TgInlineButton("/tg", "✈️ /tg — Kirim Laporan & Pin GPS ke Telegram"),
        TgInlineButton("/verifikasi", "✅ /verifikasi — Antrean Berjenjang (${pendingQueue.size})"),
        TgInlineButton("/rekap", "📊 /rekap — Statistik & Klasifikasi"),
        TgInlineButton("/admin", "🛡️ /admin — Pengaturan Admin Bot Telegram")
    )

    fun showStartMenu() {
        val greetingName = profile?.fullName?.ifBlank { currentUserDisplayName }
            ?: currentUserDisplayName.ifBlank { "ASN Diskominfotik" }
        val tierLabel = profile?.levelEnum()?.label ?: JabatanLevel.PELAKSANA.label
        val modeInfo = when {
            botApiConfigured -> "🟢 Mode Telegram Bot API Aktif ($activeBotHandle • sendMessage, sendLocation & sendDocument PDF Otomatis)"
            tgAppInstalled -> "🟢 Mode Aplikasi Telegram Perangkat Aktif (Terhubung langsung ke Telegram di ponsel Anda)"
            else -> "🟢 Mode Telegram Web/Deep-Link t.me Aktif"
        }
        val configuredTargets = listOfNotNull(
            (profile?.pengawasPhone?.ifBlank { adminConfig.pengawasChatId })?.takeIf { it.isNotBlank() }?.let { "Pengawas: $it" },
            (profile?.adminPhone?.ifBlank { adminConfig.adminChatId })?.takeIf { it.isNotBlank() }?.let { "Admin: $it" },
            (profile?.kadisPhone?.ifBlank { adminConfig.kadisChatId })?.takeIf { it.isNotBlank() }?.let { "Kadis: $it" },
            adminConfig.defaultGroupChatId.takeIf { it.isNotBlank() }?.let { "Grup Dinas: $it" }
        ).joinToString(" | ").ifBlank { "Belum diatur (ketuk 🛡️ /admin atau Profil)" }

        val policyBadges = buildList {
            if (adminConfig.requireGpsLocation) add("Wajib GPS")
            if (adminConfig.requirePhotoEvidence) add("Wajib Foto")
            if (adminConfig.autoSendPdfToTelegram) add("Auto-PDF Telegram")
            if (adminConfig.autoSendGpsPin) add("Auto-Pin GPS")
        }.joinToString(" • ").ifBlank { "Standar Berjenjang" }

        addBotMessage(
            text = "✈️ Halo, $greetingName ($tierLabel)!\n\n" +
                "Selamat datang di Bot Telegram Resmi $activeBotHandle — Sistem Pelaporan Pelaksanaan Kegiatan & Aktivitas Harian ASN Dinas Kominfo dan Statistik Provinsi Gorontalo.\n\n" +
                "• Status: $modeInfo\n" +
                "• Target Chat ID / Username: $configuredTargets\n" +
                "• Kebijakan Admin: $policyBadges\n" +
                "• Pejabat Pengesah PDF: ${adminConfig.kadisFullName}\n\n" +
                "Gunakan tombol Inline Keyboard di bawah atau ketik perintah /lapor, /tg_pdf, /verifikasi, /admin:",
            buttons = mainMenuButtons()
        )
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            showStartMenu()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendReportPdfToRealTelegram(reportsToExport: List<ActivityReport>, targetReport: ActivityReport?) {
        if (reportsToExport.isEmpty()) {
            addBotMessage(
                text = "⚠️ Belum ada laporan kegiatan untuk dikirim ke Telegram. Buat laporan terlebih dahulu dengan /lapor.",
                buttons = listOf(TgInlineButton("/lapor", "➕ /lapor — Catat Kegiatan"))
            )
            return
        }
        scope.launch {
            val refReport = targetReport ?: reportsToExport.first()
            PdfReportGenerator.generatePdf(
                context = context,
                profile = profile,
                reports = reportsToExport,
                periodLabel = "Laporan Resmi Bot Telegram e-Lapkin",
                adminConfig = adminConfig
            ).fold(
                onSuccess = { pdfFile ->
                    val res = TelegramBotService.dispatchPdfToTelegram(
                        context = context,
                        pdfFile = pdfFile,
                        report = refReport,
                        profile = profile,
                        adminConfig = adminConfig
                    )
                    addBotMessage(
                        text = "📄 Status Pengiriman PDF Telegram:\n${res.message}",
                        buttons = mainMenuButtons()
                    )
                },
                onFailure = { err ->
                    addBotMessage("⚠️ Gagal menyiapkan PDF untuk Telegram: ${err.localizedMessage}")
                }
            )
        }
    }

    fun promptConfirmStep() {
        if (adminConfig.requireGpsLocation && !draftGpsCaptured) {
            currentStep = TgBotStep.ASK_GPS
            addBotMessage(
                text = "🔒 Kebijakan Admin mewajibkan titik koordinat GPS pada setiap laporan kegiatan. Silakan ketuk tombol Bagikan Lokasi GPS di bawah:",
                buttons = listOf(TgInlineButton("BAGIKAN_GPS", "📍 Ambil & Kunci Lokasi GPS Sekarang"))
            )
            return
        }
        if (adminConfig.requirePhotoEvidence && draftPhotoBase64.isBlank()) {
            currentStep = TgBotStep.ASK_PHOTO
            addBotMessage(
                text = "🔒 Kebijakan Admin mewajibkan unggah Bukti Foto Kegiatan sebelum laporan dapat dikirim. Pilih Kamera atau Galeri di bawah:",
                buttons = listOf(
                    TgInlineButton("AMBIL_KAMERA", "📷 Ambil Foto Kamera"),
                    TgInlineButton("PILIH_GALERI", "🖼️ Pilih Foto Galeri")
                )
            )
            return
        }

        currentStep = TgBotStep.READY_CONFIRM
        val gpsInfo = if (draftGpsCaptured) {
            String.format(Locale.US, "%.5f, %.5f (±%.0fm) %s", draftGpsLat, draftGpsLng, draftGpsAcc, draftGpsAddr)
        } else {
            "Tidak dilampirkan"
        }
        val photoInfo = if (draftPhotoBase64.isNotBlank()) "Terlampir (JPEG Terkompresi)" else "Tidak ada foto"
        val authorTier = profile?.levelEnum() ?: JabatanLevel.PELAKSANA
        val (targetRole, targetChat) = when (authorTier) {
            JabatanLevel.PELAKSANA ->
                "Pejabat Pengawas / Fungsional" to (profile?.pengawasPhone?.ifBlank { adminConfig.pengawasChatId }.orEmpty())
            JabatanLevel.PENGAWAS_FUNGSIONAL ->
                "Pejabat Administrator" to (profile?.adminPhone?.ifBlank { adminConfig.adminChatId }.orEmpty())
            JabatanLevel.ADMINISTRATOR, JabatanLevel.KEPALA_DINAS ->
                "Kepala Dinas" to (profile?.kadisPhone?.ifBlank { adminConfig.kadisChatId }.orEmpty())
        }
        val chatDisplay = targetChat.ifBlank { adminConfig.defaultGroupChatId }.ifBlank { "Pilih Chat di Aplikasi Telegram" }

        addBotMessage(
            text = "📋 KONFIRMASI PENGIRIMAN BOT TELEGRAM\n" +
                "────────────────────\n" +
                "• Tanggal: ${todayIso()} (08:00 - 16:00 WITA)\n" +
                "• Klasifikasi: ${draftClassification.label}\n" +
                "• Bidang/Sekretariat: $draftBidang\n" +
                "• Sub Kegiatan: $draftSubKegiatan\n" +
                "• Ringkasan: $draftSummary\n" +
                "• Uraian/Output: ${draftDescription.ifBlank { "-" }}\n" +
                "• Lokasi: $draftLocation\n" +
                "• GPS: $gpsInfo\n" +
                "• Bukti Foto: $photoInfo\n" +
                "• Tujuan Berjenjang: $targetRole ($chatDisplay)\n\n" +
                "Pilih metode pengiriman Telegram:",
            buttons = listOf(
                TgInlineButton("KIRIM_TG_LANGSUNG", "✈️ Simpan & Kirim ke Telegram Nyata"),
                TgInlineButton("KIRIM_TG_PDF", "📄 Simpan & Unggah PDF A4 ke Telegram"),
                TgInlineButton("KIRIM_BERJENJANG", "☁️ Simpan ke Cloud Saja"),
                TgInlineButton("BATAL_LAPOR", "❌ Batal")
            )
        )
    }

    val executeGpsCapture: () -> Unit = {
        scope.launch {
            isCapturingGps = true
            LocationHelper.captureCurrentLocation(context).fold(
                onSuccess = { fix ->
                    draftGpsLat = fix.latitude
                    draftGpsLng = fix.longitude
                    draftGpsAcc = fix.accuracyMeters
                    draftGpsAddr = fix.address
                    draftGpsCaptured = true
                    if (draftLocation.isBlank() && fix.address.isNotBlank()) {
                        draftLocation = fix.address
                    }
                    val summaryText = String.format(
                        Locale.US,
                        "📍 Lokasi GPS Terkunci: %.6f, %.6f (±%.0fm)\n%s",
                        fix.latitude,
                        fix.longitude,
                        fix.accuracyMeters,
                        fix.address
                    )
                    addUserMessage(text = "Membagikan Lokasi GPS ke Bot Telegram", gpsSummary = summaryText)
                    if (currentStep == TgBotStep.ASK_GPS) {
                        currentStep = TgBotStep.ASK_PHOTO
                        val photoButtons = buildList {
                            add(TgInlineButton("AMBIL_KAMERA", "📷 Kamera"))
                            add(TgInlineButton("PILIH_GALERI", "🖼️ Galeri"))
                            if (!adminConfig.requirePhotoEvidence) {
                                add(TgInlineButton("LEWATI_FOTO", "⏭️ Lewati Foto"))
                            }
                        }
                        addBotMessage(
                            text = "✅ Koordinat GPS diterima! Sekarang unggah Bukti Foto Kegiatan menggunakan tombol Kamera/Galeri di bawah:",
                            buttons = photoButtons
                        )
                    }
                },
                onFailure = { err ->
                    val retryButtons = buildList {
                        add(TgInlineButton("BAGIKAN_GPS", "📍 Coba GPS Lagi"))
                        if (!adminConfig.requireGpsLocation) {
                            add(TgInlineButton("LEWATI_GPS", "⏭️ Lewati GPS"))
                        }
                    }
                    addBotMessage(
                        text = "⚠️ Gagal mengambil sinyal GPS: ${err.localizedMessage ?: "Pastikan GPS aktif"}.",
                        buttons = retryButtons
                    )
                }
            )
            isCapturingGps = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            executeGpsCapture()
        } else {
            addBotMessage(
                text = "⚠️ Izin lokasi ditolak.",
                buttons = if (adminConfig.requireGpsLocation) {
                    listOf(TgInlineButton("BAGIKAN_GPS", "📍 Izinkan Lokasi GPS"))
                } else {
                    listOf(TgInlineButton("LEWATI_GPS", "⏭️ Lanjut Tanpa GPS"))
                }
            )
        }
    }

    val handlePhotoSelected: (Uri) -> Unit = { uri ->
        scope.launch {
            isProcessingPhoto = true
            ImageHelper.compressUriToBase64(context, uri).fold(
                onSuccess = { b64 ->
                    draftPhotoBase64 = b64
                    addUserMessage(
                        text = "📷 Mengunggah Bukti Foto Kegiatan",
                        photoBase64 = b64
                    )
                    if (currentStep == TgBotStep.ASK_PHOTO) {
                        promptConfirmStep()
                    }
                },
                onFailure = { err ->
                    addBotMessage(
                        text = "⚠️ Gagal memproses foto: ${err.localizedMessage ?: "Coba foto lain"}",
                        buttons = listOf(
                            TgInlineButton("PILIH_GALERI", "🖼️ Pilih Galeri"),
                            TgInlineButton("LEWATI_FOTO", "⏭️ Lewati Foto")
                        )
                    )
                }
            )
            isProcessingPhoto = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handlePhotoSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) handlePhotoSelected(uri)
    }

    fun startNewReportWizard() {
        currentStep = TgBotStep.ASK_CLASSIFICATION
        draftSummary = ""
        draftDescription = ""
        draftLocation = ""
        draftGpsCaptured = false
        draftPhotoBase64 = ""
        addBotMessage(
            text = "📝 PENCATATAN KEGIATAN HARIAN ASN (BOT TELEGRAM)\n\n" +
                "Langkah 1/7: Pilih Klasifikasi Kegiatan yang Anda laksanakan (ketuk Inline Keyboard atau ketik angka 1–4):",
            buttons = listOf(
                TgInlineButton("CLS_1", "1️⃣ Kegiatan Rutin"),
                TgInlineButton("CLS_2", "2️⃣ Reaktif"),
                TgInlineButton("CLS_3", "3️⃣ Arahan Pimpinan"),
                TgInlineButton("CLS_4", "4️⃣ Tugas Tambahan Lainnya")
            )
        )
    }

    fun processCommandOrStep(rawInput: String) {
        val text = rawInput.trim()
        if (text.isBlank()) return

        when {
            text.equals("/start", ignoreCase = true) ||
                text.equals("/menu", ignoreCase = true) ||
                text.equals("menu", ignoreCase = true) ||
                text.equals("BATAL_LAPOR", ignoreCase = true) ||
                text.equals("/batal", ignoreCase = true) -> {
                addUserMessage(if (text == "BATAL_LAPOR") "/batal (Batalkan Pencatatan)" else text)
                currentStep = TgBotStep.IDLE
                showStartMenu()
                return
            }
            text.equals("/admin", ignoreCase = true) || text.equals("admin", ignoreCase = true) -> {
                onOpenAdminSettings()
                return
            }
            text.equals("/atur_tg", ignoreCase = true) || text.equals("/profil", ignoreCase = true) -> {
                onOpenProfileSetup()
                return
            }
            text.equals("/lapor", ignoreCase = true) || text.equals("lapor", ignoreCase = true) -> {
                addUserMessage("➕ /lapor")
                startNewReportWizard()
                return
            }
            text.equals("/rekap", ignoreCase = true) || text.equals("rekap", ignoreCase = true) -> {
                addUserMessage("📊 /rekap")
                val rutin = myReports.count { it.classificationEnum() == ActivityClassification.RUTIN }
                val reaktif = myReports.count { it.classificationEnum() == ActivityClassification.REAKTIF }
                val arahan = myReports.count { it.classificationEnum() == ActivityClassification.ARAHAN_PIMPINAN }
                val tambahan = myReports.count { it.classificationEnum() == ActivityClassification.TUGAS_TAMBAHAN }
                val approved = myReports.count { it.statusEnum() == ReportStatus.APPROVED_FINAL }
                val latestLines = if (myReports.isEmpty()) {
                    "Belum ada laporan yang dikirim."
                } else {
                    myReports.take(4).mapIndexed { i, r ->
                        "${i + 1}. [${r.classificationEnum().shortLabel}] ${r.summary}\n   ↳ Status: ${r.statusEnum().label} (${r.activityDate})"
                    }.joinToString("\n")
                }
                addBotMessage(
                    text = "📊 REKAPITULASI LAPORAN HARIAN ASN ($activeBotHandle)\n" +
                        "Total Laporan: ${myReports.size} Kegiatan (Disahkan Kadis: $approved)\n\n" +
                        "• Kegiatan Rutin: $rutin\n" +
                        "• Reaktif: $reaktif\n" +
                        "• Arahan Pimpinan: $arahan\n" +
                        "• Tugas Tambahan: $tambahan\n\n" +
                        "Daftar Laporan Terbaru:\n$latestLines",
                    buttons = mainMenuButtons()
                )
                return
            }
            text.equals("/verifikasi", ignoreCase = true) || text.equals("verifikasi", ignoreCase = true) -> {
                addUserMessage("✅ /verifikasi")
                if (pendingQueue.isEmpty()) {
                    addBotMessage(
                        text = "✅ Antrean Verifikasi Berjenjang Kosong\nTidak ada laporan bawahan yang menunggu persetujuan Anda saat ini.",
                        buttons = mainMenuButtons()
                    )
                } else {
                    addBotMessage(
                        text = "🗂️ Ditemukan ${pendingQueue.size} Laporan Menunggu Verifikasi Berjenjang:\n" +
                            "Ketuk Inline Keyboard di bawah untuk menyetujui & meneruskan notifikasi ke Telegram jenjang berikutnya."
                    )
                    pendingQueue.take(5).forEach { item ->
                        addBotMessage(
                            text = "📌 ${item.authorName} (${item.authorTierEnum().label})\n" +
                                "• Klasifikasi: ${item.classificationEnum().label}\n" +
                                "• Sub Kegiatan: ${item.subKegiatan}\n" +
                                "• Ringkasan: ${item.summary}\n" +
                                "• Lokasi: ${item.locationName}\n" +
                                "• Status Saat Ini: ${item.statusEnum().tierLabel}",
                            reportAttachment = item,
                            buttons = listOf(
                                TgInlineButton("ACC_TG_${item.id}", "✅ Setujui & Teruskan ke Telegram"),
                                TgInlineButton("REV_${item.id}", "↩️ Kembalikan Revisi"),
                                TgInlineButton("DTL_${item.id}", "🔍 Lihat Detail")
                            )
                        )
                    }
                }
                return
            }
            text.equals("/pdf", ignoreCase = true) || text.equals("pdf", ignoreCase = true) -> {
                addUserMessage("📄 /pdf")
                if (myReports.isEmpty()) {
                    addBotMessage(
                        text = "⚠️ Belum ada data laporan kegiatan untuk dicetak ke PDF. Silakan catat kegiatan terlebih dahulu dengan /lapor.",
                        buttons = listOf(TgInlineButton("/lapor", "➕ /lapor"))
                    )
                } else {
                    onExportPdf(myReports, "Rekap Laporan Harian via Bot Telegram")
                }
                return
            }
            text.equals("/tg_pdf", ignoreCase = true) || text.equals("/wa_pdf", ignoreCase = true) -> {
                addUserMessage("📤 /tg_pdf (Kirim Berkas PDF A4 ke Telegram)")
                sendReportPdfToRealTelegram(myReports, myReports.firstOrNull())
                return
            }
            text.equals("/tg", ignoreCase = true) || text.equals("/wa", ignoreCase = true) -> {
                addUserMessage("✈️ /tg (Kirim Laporan & Pin GPS ke Telegram)")
                val latest = myReports.firstOrNull()
                if (latest == null) {
                    addBotMessage(
                        text = "⚠️ Belum ada laporan untuk dikirim ke Telegram. Buat laporan baru terlebih dahulu dengan /lapor.",
                        buttons = listOf(TgInlineButton("/lapor", "➕ /lapor"))
                    )
                } else {
                    scope.launch {
                        val res = TelegramBotService.dispatchReportToTelegram(
                            context = context,
                            report = latest,
                            profile = profile,
                            adminConfig = adminConfig
                        )
                        addBotMessage(
                            text = "✈️ Status Pengiriman Telegram:\n${res.message}",
                            buttons = mainMenuButtons()
                        )
                    }
                }
                return
            }
            text.startsWith("ACC_TG_") -> {
                val reportId = text.removePrefix("ACC_TG_")
                val target = queueReports.firstOrNull { it.id == reportId }
                if (target != null) {
                    addUserMessage("✅ Setujui & Teruskan ke Telegram: ${target.summary}")
                    onReviewDecision(target, true, "Diverifikasi & diteruskan via Bot Telegram $activeBotHandle")
                    val escalatedStatus = when (target.statusEnum()) {
                        ReportStatus.SUBMITTED_PENGAWAS -> ReportStatus.SUBMITTED_ADMINISTRATOR.name
                        ReportStatus.SUBMITTED_ADMINISTRATOR -> ReportStatus.SUBMITTED_KADIS.name
                        else -> ReportStatus.APPROVED_FINAL.name
                    }
                    val updatedCopy = target.copy(status = escalatedStatus)
                    scope.launch {
                        val dispatch = TelegramBotService.dispatchReportToTelegram(
                            context = context,
                            report = updatedCopy,
                            profile = profile,
                            adminConfig = adminConfig
                        )
                        addBotMessage(
                            text = "✅ Laporan \"${target.summary}\" diverifikasi!\n✈️ ${dispatch.message}",
                            buttons = mainMenuButtons()
                        )
                    }
                }
                return
            }
            text.startsWith("REV_") -> {
                val reportId = text.removePrefix("REV_")
                val target = queueReports.firstOrNull { it.id == reportId }
                if (target != null) {
                    addUserMessage("↩️ Kembalikan Revisi: ${target.summary}")
                    onReviewDecision(target, false, "Mohon lengkapi rincian output dan bukti foto kegiatan.")
                    addBotMessage(
                        text = "↩️ Laporan \"${target.summary}\" telah dikembalikan kepada pelapor untuk direvisi.",
                        buttons = mainMenuButtons()
                    )
                }
                return
            }
            text.startsWith("DTL_") -> {
                val reportId = text.removePrefix("DTL_")
                onOpenDetail(reportId)
                return
            }
        }

        // Conversational Step Machine
        when (currentStep) {
            TgBotStep.IDLE -> {
                addUserMessage(text)
                showStartMenu()
            }

            TgBotStep.ASK_CLASSIFICATION -> {
                val chosen = when (text.uppercase(Locale.US)) {
                    "1", "CLS_1", "RUTIN" -> ActivityClassification.RUTIN
                    "2", "CLS_2", "REAKTIF" -> ActivityClassification.REAKTIF
                    "3", "CLS_3", "ARAHAN" -> ActivityClassification.ARAHAN_PIMPINAN
                    "4", "CLS_4", "TAMBAHAN" -> ActivityClassification.TUGAS_TAMBAHAN
                    else -> ActivityClassification.entries.firstOrNull {
                        it.label.contains(text, ignoreCase = true)
                    } ?: ActivityClassification.RUTIN
                }
                draftClassification = chosen
                addUserMessage("Klasifikasi: ${chosen.label}")
                currentStep = TgBotStep.ASK_BIDANG
                val bidangOpts = DiskominfotikCatalog.units.mapIndexed { idx, unit ->
                    TgInlineButton("BID_$idx", "${idx + 1}. ${unit.name.take(30)}")
                }
                val bidangListText = DiskominfotikCatalog.units.mapIndexed { idx, unit ->
                    "${idx + 1}. ${unit.name}"
                }.joinToString("\n")
                addBotMessage(
                    text = "Langkah 2/7: Pilih Bidang / Sekretariat pada Dinas Kominfo & Statistik Provinsi Gorontalo:\n\n$bidangListText",
                    buttons = bidangOpts
                )
            }

            TgBotStep.ASK_BIDANG -> {
                val idx = text.removePrefix("BID_").trim().toIntOrNull()?.let {
                    if (text.startsWith("BID_")) it else it - 1
                }
                val selectedUnit = if (idx != null && idx in DiskominfotikCatalog.units.indices) {
                    DiskominfotikCatalog.units[idx]
                } else {
                    DiskominfotikCatalog.units.firstOrNull {
                        it.name.contains(text, ignoreCase = true)
                    } ?: DiskominfotikCatalog.units.first()
                }
                draftBidang = selectedUnit.name
                addUserMessage("Unit: ${selectedUnit.name}")
                currentStep = TgBotStep.ASK_SUB_KEGIATAN
                val subOpts = selectedUnit.subKegiatans.mapIndexed { sIdx, sub ->
                    TgInlineButton("SUB_$sIdx", "Sub ${sIdx + 1}: ${sub.take(28)}...")
                }
                val subListText = selectedUnit.subKegiatans.mapIndexed { sIdx, sub ->
                    "${sIdx + 1}. $sub"
                }.joinToString("\n")
                addBotMessage(
                    text = "Langkah 3/7: Pilih Sub Kegiatan pada ${selectedUnit.name} (atau ketik langsung nama sub kegiatan):\n\n$subListText",
                    buttons = subOpts
                )
            }

            TgBotStep.ASK_SUB_KEGIATAN -> {
                val subList = DiskominfotikCatalog.subKegiatansFor(draftBidang)
                val sIdx = text.removePrefix("SUB_").trim().toIntOrNull()?.let {
                    if (text.startsWith("SUB_")) it else it - 1
                }
                val chosenSub = if (sIdx != null && sIdx in subList.indices) {
                    subList[sIdx]
                } else {
                    text
                }
                draftSubKegiatan = chosenSub
                addUserMessage("Sub Kegiatan: $chosenSub")
                currentStep = TgBotStep.ASK_SUMMARY
                addBotMessage(
                    text = "Langkah 4/7: Ketik Ringkasan Kegiatan dan Aktivitas harian yang Anda laksanakan (minimal 3 karakter):\n\nContoh: Pemeliharaan Jaringan Intra Pemerintah & Monitoring Bandwidth OPD"
                )
            }

            TgBotStep.ASK_SUMMARY -> {
                if (text.length < 3) {
                    addUserMessage(text)
                    addBotMessage("⚠️ Ringkasan kegiatan terlalu singkat. Mohon ketik minimal 3 karakter.")
                    return
                }
                draftSummary = text
                addUserMessage(text)
                currentStep = TgBotStep.ASK_DESCRIPTION
                addBotMessage(
                    text = "Langkah 5/7: Ketik Uraian Pelaksanaan & Output Hasil Kerja (atau ketuk tombol cepat):",
                    buttons = listOf(
                        TgInlineButton("SAMA_RINGKASAN", "✨ Gunakan Ringkasan & Output Terlaksana")
                    )
                )
            }

            TgBotStep.ASK_DESCRIPTION -> {
                val desc = if (text == "SAMA_RINGKASAN") {
                    "Telah dilaksanakan $draftSummary dengan baik sesuai prosedur dan menghasilkan output laporan teknis."
                } else {
                    text
                }
                draftDescription = desc
                addUserMessage(desc)
                currentStep = TgBotStep.ASK_LOCATION
                addBotMessage(
                    text = "Langkah 6/7: Di mana Lokasi / Tempat pelaksanaan kegiatan tersebut?",
                    buttons = listOf(
                        TgInlineButton("LOK_KANTOR", "🏢 Kantor Diskominfotik Prov. Gorontalo"),
                        TgInlineButton("LOK_GCC", "🖥️ Gorontalo Command Center"),
                        TgInlineButton("LOK_GUBERNUR", "🏛️ Kantor Gubernur Gorontalo")
                    )
                )
            }

            TgBotStep.ASK_LOCATION -> {
                val loc = when (text) {
                    "LOK_KANTOR" -> "Kantor Dinas Kominfo & Statistik Provinsi Gorontalo"
                    "LOK_GCC" -> "Ruang Gorontalo Command Center (GCC)"
                    "LOK_GUBERNUR" -> "Kompleks Kantor Gubernur Provinsi Gorontalo"
                    else -> text
                }
                draftLocation = loc
                addUserMessage("Lokasi: $loc")
                currentStep = TgBotStep.ASK_GPS
                val gpsBtns = buildList {
                    add(TgInlineButton("BAGIKAN_GPS", "📍 Bagikan Lokasi GPS"))
                    if (!adminConfig.requireGpsLocation) {
                        add(TgInlineButton("LEWATI_GPS", "⏭️ Lewati GPS"))
                    }
                }
                addBotMessage(
                    text = "Langkah 7/7 (Bagian A): Lampirkan Titik Koordinat GPS lokasi kegiatan Anda (Bot akan mengirimkan pin lokasi interaktif via sendLocation ke Telegram):",
                    buttons = gpsBtns
                )
            }

            TgBotStep.ASK_GPS -> {
                when (text) {
                    "BAGIKAN_GPS" -> {
                        if (LocationHelper.hasLocationPermission(context)) {
                            executeGpsCapture()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                    "LEWATI_GPS" -> {
                        if (adminConfig.requireGpsLocation) {
                            addBotMessage(
                                text = "🔒 Admin mewajibkan titik lokasi GPS pada setiap laporan.",
                                buttons = listOf(TgInlineButton("BAGIKAN_GPS", "📍 Bagikan Lokasi GPS"))
                            )
                            return
                        }
                        addUserMessage("Lewati Koordinat GPS")
                        currentStep = TgBotStep.ASK_PHOTO
                        val photoBtns = buildList {
                            add(TgInlineButton("AMBIL_KAMERA", "📷 Ambil Foto Kamera"))
                            add(TgInlineButton("PILIH_GALERI", "🖼️ Pilih dari Galeri"))
                            if (!adminConfig.requirePhotoEvidence) {
                                add(TgInlineButton("LEWATI_FOTO", "⏭️ Lewati Foto"))
                            }
                        }
                        addBotMessage(
                            text = "Langkah 7/7 (Bagian B): Unggah Bukti Foto Kegiatan melalui Kamera atau Galeri:",
                            buttons = photoBtns
                        )
                    }
                    else -> {
                        addUserMessage(text)
                        addBotMessage(
                            text = "Ketuk tombol Bagikan Lokasi GPS di bawah:",
                            buttons = listOf(TgInlineButton("BAGIKAN_GPS", "📍 Bagikan Lokasi GPS"))
                        )
                    }
                }
            }

            TgBotStep.ASK_PHOTO -> {
                when (text) {
                    "AMBIL_KAMERA" -> {
                        val uri = ImageHelper.createTempCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                    "PILIH_GALERI" -> {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    "LEWATI_FOTO" -> {
                        if (adminConfig.requirePhotoEvidence) {
                            addBotMessage(
                                text = "🔒 Admin mewajibkan unggah Bukti Foto Kegiatan.",
                                buttons = listOf(
                                    TgInlineButton("AMBIL_KAMERA", "📷 Ambil Foto Kamera"),
                                    TgInlineButton("PILIH_GALERI", "🖼️ Pilih dari Galeri")
                                )
                            )
                            return
                        }
                        addUserMessage("Lewati Bukti Foto")
                        promptConfirmStep()
                    }
                    else -> {
                        addUserMessage(text)
                        promptConfirmStep()
                    }
                }
            }

            TgBotStep.READY_CONFIRM -> {
                val sendRealTgText = text == "KIRIM_TG_LANGSUNG"
                val sendRealTgPdf = text == "KIRIM_TG_PDF"
                val sendCloudOnly = text == "KIRIM_BERJENJANG" || text.equals("kirim", ignoreCase = true)

                if (!sendRealTgText && !sendRealTgPdf && !sendCloudOnly) {
                    addUserMessage(text)
                    addBotMessage(
                        text = "Silakan pilih metode pengiriman Telegram di bawah:",
                        buttons = listOf(
                            TgInlineButton("KIRIM_TG_LANGSUNG", "✈️ Simpan & Kirim ke Telegram Nyata"),
                            TgInlineButton("KIRIM_TG_PDF", "📄 Simpan & Unggah PDF A4 ke Telegram"),
                            TgInlineButton("KIRIM_BERJENJANG", "☁️ Simpan ke Cloud Saja"),
                            TgInlineButton("BATAL_LAPOR", "❌ Batal")
                        )
                    )
                    return
                }

                addUserMessage(
                    when {
                        sendRealTgPdf -> "📄 Simpan & Unggah Dokumen PDF ke Telegram"
                        sendRealTgText -> "✈️ Simpan & Kirim Laporan ke Telegram Nyata"
                        else -> "☁️ Simpan & Kirim Berjenjang"
                    }
                )
                val authorTier = profile?.levelEnum() ?: JabatanLevel.PELAKSANA
                val newReport = ActivityReport(
                    authorName = profile?.fullName?.ifBlank { currentUserDisplayName }
                        ?: currentUserDisplayName.ifBlank { "ASN Diskominfotik" },
                    authorNip = profile?.nip.orEmpty(),
                    authorJabatan = profile?.jabatanTitle?.ifBlank { authorTier.label } ?: authorTier.label,
                    authorTier = authorTier.name,
                    authorEmail = currentUserEmail,
                    activityDate = todayIso(),
                    startTime = "08:00",
                    endTime = "16:00",
                    summary = draftSummary,
                    description = draftDescription,
                    classification = draftClassification.name,
                    unitBidang = draftBidang,
                    subKegiatan = draftSubKegiatan,
                    locationName = draftLocation,
                    gpsLatitude = draftGpsLat,
                    gpsLongitude = draftGpsLng,
                    gpsAccuracyMeters = draftGpsAcc,
                    gpsAddress = draftGpsAddr,
                    gpsCaptured = draftGpsCaptured,
                    photoBase64 = draftPhotoBase64,
                    photoCaption = draftSummary.take(120),
                    pengawasEmail = profile?.pengawasEmail.orEmpty(),
                    adminEmail = profile?.adminEmail.orEmpty(),
                    kadisEmail = profile?.kadisEmail.orEmpty()
                )

                currentStep = TgBotStep.IDLE
                onSaveOrSubmitReport(newReport, true) { savedId ->
                    val createdCopy = newReport.copy(
                        id = savedId,
                        status = when (authorTier) {
                            JabatanLevel.PELAKSANA -> ReportStatus.SUBMITTED_PENGAWAS.name
                            JabatanLevel.PENGAWAS_FUNGSIONAL -> ReportStatus.SUBMITTED_ADMINISTRATOR.name
                            JabatanLevel.ADMINISTRATOR -> ReportStatus.SUBMITTED_KADIS.name
                            JabatanLevel.KEPALA_DINAS -> ReportStatus.SUBMITTED_KADIS.name
                        }
                    )

                    if (sendRealTgText) {
                        scope.launch {
                            val dispatch = TelegramBotService.dispatchReportToTelegram(
                                context = context,
                                report = createdCopy,
                                profile = profile,
                                adminConfig = adminConfig
                            )
                            var pdfNote = ""
                            if (adminConfig.autoSendPdfToTelegram && TelegramBotService.isBotApiConfigured(context)) {
                                PdfReportGenerator.generatePdf(
                                    context = context,
                                    profile = profile,
                                    reports = listOf(createdCopy),
                                    periodLabel = "Laporan Harian Otomatis Bot Telegram",
                                    adminConfig = adminConfig
                                ).onSuccess { pdfFile ->
                                    val pdfRes = TelegramBotService.dispatchPdfToTelegram(
                                        context = context,
                                        pdfFile = pdfFile,
                                        report = createdCopy,
                                        profile = profile,
                                        adminConfig = adminConfig
                                    )
                                    if (pdfRes.usedBotApi) {
                                        pdfNote = "\n📄 ${pdfRes.message}"
                                    }
                                }
                            }
                            addBotMessage(
                                text = "✅ LAPORAN TERSIMPAN & DIKIRIM KE TELEGRAM!\n" +
                                    "✈️ ${dispatch.message}$pdfNote",
                                reportAttachment = createdCopy,
                                buttons = mainMenuButtons()
                            )
                        }
                    } else if (sendRealTgPdf) {
                        addBotMessage(
                            text = "✅ LAPORAN TERSIMPAN & MENGIRIM LAMPIRAN PDF A4 KE TELEGRAM!",
                            reportAttachment = createdCopy,
                            buttons = mainMenuButtons()
                        )
                        sendReportPdfToRealTelegram(listOf(createdCopy), createdCopy)
                    } else {
                        addBotMessage(
                            text = "✅ LAPORAN BERHASIL DISIMPAN KE CLOUD!\n" +
                                "Gunakan tombol Kirim Telegram atau PDF ke Telegram pada kartu di bawah kapan saja.",
                            reportAttachment = createdCopy,
                            buttons = mainMenuButtons()
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TgWallpaperBlue),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Telegram Bot Official Blue Header
            Surface(
                color = TgHeaderBlue,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TgAccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Telegram Bot Icon",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "SIKAPDiskominfotikBot",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified Telegram Bot",
                                    tint = Color(0xFF79C4FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (botApiConfigured) {
                                    "bot • $activeBotHandle • Bot API Live"
                                } else {
                                    "bot • $activeBotHandle • Direct Telegram Bridge"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onOpenAdminSettings,
                            modifier = Modifier.testTag("tg_header_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Pengaturan Admin Bot Telegram",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { processCommandOrStep("/tg") },
                            modifier = Modifier.testTag("tg_header_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Kirim ke Telegram",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                currentStep = TgBotStep.IDLE
                                showStartMenu()
                            },
                            modifier = Modifier.testTag("tg_header_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Menu Utama Bot",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Pinned Admin Announcement Bar (Telegram Pinned Message Style)
            if (adminConfig.broadcastAnnouncement.isNotBlank()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAdminSettings() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(TgHeaderBlue)
                        )
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = TgHeaderBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengumuman Admin / Pimpinan Diskominfotik",
                                style = MaterialTheme.typography.labelSmall,
                                color = TgAccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = adminConfig.broadcastAnnouncement,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Chat Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    TgMessageBubble(
                        message = msg,
                        botHandle = activeBotHandle,
                        onInlineButtonClick = { code ->
                            processCommandOrStep(code)
                        },
                        onSendPdfToTelegram = { rep ->
                            sendReportPdfToRealTelegram(listOf(rep), rep)
                        },
                        onSendTextToTelegram = { rep ->
                            scope.launch {
                                val res = TelegramBotService.dispatchReportToTelegram(
                                    context = context,
                                    report = rep,
                                    profile = profile,
                                    adminConfig = adminConfig
                                )
                                addBotMessage("✈️ ${res.message}")
                            }
                        },
                        onOpenReportDetail = onOpenDetail
                    )
                }
            }

            // Slash Command Bar above Composer — High-Contrast Pill Menu
            Surface(
                color = Color(0xFFE5EFF8),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chipColors = AssistChipDefaults.assistChipColors(
                        containerColor = TgAccentBlue,
                        labelColor = Color.White
                    )
                    val chipBorder = BorderStroke(1.dp, TgInlineButtonBorder)

                    AssistChip(
                        onClick = { processCommandOrStep("/lapor") },
                        label = {
                            Text(
                                text = "➕ /lapor",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("chip_cmd_lapor")
                    )
                    AssistChip(
                        onClick = { processCommandOrStep("/tg") },
                        label = {
                            Text(
                                text = "✈️ /tg Kirim",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("chip_cmd_real_tg")
                    )
                    AssistChip(
                        onClick = { processCommandOrStep("/tg_pdf") },
                        label = {
                            Text(
                                text = "📄 /tg_pdf",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("chip_cmd_tg_pdf")
                    )
                    AssistChip(
                        onClick = { processCommandOrStep("/verifikasi") },
                        label = {
                            Text(
                                text = "✅ /verifikasi (${pendingQueue.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("chip_cmd_verifikasi")
                    )
                    AssistChip(
                        onClick = { processCommandOrStep("/rekap") },
                        label = {
                            Text(
                                text = "📊 /rekap",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = chipColors,
                        border = chipBorder
                    )
                    AssistChip(
                        onClick = { processCommandOrStep("/admin") },
                        label = {
                            Text(
                                text = "🛡️ /admin",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = chipColors,
                        border = chipBorder,
                        modifier = Modifier.testTag("chip_cmd_admin")
                    )
                }
            }

            // Telegram Bottom Chat Input Composer
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) {
                                executeGpsCapture()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        enabled = !isCapturingGps,
                        modifier = Modifier.testTag("bot_attach_gps_button")
                    ) {
                        if (isCapturingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = "Lampirkan GPS",
                                tint = TgAccentBlue
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingPhoto,
                        modifier = Modifier.testTag("bot_attach_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Lampirkan Foto Galeri",
                            tint = TgAccentBlue
                        )
                    }

                    IconButton(
                        onClick = {
                            val uri = ImageHelper.createTempCameraUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        enabled = !isProcessingPhoto,
                        modifier = Modifier.testTag("bot_attach_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Ambil Foto Kamera",
                            tint = TgAccentBlue
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = when (currentStep) {
                                    TgBotStep.ASK_SUMMARY -> "Ketik ringkasan kegiatan..."
                                    TgBotStep.ASK_DESCRIPTION -> "Ketik uraian output kegiatan..."
                                    TgBotStep.ASK_LOCATION -> "Ketik nama lokasi kegiatan..."
                                    else -> "Ketik perintah /lapor atau /admin..."
                                },
                                color = Color(0xFF475569)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedPlaceholderColor = Color(0xFF475569),
                            unfocusedPlaceholderColor = Color(0xFF475569),
                            focusedBorderColor = TgHeaderBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = TgHeaderBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bot_chat_input")
                    )

                    FilledIconButton(
                        onClick = {
                            val toSend = inputText
                            inputText = ""
                            processCommandOrStep(toSend)
                        },
                        enabled = !isBusy && inputText.trim().isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = TgAccentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF94A3B8),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("bot_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Pesan Telegram"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TgMessageBubble(
    message: TgChatMessage,
    botHandle: String,
    onInlineButtonClick: (String) -> Unit,
    onSendPdfToTelegram: (ActivityReport) -> Unit,
    onSendTextToTelegram: (ActivityReport) -> Unit,
    onOpenReportDetail: (String) -> Unit
) {
    val bubbleColor = if (message.fromUser) TgOutgoingBubble else Color.White
    val bubbleShape = if (message.fromUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!message.fromUser) {
                    Text(
                        text = "SIKAPDiskominfotikBot ($botHandle)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TgAccentBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (message.photoBase64.isNotBlank()) {
                    Base64ImagePreview(
                        base64 = message.photoBase64,
                        contentDescription = "Lampiran Foto Bukti",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                if (message.gpsSummary.isNotBlank()) {
                    Surface(
                        color = Color(0xFFEAF3FA),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TgAccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = message.gpsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0A2540),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0F172A)
                )

                // Attached Report Card inside Telegram Bot Bubble
                if (message.reportAttachment != null) {
                    val rep = message.reportAttachment
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FC)),
                        border = BorderStroke(1.dp, Color(0xFFB8D4EC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (rep.id.isNotBlank()) onOpenReportDetail(rep.id)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "[${rep.classificationEnum().label}] ${rep.summary}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sub Kegiatan: ${rep.subKegiatan}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                text = "Status: ${rep.statusEnum().label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TgAccentBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSendPdfToTelegram(rep) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = TgAccentBlue
                                    ),
                                    border = BorderStroke(1.dp, TgAccentBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        tint = TgAccentBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "PDF Telegram",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TgAccentBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { onSendTextToTelegram(rep) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TgAccentBlue,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Kirim TG",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Timestamp + Telegram double check ticks
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                    if (message.fromUser) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = "Terkirim",
                            tint = TgAccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // High-Contrast Telegram Inline Keyboard Buttons below Bot Message
        if (!message.fromUser && message.inlineButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier.widthIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                message.inlineButtons.forEach { btn ->
                    Surface(
                        color = TgInlineButtonBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TgInlineButtonBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onInlineButtonClick(btn.code) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
