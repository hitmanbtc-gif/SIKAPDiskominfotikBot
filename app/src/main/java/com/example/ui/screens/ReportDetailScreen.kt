package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ActivityReport
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.ReportStatus
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.ClassificationBadge
import com.example.ui.components.GorontaloProvinceLogo
import com.example.ui.components.HierarchicalStepperCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.lapkinTextFieldColors
import com.example.ui.components.lapkinTextFieldTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: ActivityReport?,
    currentUserId: String,
    currentUserEmail: String,
    reviewerDisplayName: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExportPdf: (ActivityReport) -> Unit,
    onReviewDecision: (ActivityReport, Boolean, String, Boolean) -> Unit
) {
    var reviewNote by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GorontaloProvinceLogo(size = 28.dp)
                        Text("Detail Laporan Kegiatan")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (report != null) {
                        IconButton(
                            onClick = { onExportPdf(report) },
                            modifier = Modifier.testTag("detail_export_pdf_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "Ekspor PDF")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Laporan tidak ditemukan.")
            }
            return@Scaffold
        }

        val status = report.statusEnum()
        val isOwner = report.userId == currentUserId
        val normalizedEmail = currentUserEmail.trim().lowercase()
        val canEdit = isOwner && (status == ReportStatus.DRAFT || status == ReportStatus.REVISION)
        val canDelete = isOwner || normalizedEmail == DiskominfotikCatalog.ADMIN_EMAIL
        val isPendingReview = status == ReportStatus.SUBMITTED_PENGAWAS ||
            status == ReportStatus.SUBMITTED_ADMINISTRATOR ||
            status == ReportStatus.SUBMITTED_KADIS
        val canForwardApprovedTier = report.canPengawasForwardToAdmin() || report.canAdminForwardToKadis()
        val isAuthorizedReviewer = normalizedEmail == DiskominfotikCatalog.ADMIN_EMAIL ||
            report.reviewerEmails.any { it.equals(normalizedEmail, ignoreCase = true) }
        val isReviewer = (isPendingReview || canForwardApprovedTier) && isAuthorizedReviewer

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
                // Header Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClassificationBadge(classification = report.classificationEnum())
                            StatusBadge(status = status, overrideLabel = report.approvalStatusLabel())
                        }
                        Text(
                            text = report.summary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${report.activityDate} • Pukul ${report.startTime} – ${report.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (report.description.isNotBlank()) {
                            Text(
                                text = report.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Unit Kerja: ${report.unitBidang}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Sub Kegiatan: ${report.subKegiatan}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pelapor: ${report.authorName} (${report.authorTierEnum().label})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Status Jenjang: ${report.approvalTierDetailLabel()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Location & GPS Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text("Lokasi & Koordinat GPS", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = report.locationName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (report.gpsCaptured) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Lat %.6f, Lng %.6f • Akurasi ±%.0f m",
                                    report.gpsLatitude,
                                    report.gpsLongitude,
                                    report.gpsAccuracyMeters
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (report.gpsAddress.isNotBlank()) {
                                Text(
                                    text = report.gpsAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Koordinat GPS belum ditautkan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Photo Evidence Card
                if (report.photoBase64.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Bukti Foto Kegiatan", style = MaterialTheme.typography.titleMedium)
                            Base64ImagePreview(
                                base64 = report.photoBase64,
                                contentDescription = stringResource(id = R.string.cd_activity_photo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            if (report.photoCaption.isNotBlank()) {
                                Text(
                                    text = report.photoCaption,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Hierarchical Stepper
                HierarchicalStepperCard(report = report)

                // Reviewer Action Panel (for Pengawas / Administrator / Kepala Dinas)
                if (isReviewer) {
                    val panelTitle = when {
                        status == ReportStatus.SUBMITTED_PENGAWAS ->
                            "Pengesahan Pejabat Pengawas / Fungsional"
                        report.canPengawasForwardToAdmin() ->
                            "Opsi Teruskan ke Pejabat Administrator (Oleh Pengawas/JF)"
                        status == ReportStatus.SUBMITTED_ADMINISTRATOR ->
                            "Pengesahan Pejabat Administrator"
                        report.canAdminForwardToKadis() ->
                            "Opsi Teruskan ke Kepala Dinas (Oleh Pejabat Administrator)"
                        status == ReportStatus.SUBMITTED_KADIS ->
                            "Pengesahan Akhir Kepala Dinas"
                        else -> "Tindakan Verifikasi"
                    }
                    val panelDescription = when {
                        status == ReportStatus.SUBMITTED_PENGAWAS ->
                            "Laporan dari PPPK / Pelaksana langsung disahkan oleh Pejabat Pengawas / Fungsional. Anda juga dapat memilih untuk meneruskan laporan ini ke Pejabat Administrator."
                        report.canPengawasForwardToAdmin() ->
                            "Laporan ini sudah disahkan oleh Pejabat Pengawas / Fungsional. Jika laporan ini strategis/penting, Anda dapat memilih untuk meneruskannya ke Pejabat Administrator."
                        status == ReportStatus.SUBMITTED_ADMINISTRATOR ->
                            "Laporan dari Pejabat Pengawas/Fungsional (atau yang diteruskan) disahkan oleh Pejabat Administrator. Anda juga dapat memilih untuk meneruskan laporan ini ke Kepala Dinas."
                        report.canAdminForwardToKadis() ->
                            "Laporan ini sudah disahkan oleh Pejabat Administrator. Jika laporan ini perlu diketahui pimpinan tertinggi, Anda dapat memilih untuk meneruskannya ke Kepala Dinas."
                        status == ReportStatus.SUBMITTED_KADIS ->
                            "Laporan pilihan yang diteruskan oleh Pejabat Administrator untuk disahkan oleh Kepala Dinas."
                        else -> ""
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = panelTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (panelDescription.isNotBlank()) {
                                Text(
                                    text = panelDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedTextField(
                                value = reviewNote,
                                onValueChange = { reviewNote = it },
                                label = { Text("Catatan Pengesahan / Arahan / Revisi") },
                                placeholder = { Text("Tulis catatan pengesahan atau alasan revisi...") },
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                colors = lapkinTextFieldColors(),
                                textStyle = lapkinTextFieldTextStyle(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("review_note_input")
                            )

                            if (isPendingReview) {
                                val directApproveLabel = when (status) {
                                    ReportStatus.SUBMITTED_PENGAWAS -> "Sahkan Laporan (Pengawas/JF)"
                                    ReportStatus.SUBMITTED_ADMINISTRATOR -> "Sahkan Laporan (Administrator)"
                                    ReportStatus.SUBMITTED_KADIS -> "Sahkan Laporan (Kepala Dinas)"
                                    else -> "Sahkan Laporan"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onReviewDecision(report, false, reviewNote, false) },
                                        enabled = !isBusy && reviewNote.trim().isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("return_revision_button")
                                    ) {
                                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Revisi")
                                    }
                                    Button(
                                        onClick = { onReviewDecision(report, true, reviewNote, false) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.6f)
                                            .testTag("approve_forward_button")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(directApproveLabel)
                                    }
                                }

                                if (status == ReportStatus.SUBMITTED_PENGAWAS) {
                                    FilledTonalButton(
                                        onClick = { onReviewDecision(report, true, reviewNote, true) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("approve_and_forward_admin_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sahkan & Teruskan ke Pejabat Administrator")
                                    }
                                } else if (status == ReportStatus.SUBMITTED_ADMINISTRATOR) {
                                    FilledTonalButton(
                                        onClick = { onReviewDecision(report, true, reviewNote, true) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("approve_and_forward_kadis_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sahkan & Teruskan ke Kepala Dinas")
                                    }
                                }
                            } else if (report.canPengawasForwardToAdmin()) {
                                Button(
                                    onClick = { onReviewDecision(report, true, reviewNote, true) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("forward_to_administrator_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Teruskan Laporan Ini ke Pejabat Administrator")
                                }
                            } else if (report.canAdminForwardToKadis()) {
                                Button(
                                    onClick = { onReviewDecision(report, true, reviewNote, true) },
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("forward_to_kadis_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Teruskan Laporan Ini ke Kepala Dinas")
                                }
                            }
                        }
                    }
                }

                // Owner Edit / Delete / PDF Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canDelete) {
                        OutlinedButton(
                            onClick = { onDelete(report.id) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Laporan")
                        }
                    }
                    if (canEdit) {
                        Button(
                            onClick = { onEdit(report.id) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ubah & Kirim")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onExportPdf(report) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_pdf_cta_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unduh PDF")
                        }
                    }
                }
            }
        }
    }
}
