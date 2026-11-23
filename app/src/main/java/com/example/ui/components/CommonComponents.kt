package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.JabatanLevel
import com.example.data.model.ReportStatus
import com.example.ui.theme.AmberGold100
import com.example.ui.theme.AmberGold700
import com.example.ui.theme.CivicIndigo100
import com.example.ui.theme.CivicIndigo700
import com.example.ui.theme.CoralRed100
import com.example.ui.theme.CoralRed700
import com.example.ui.theme.GorontaloTeal100
import com.example.ui.theme.GorontaloTeal700
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.VioletAccent100
import com.example.ui.theme.VioletAccent700
import com.example.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GorontaloProvinceLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_logo_gorontalo),
        contentDescription = "Logo Provinsi Gorontalo",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

@Composable
fun lapkinTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF090D16),
    unfocusedTextColor = Color(0xFF090D16),
    disabledTextColor = Color(0xFF090D16),
    errorTextColor = Color(0xFF7F1D1D),
    focusedContainerColor = Color(0xFFFFFFFF),
    unfocusedContainerColor = Color(0xFFFFFFFF),
    disabledContainerColor = Color(0xFFF8FAFC),
    focusedLabelColor = Color(0xFF0B3D70),
    unfocusedLabelColor = Color(0xFF1E293B),
    disabledLabelColor = Color(0xFF334155),
    focusedPlaceholderColor = Color(0xFF334155),
    unfocusedPlaceholderColor = Color(0xFF334155),
    focusedBorderColor = Color(0xFF0B3D70),
    unfocusedBorderColor = Color(0xFF475569),
    cursorColor = Color(0xFF0B3D70)
)

@Composable
fun lapkinTextFieldTextStyle(): TextStyle = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.5.sp,
    lineHeight = 22.sp,
    color = Color(0xFF090D16)
)

@Composable
fun classificationStyle(classification: ActivityClassification): Triple<Color, Color, ImageVector> {
    return when (classification) {
        ActivityClassification.RUTIN ->
            Triple(CivicIndigo100, CivicIndigo700, Icons.Filled.WorkHistory)
        ActivityClassification.REAKTIF ->
            Triple(CoralRed100, CoralRed700, Icons.Filled.Bolt)
        ActivityClassification.ARAHAN_PIMPINAN ->
            Triple(VioletAccent100, VioletAccent700, Icons.Filled.Campaign)
        ActivityClassification.TUGAS_TAMBAHAN ->
            Triple(GorontaloTeal100, GorontaloTeal700, Icons.Filled.AddTask)
    }
}

@Composable
fun statusColors(status: ReportStatus): Pair<Color, Color> {
    return when (status) {
        ReportStatus.DRAFT ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        ReportStatus.SUBMITTED_PENGAWAS ->
            AmberGold100 to AmberGold700
        ReportStatus.SUBMITTED_ADMINISTRATOR ->
            CivicIndigo100 to CivicIndigo700
        ReportStatus.SUBMITTED_KADIS ->
            VioletAccent100 to VioletAccent700
        ReportStatus.REVISION ->
            CoralRed100 to CoralRed700
        ReportStatus.APPROVED_FINAL ->
            GorontaloTeal100 to GorontaloTeal700
    }
}

