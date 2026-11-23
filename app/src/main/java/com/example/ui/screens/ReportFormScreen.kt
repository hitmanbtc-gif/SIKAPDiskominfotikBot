package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.UserProfile
import com.example.ui.components.Base64ImagePreview
import com.example.ui.theme.GorontaloTeal100
import com.example.ui.theme.GorontaloTeal700
import com.example.util.ImageHelper
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportFormScreen(
    existingReport: ActivityReport?,
    profile: UserProfile?,
    currentUserEmail: String,
    currentUserDisplayName: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSaveOrSubmit: (ActivityReport, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var summary by remember(existingReport?.id) { mutableStateOf(existingReport?.summary.orEmpty()) }
    var description by remember(existingReport?.id) { mutableStateOf(existingReport?.description.orEmpty()) }
    var activityDate by remember(existingReport?.id) {
        mutableStateOf(existingReport?.activityDate?.ifBlank { todayIso } ?: todayIso)
    }
    var startTime by remember(existingReport?.id) {
        mutableStateOf(existingReport?.startTime?.ifBlank { "08:00" } ?: "08:00")
    }
    var endTime by remember(existingReport?.id) {
        mutableStateOf(existingReport?.endTime?.ifBlank { "12:00" } ?: "12:00")
    }
    var classification by remember(existingReport?.id) {
        mutableStateOf(existingReport?.classificationEnum() ?: ActivityClassification.RUTIN)
    }

    val initialBidang = existingReport?.unitBidang?.ifBlank { null }
        ?: profile?.unitBidang?.ifBlank { null }
        ?: DiskominfotikCatalog.units.first().name
    var unitBidang by remember(existingReport?.id, profile?.unitBidang) { mutableStateOf(initialBidang) }
    var subKegiatan by remember(existingReport?.id) {
        mutableStateOf(
            existingReport?.subKegiatan?.ifBlank { null }
                ?: DiskominfotikCatalog.subKegiatansFor(initialBidang).first()
        )
    }
    var locationName by remember(existingReport?.id) { mutableStateOf(existingReport?.locationName.orEmpty()) }

    var gpsLat by remember(existingReport?.id) { mutableDoubleStateOf(existingReport?.gpsLatitude ?: 0.0) }
    var gpsLng by remember(existingReport?.id) { mutableDoubleStateOf(existingReport?.gpsLongitude ?: 0.0) }
    var gpsAccuracy by remember(existingReport?.id) { mutableDoubleStateOf(existingReport?.gpsAccuracyMeters ?: 0.0) }
    var gpsAddress by remember(existingReport?.id) { mutableStateOf(existingReport?.gpsAddress.orEmpty()) }
    var gpsCaptured by remember(existingReport?.id) { mutableStateOf(existingReport?.gpsCaptured ?: false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    var photoBase64 by remember(existingReport?.id) { mutableStateOf(existingReport?.photoBase64.orEmpty()) }
    var photoCaption by remember(existingReport?.id) { mutableStateOf(existingReport?.photoCaption.orEmpty()) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }

    var pengawasEmail by remember(existingReport?.id, profile?.pengawasEmail) {
        mutableStateOf(existingReport?.pengawasEmail?.ifBlank { null } ?: profile?.pengawasEmail.orEmpty())
    }
    var adminEmail by remember(existingReport?.id, profile?.adminEmail) {
        mutableStateOf(existingReport?.adminEmail?.ifBlank { null } ?: profile?.adminEmail.orEmpty())
    }
    var kadisEmail by remember(existingReport?.id, profile?.kadisEmail) {
        mutableStateOf(existingReport?.kadisEmail?.ifBlank { null } ?: profile?.kadisEmail.orEmpty())
    }

    var formValidationError by remember { mutableStateOf<String?>(null) }
    var bidangDropdownExpanded by remember { mutableStateOf(false) }
    var subDropdownExpanded by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val triggerGpsCapture = {
        scope.launch {
            isFetchingGps = true
            gpsError = null
            LocationHelper.captureCurrentLocation(context).fold(
                onSuccess = { fix ->
                    gpsLat = fix.latitude
                    gpsLng = fix.longitude
                    gpsAccuracy = fix.accuracyMeters
                    gpsAddress = fix.address
                    gpsCaptured = true
                    if (locationName.isBlank() && fix.address.isNotBlank()) {
                        locationName = fix.address
                    }
                },
                onFailure = { err ->
                    gpsError = err.localizedMessage ?: "Gagal mengambil koordinat GPS."
                }
            )
            isFetchingGps = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerGpsCapture()
        } else {
            gpsError = "Izin lokasi GPS ditolak. Aktifkan izin lokasi untuk menautkan koordinat."
        }
    }

    val handlePickedImage: (Uri) -> Unit = { uri ->
        scope.launch {
            isProcessingPhoto = true
            photoError = null
            ImageHelper.compressUriToBase64(context, uri).fold(
                onSuccess = { encoded -> photoBase64 = encoded },
                onFailure = { err -> photoError = err.localizedMessage ?: "Gagal memproses foto." }
            )
            isProcessingPhoto = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handlePickedImage(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            handlePickedImage(uri)
        }
    }

    fun buildReportDraft(): ActivityReport {
        val authorTier = profile?.levelEnum() ?: JabatanLevel.PELAKSANA
        return ActivityReport(
            id = existingReport?.id.orEmpty(),
            userId = existingReport?.userId.orEmpty(),
            authorName = profile?.fullName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName.ifBlank { "ASN Diskominfotik" },
            authorNip = profile?.nip.orEmpty(),
            authorJabatan = profile?.jabatanTitle?.ifBlank { authorTier.label } ?: authorTier.label,
            authorTier = authorTier.name,
            authorEmail = currentUserEmail,
            activityDate = activityDate,
            startTime = startTime,
            endTime = endTime,
            summary = summary,
            description = description,
            classification = classification.name,
            unitBidang = unitBidang,
            subKegiatan = subKegiatan,
            locationName = locationName,
            gpsLatitude = gpsLat,
            gpsLongitude = gpsLng,
            gpsAccuracyMeters = gpsAccuracy,
            gpsAddress = gpsAddress,
            gpsCaptured = gpsCaptured,
            photoBase64 = photoBase64,
            photoCaption = photoCaption,
            pengawasEmail = pengawasEmail,
            adminEmail = adminEmail,
            kadisEmail = kadisEmail,
            lastRevisionNote = existingReport?.lastRevisionNote.orEmpty()
        )
    }

    fun submitWithValidation(isSubmit: Boolean) {
        if (summary.trim().length < 3) {
            formValidationError = "Ringkasan kegiatan minimal 3 karakter."
            return
        }
        if (subKegiatan.trim().length < 2) {
            formValidationError = "Sub kegiatan pada Bidang/Sekretariat wajib diisi."
            return
        }
        if (locationName.trim().length < 2) {
            formValidationError = "Lokasi / tempat pelaksanaan kegiatan wajib diisi."
            return
        }
        formValidationError = null
        onSaveOrSubmit(buildReportDraft(), isSubmit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingReport == null) "Catat Kegiatan Harian" else "Ubah Laporan Kegiatan"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("form_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (formValidationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formValidationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // 1. Ringkasan & Uraian
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Ringkasan Kegiatan / Aktivitas *") },
                    placeholder = { Text("Contoh: Rapat Koordinasi SPBE & Pemeliharaan Server") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_summary_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Uraian Pelaksanaan & Output") },
                    placeholder = { Text("Jelaskan rincian aktivitas, kendala, dan output yang dihasilkan...") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_description_input")
                )

                // 2. Date & Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = activityDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tanggal") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        activityDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pilih tanggal")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mulai") },
                        trailingIcon = {
                            IconButton(onClick = {
                                TimePickerDialog(context, { _, h, min ->
                                    startTime = String.format(Locale.US, "%02d:%02d", h, min)
                                }, 8, 0, true).show()
                            }) {
                                Icon(Icons.Filled.Schedule, contentDescription = "Jam mulai")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selesai") },
                        trailingIcon = {
                            IconButton(onClick = {
                                TimePickerDialog(context, { _, h, min ->
                                    endTime = String.format(Locale.US, "%02d:%02d", h, min)
                                }, 12, 0, true).show()
                            }) {
                                Icon(Icons.Filled.Schedule, contentDescription = "Jam selesai")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Klasifikasi Kegiatan
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Klasifikasi Kegiatan *",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActivityClassification.entries.forEach { cls ->
                            FilterChip(
                                selected = classification == cls,
                                onClick = { classification = cls },
                                label = { Text(cls.label) },
                                modifier = Modifier.testTag("chip_class_${cls.name}")
                            )
                        }
                    }
                    Text(
                        text = classification.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 4. Hubungan Bidang / Sekretariat & Sub Kegiatan
                ExposedDropdownMenuBox(
                    expanded = bidangDropdownExpanded,
                    onExpandedChange = { bidangDropdownExpanded = !bidangDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = unitBidang,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bidang / Sekretariat *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bidangDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = bidangDropdownExpanded,
                        onDismissRequest = { bidangDropdownExpanded = false }
                    ) {
                        DiskominfotikCatalog.units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    unitBidang = unit.name
                                    subKegiatan = unit.subKegiatans.first()
                                    bidangDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                val subOptions = remember(unitBidang) { DiskominfotikCatalog.subKegiatansFor(unitBidang) }
                ExposedDropdownMenuBox(
                    expanded = subDropdownExpanded,
                    onExpandedChange = { subDropdownExpanded = !subDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = subKegiatan,
                        onValueChange = { subKegiatan = it },
                        label = { Text("Hubungan Sub Kegiatan *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 3,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subDropdownExpanded,
                        onDismissRequest = { subDropdownExpanded = false }
                    ) {
                        subOptions.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    subKegiatan = sub
                                    subDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 5. Lokasi / Tempat & Integrasi GPS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Lokasi & Integrasi GPS", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Lokasi / Tempat Kegiatan *") },
                            placeholder = { Text("Contoh: Ruang Rapat Diskominfotik Prov. Gorontalo") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_location_input")
                        )

                        Button(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    triggerGpsCapture()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isFetchingGps,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("capture_gps_button")
                        ) {
                            if (isFetchingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mengambil Koordinat GPS...")
                            } else {
                                Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.action_capture_gps))
                            }
                        }

                        if (gpsCaptured) {
                            Surface(
                                color = GorontaloTeal100,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "GPS Terkunci: %.6f, %.6f (Akurasi ±%.0f m)",
                                            gpsLat,
                                            gpsLng,
                                            gpsAccuracy
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GorontaloTeal700
                                    )
                                    if (gpsAddress.isNotBlank()) {
                                        Text(
                                            text = gpsAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GorontaloTeal700
                                        )
                                    }
                                }
                            }
                        }
                        if (gpsError != null) {
                            Text(
                                text = gpsError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 6. Unggah Bukti Foto Kegiatan
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Bukti Foto Kegiatan", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isProcessingPhoto,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_photo_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galeri")
                            }
                            OutlinedButton(
                                onClick = {
                                    val uri = ImageHelper.createTempCameraUri(context)
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                enabled = !isProcessingPhoto,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("take_photo_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kamera")
                            }
                        }

                        if (isProcessingPhoto) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Mengompres foto bukti...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (photoError != null) {
                            Text(
                                text = photoError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (photoBase64.isNotBlank()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Base64ImagePreview(
                                    base64 = photoBase64,
                                    contentDescription = stringResource(id = R.string.cd_activity_photo),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                IconButton(
                                    onClick = { photoBase64 = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Hapus foto",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = photoCaption,
                                onValueChange = { photoCaption = it },
                                label = { Text("Keterangan Foto (Opsional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 7. Tujuan Verifikator Berjenjang
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Rute Verifikasi Berjenjang (Opsional Email Atasan)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Isi email Google atasan agar laporan langsung masuk ke kotak verifikasi mereka.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pengawasEmail,
                            onValueChange = { pengawasEmail = it },
                            label = { Text("Email Pejabat Pengawas / Fungsional") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = adminEmail,
                            onValueChange = { adminEmail = it },
                            label = { Text("Email Pejabat Administrator (Kabid/Sekretaris)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = kadisEmail,
                            onValueChange = { kadisEmail = it },
                            label = { Text("Email Kepala Dinas") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Submit Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { submitWithValidation(false) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_draft_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Draf")
                    }
                    Button(
                        onClick = { submitWithValidation(true) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("submit_report_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim Berjenjang")
                        }
                    }
                }
            }
        }
    }
}
