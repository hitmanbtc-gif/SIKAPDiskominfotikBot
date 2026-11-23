package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.ReportStatus
import com.example.data.model.UserProfile
import com.example.ui.components.ActivityReportCard
import com.example.ui.components.Base64ImagePreview
import com.example.ui.components.GorontaloProvinceLogo
import com.example.ui.components.classificationStyle
import com.example.ui.viewmodel.UiState

@Composable
fun DashboardScreen(
    profileState: UiState<UserProfile?>,
    reportsState: UiState<List<ActivityReport>>,
    reviewQueueState: UiState<List<ActivityReport>>,
    userDisplayName: String,
    onCreateReport: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenReport: (String) -> Unit,
    onOpenAllReports: (ActivityClassification?) -> Unit,
    onOpenVerificationQueue: () -> Unit,
    onExportPdf: (List<ActivityReport>) -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()
    val queueCount = (reviewQueueState as? UiState.Success)?.data?.count {
        val st = it.statusEnum()
        st == ReportStatus.SUBMITTED_PENGAWAS ||
            st == ReportStatus.SUBMITTED_ADMINISTRATOR ||
            st == ReportStatus.SUBMITTED_KADIS
    } ?: 0

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kominfo_banner),
                        contentDescription = stringResource(id = R.string.cd_banner_kominfo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GorontaloProvinceLogo(size = 46.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(id = R.string.instansi_pemda),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                Text(
                                    text = stringResource(id = R.string.instansi_dinas),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .clickable(onClick = onOpenProfile),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profile?.profilePhotoBase64.isNullOrBlank()) {
                                        Base64ImagePreview(
                                            base64 = profile!!.profilePhotoBase64,
                                            contentDescription = "Foto Profil ASN",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        val displayName = profile?.fullName?.ifBlank { userDisplayName }
                                            ?: userDisplayName.ifBlank { "ASN" }
                                        val initials = displayName.trim()
                                            .split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(2)
                                            .joinToString("") { it.take(1).uppercase() }
                                            .ifBlank { "A" }
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile?.fullName?.ifBlank { userDisplayName } ?: userDisplayName.ifBlank { "ASN Diskominfotik" },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (profile != null) {
                                            "${profile.levelEnum().label} • ${profile.unitBidang}"
                                        } else {
                                            "Lengkapi Profil NIP & Unit Bidang Anda"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onOpenProfile)
                                    .testTag("dashboard_profile_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = "Profil ASN",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (profile == null) "Atur" else "Profil",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profile setup alert if not saved yet
            if (profileState is UiState.Success && profile == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lengkapi Identitas ASN & Atasan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Atur NIP, Tingkatan Jabatan, Bidang/Sekretariat, serta email verifikator untuk kop & tanda tangan PDF.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            FilledTonalButton(
                                onClick = onOpenProfile,
                                modifier = Modifier.testTag("setup_profile_banner_button")
                            ) {
                                Text("Lengkapi")
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCreateReport,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dashboard_add_activity_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Aktivitas")
                    }
                    OutlinedButton(
                        onClick = { onExportPdf(reports) },
                        enabled = reports.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("dashboard_export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF")
                    }
                }
            }

            // Verification Queue Alert Card
            if (queueCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenVerificationQueue)
                            .testTag("dashboard_verification_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Column {
                                    Text(
                                        text = "$queueCount Laporan Menunggu Verifikasi",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Ketuk untuk menelaah, meneruskan, atau mengesahkan berjenjang.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Classification Summary Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Rekapitulasi Klasifikasi Kegiatan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val entries = ActivityClassification.entries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClassificationStatCard(
                            classification = entries[0],
                            count = reports.count { it.classificationEnum() == entries[0] },
                            onClick = { onOpenAllReports(entries[0]) },
                            modifier = Modifier.weight(1f)
                        )
                        ClassificationStatCard(
                            classification = entries[1],
                            count = reports.count { it.classificationEnum() == entries[1] },
                            onClick = { onOpenAllReports(entries[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClassificationStatCard(
                            classification = entries[2],
                            count = reports.count { it.classificationEnum() == entries[2] },
                            onClick = { onOpenAllReports(entries[2]) },
                            modifier = Modifier.weight(1f)
                        )
                        ClassificationStatCard(
                            classification = entries[3],
                            count = reports.count { it.classificationEnum() == entries[3] },
                            onClick = { onOpenAllReports(entries[3]) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Activities Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aktivitas Terbaru (${reports.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (reports.isNotEmpty()) {
                        Text(
                            text = "Lihat Semua",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onOpenAllReports(null) }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            when (reportsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = reportsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (reports.isEmpty()) {
                        item {
                            EmptyReportsCard(onCreateReport = onCreateReport)
                        }
                    } else {
                        items(reports.take(6), key = { it.id }) { report ->
                            ActivityReportCard(
                                report = report,
                                onClick = { onOpenReport(report.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassificationStatCard(
    classification: ActivityClassification,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = classificationStyle(classification)
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("stat_card_${classification.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = classification.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyReportsCard(onCreateReport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_reports),
                contentDescription = stringResource(id = R.string.cd_empty_reports),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Text(
                text = "Belum Ada Laporan Aktivitas Harian",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Mulai catat aktivitas harian Anda, tautkan dengan Sub Kegiatan Bidang/Sekretariat, lengkapi bukti foto & koordinat GPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCreateReport,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buat Laporan Pertama")
            }
        }
    }
}