@Composable
fun ClassificationBadge(
    classification: ActivityClassification,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = classificationStyle(classification)
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = classification.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: ReportStatus,
    modifier: Modifier = Modifier,
    overrideLabel: String? = null
) {
    val (bg, fg) = statusColors(status)
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = overrideLabel ?: status.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Base64ImagePreview(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, base64) {
        value = if (base64.isBlank()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                ImageHelper.decodeBase64ToBitmap(base64)?.asImageBitmap()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Composable
fun ActivityReportCard(
    report: ActivityReport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAuthor: Boolean = false
) {
    val classification = report.classificationEnum()
    val status = report.statusEnum()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("activity_card_${report.id}"),
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
                ClassificationBadge(classification = classification)
                StatusBadge(
                    status = status,
                    overrideLabel = report.approvalStatusLabel()
                )
            }

            Text(
                text = report.summary,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (showAuthor && report.authorName.isNotBlank()) {
                Text(
                    text = "${report.authorName} • ${report.authorTierEnum().shortLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "${report.unitBidang} — ${report.subKegiatan}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${report.activityDate} • ${report.startTime}-${report.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (report.gpsCaptured) {
                    Surface(
                        color = GorontaloTeal100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "GPS Terverifikasi",
                                tint = GorontaloTeal700,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "GPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = GorontaloTeal700
                            )
                        }
                    }
                }

                if (report.photoBase64.isNotBlank()) {
                    Surface(
                        color = CivicIndigo100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Foto Bukti",
                                tint = CivicIndigo700,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Foto",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicIndigo700
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HierarchicalStepperCard(
    report: ActivityReport,
    modifier: Modifier = Modifier
) {
    val status = report.statusEnum()
    val authorTier = report.authorTierEnum()
    val approvedByPengawas = report.canPengawasForwardToAdmin() || report.pengawasReviewerName.isNotBlank()
    val approvedByAdmin = report.canAdminForwardToKadis() || report.adminReviewerName.isNotBlank()
    val approvedByKadis = (status == ReportStatus.APPROVED_FINAL && report.currentTier == "COMPLETED") ||
        report.kadisReviewerName.isNotBlank()

    val currentStep = when (status) {
        ReportStatus.DRAFT, ReportStatus.REVISION -> 0
        ReportStatus.SUBMITTED_PENGAWAS -> 1
        ReportStatus.SUBMITTED_ADMINISTRATOR -> 2
        ReportStatus.SUBMITTED_KADIS -> 3
        ReportStatus.APPROVED_FINAL -> when {
            report.canPengawasForwardToAdmin() -> 2
            report.canAdminForwardToKadis() -> 3
            else -> 4
        }
    }

    val tierFlowSubtitle = when (authorTier) {
        JabatanLevel.PELAKSANA ->
            "Jenjang Pelaporan PPPK/Pelaksana: Hanya sampai Pejabat Pengawas/Fungsional (Pengesah Langsung). Pengawas/Fungsional dapat memilih laporan untuk diteruskan ke Pejabat Administrator."
        JabatanLevel.PENGAWAS_FUNGSIONAL ->
            "Jenjang Pelaporan Pejabat Pengawas/Fungsional: Hanya sampai Pejabat Administrator (Pengesah Langsung). Administrator dapat memilih laporan untuk diteruskan ke Kepala Dinas."
        else ->
            "Jenjang Pelaporan Pejabat Administrator: Langsung disahkan oleh Kepala Dinas."
    }

    val steps = listOf(
        Triple(
            "1. Pelapor (${authorTier.label})",
            report.authorName.ifBlank { "Pelapor ASN" },
            "Pengajuan Laporan Kegiatan Harian"
        ),
        Triple(
            "2. Pejabat Pengawas / Fungsional (Pengesah Laporan PPPK/Pelaksana)",
            report.pengawasReviewerName.ifBlank { report.pengawasEmail.ifBlank { "Pejabat Pengawas / Fungsional" } },
            report.pengawasNote.ifBlank {
                when {
                    report.canPengawasForwardToAdmin() ->
                        "Telah Disahkan oleh Pejabat Pengawas/Fungsional (Dapat dipilih untuk diteruskan ke Administrator)"
                    approvedByPengawas ->
                        "Disahkan & Diteruskan ke Pejabat Administrator"
                    authorTier != JabatanLevel.PELAKSANA ->
                        "Pelapor berasal dari jenjang ${authorTier.shortLabel}"
                    else ->
                        "Menunggu pengesahan langsung Pejabat Pengawas/Fungsional"
                }
            }
        ),
        Triple(
            "3. Pejabat Administrator (Pengesah Laporan Pengawas/JF & Penerusan Pilihan)",
            report.adminReviewerName.ifBlank { report.adminEmail.ifBlank { "Kabid / Sekretaris" } },
            report.adminNote.ifBlank {
                when {
                    report.canAdminForwardToKadis() ->
                        "Telah Disahkan oleh Pejabat Administrator (Dapat dipilih untuk diteruskan ke Kepala Dinas)"
                    approvedByAdmin ->
                        "Disahkan & Diteruskan ke Kepala Dinas"
                    status == ReportStatus.SUBMITTED_ADMINISTRATOR ->
                        "Menunggu pengesahan langsung Pejabat Administrator"
                    else ->
                        "Opsional: Hanya jika laporan dipilih untuk diteruskan oleh Pengawas/Fungsional"
                }
            }
        ),
        Triple(
            "4. Kepala Dinas (Pengesah Laporan Administrator & Penerusan Pilihan)",
            report.kadisReviewerName.ifBlank { report.kadisEmail.ifBlank { "Kepala Dinas Kominfo & Statistik" } },
            report.kadisNote.ifBlank {
                when {
                    approvedByKadis ->
                        "Telah Disahkan Final oleh Kepala Dinas"
                    status == ReportStatus.SUBMITTED_KADIS ->
                        "Menunggu pengesahan Kepala Dinas"
                    else ->
                        "Opsional: Hanya jika laporan dipilih untuk diteruskan oleh Pejabat Administrator"
                }
            }
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Jenjang Pelaporan & Pengesahan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = tierFlowSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (status == ReportStatus.REVISION && report.lastRevisionNote.isNotBlank()) {
                Surface(
                    color = CoralRed100,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = CoralRed700,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Catatan Revisi",
                                style = MaterialTheme.typography.labelLarge,
                                color = CoralRed700
                            )
                            Text(
                                text = report.lastRevisionNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralRed700
                            )
                        }
                    }
                }
            }

            steps.forEachIndexed { index, (title, actor, note) ->
                val isCompleted = when (index) {
                    0 -> status != ReportStatus.DRAFT && status != ReportStatus.REVISION
                    1 -> approvedByPengawas || currentStep > 1
                    2 -> approvedByAdmin || currentStep > 2
                    3 -> approvedByKadis
                    else -> false
                }
                val isActive = currentStep == index &&
                    status != ReportStatus.APPROVED_FINAL &&
                    status != ReportStatus.DRAFT &&
                    status != ReportStatus.REVISION
                val circleBg = when {
                    isCompleted -> GorontaloTeal600Color()
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val circleFg = when {
                    isCompleted || isActive -> Color.White
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(circleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selesai",
                                    tint = circleFg,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = circleFg,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (index < steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(22.dp)
                                    .background(
                                        if (isCompleted) GorontaloTeal600Color()
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = actor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GorontaloTeal600Color(): Color = MaterialTheme.colorScheme.secondary

