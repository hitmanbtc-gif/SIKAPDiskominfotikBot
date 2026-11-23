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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    showAuthor: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val classification = report.classificationEnum()
    val status = report.statusEnum()
    val authorTier = report.authorTierEnum()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Laporan Kegiatan?") },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus laporan \"${report.summary}\" (${report.activityDate})?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_report_${report.id}")
                ) {
                    Text("Ya, Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

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

            Text(
                text = "Jenjang: ${authorTier.submissionRouteSummary()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (showAuthor && report.authorName.isNotBlank()) {
                Text(
                    text = "${report.authorName} • ${authorTier.shortLabel}",
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
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

                if (onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("delete_report_${report.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Hapus Laporan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hapus",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class TierStepVisual(
    val title: String,
    val actor: String,
    val note: String,
    val isCompleted: Boolean,
    val isActive: Boolean
)

@Composable
fun HierarchicalStepperCard(
    report: ActivityReport,
    modifier: Modifier = Modifier
) {
    val status = report.statusEnum()
    val authorTier = report.authorTierEnum()
    val isSubmitted = status != ReportStatus.DRAFT && status != ReportStatus.REVISION
    val approvedByPengawas = report.canPengawasForwardToAdmin() ||
        report.pengawasReviewerName.isNotBlank() ||
        status == ReportStatus.SUBMITTED_ADMINISTRATOR ||
        status == ReportStatus.SUBMITTED_KADIS ||
        (status == ReportStatus.APPROVED_FINAL && authorTier == JabatanLevel.PELAKSANA)
    val approvedByAdmin = report.canAdminForwardToKadis() ||
        report.adminReviewerName.isNotBlank() ||
        status == ReportStatus.SUBMITTED_KADIS ||
        (status == ReportStatus.APPROVED_FINAL && (report.currentTier == "ADMINISTRATOR" || report.currentTier == "COMPLETED"))
    val approvedByKadis = (status == ReportStatus.APPROVED_FINAL && report.currentTier == "COMPLETED") ||
        report.kadisReviewerName.isNotBlank()

    val tierFlowSubtitle = when (authorTier) {
        JabatanLevel.PELAKSANA ->
            "Jenjang Pengajuan Pelaksana / PPPK ➔ Diajukan langsung ke Pejabat Pengawas / Fungsional untuk Disahkan."
        JabatanLevel.PENGAWAS_FUNGSIONAL ->
            "Jenjang Pengajuan Pejabat Pengawas / Fungsional ➔ Diajukan langsung ke Pejabat Administrator untuk Disahkan."
        else ->
            "Jenjang Pengajuan Pejabat Administrator ➔ Diajukan langsung kepada Kepala Dinas untuk Disahkan."
    }

    val steps: List<TierStepVisual> = when (authorTier) {
        JabatanLevel.PELAKSANA -> buildList {
            add(
                TierStepVisual(
                    title = "1. Pengajuan Laporan (Pelaksana / PPPK)",
                    actor = report.authorName.ifBlank { "Pelapor Pelaksana / PPPK" },
                    note = if (isSubmitted) "Telah diajukan ke Pejabat Pengawas / Fungsional" else "Belum diajukan (Draf / Revisi)",
                    isCompleted = isSubmitted,
                    isActive = !isSubmitted
                )
            )
            add(
                TierStepVisual(
                    title = "2. Pengesahan Pejabat Pengawas / Fungsional",
                    actor = report.pengawasReviewerName.ifBlank { report.pengawasEmail.ifBlank { "Pejabat Pengawas / Fungsional" } },
                    note = report.pengawasNote.ifBlank {
                        if (approvedByPengawas) {
                            "Telah Disahkan oleh Pejabat Pengawas / Fungsional"
                        } else {
                            "Menunggu pengesahan Pejabat Pengawas / Fungsional"
                        }
                    },
                    isCompleted = approvedByPengawas,
                    isActive = status == ReportStatus.SUBMITTED_PENGAWAS
                )
            )
            if (status == ReportStatus.SUBMITTED_ADMINISTRATOR || approvedByAdmin) {
                add(
                    TierStepVisual(
                        title = "3. Penerusan Pilihan ke Pejabat Administrator",
                        actor = report.adminReviewerName.ifBlank { report.adminEmail.ifBlank { "Pejabat Administrator" } },
                        note = report.adminNote.ifBlank {
                            if (approvedByAdmin) "Disahkan oleh Pejabat Administrator"
                            else "Diteruskan oleh Pengawas/Fungsional ke Pejabat Administrator"
                        },
                        isCompleted = approvedByAdmin,
                        isActive = status == ReportStatus.SUBMITTED_ADMINISTRATOR
                    )
                )
            }
            if (status == ReportStatus.SUBMITTED_KADIS || approvedByKadis) {
                add(
                    TierStepVisual(
                        title = "4. Penerusan Pilihan kepada Kepala Dinas",
                        actor = report.kadisReviewerName.ifBlank { report.kadisEmail.ifBlank { "Kepala Dinas" } },
                        note = report.kadisNote.ifBlank {
                            if (approvedByKadis) "Disahkan oleh Kepala Dinas"
                            else "Menunggu pengesahan Kepala Dinas"
                        },
                        isCompleted = approvedByKadis,
                        isActive = status == ReportStatus.SUBMITTED_KADIS
                    )
                )
            }
        }

        JabatanLevel.PENGAWAS_FUNGSIONAL -> buildList {
            add(
                TierStepVisual(
                    title = "1. Pengajuan Laporan (Pejabat Pengawas / Fungsional)",
                    actor = report.authorName.ifBlank { "Pejabat Pengawas / Fungsional" },
                    note = if (isSubmitted) "Telah diajukan ke Pejabat Administrator" else "Belum diajukan (Draf / Revisi)",
                    isCompleted = isSubmitted,
                    isActive = !isSubmitted
                )
            )
            add(
                TierStepVisual(
                    title = "2. Pengesahan Pejabat Administrator",
                    actor = report.adminReviewerName.ifBlank { report.adminEmail.ifBlank { "Pejabat Administrator / Kabid / Sekretaris" } },
                    note = report.adminNote.ifBlank {
                        if (approvedByAdmin) {
                            "Telah Disahkan oleh Pejabat Administrator"
                        } else {
                            "Menunggu pengesahan Pejabat Administrator"
                        }
                    },
                    isCompleted = approvedByAdmin,
                    isActive = status == ReportStatus.SUBMITTED_ADMINISTRATOR
                )
            )
            if (status == ReportStatus.SUBMITTED_KADIS || approvedByKadis) {
                add(
                    TierStepVisual(
                        title = "3. Penerusan Pilihan kepada Kepala Dinas",
                        actor = report.kadisReviewerName.ifBlank { report.kadisEmail.ifBlank { "Kepala Dinas" } },
                        note = report.kadisNote.ifBlank {
                            if (approvedByKadis) "Disahkan oleh Kepala Dinas"
                            else "Diteruskan oleh Administrator kepada Kepala Dinas"
                        },
                        isCompleted = approvedByKadis,
                        isActive = status == ReportStatus.SUBMITTED_KADIS
                    )
                )
            }
        }

        JabatanLevel.ADMINISTRATOR, JabatanLevel.KEPALA_DINAS -> listOf(
            TierStepVisual(
                title = "1. Pengajuan Laporan (Pejabat Administrator)",
                actor = report.authorName.ifBlank { "Pejabat Administrator" },
                note = if (isSubmitted) "Telah diajukan kepada Kepala Dinas" else "Belum diajukan (Draf / Revisi)",
                isCompleted = isSubmitted,
                isActive = !isSubmitted
            ),
            TierStepVisual(
                title = "2. Pengesahan Kepala Dinas",
                actor = report.kadisReviewerName.ifBlank { report.kadisEmail.ifBlank { "Kepala Dinas Kominfo & Statistik" } },
                note = report.kadisNote.ifBlank {
                    if (approvedByKadis) {
                        "Telah Disahkan oleh Kepala Dinas"
                    } else {
                        "Menunggu pengesahan Kepala Dinas"
                    }
                },
                isCompleted = approvedByKadis,
                isActive = status == ReportStatus.SUBMITTED_KADIS
            )
        )
    }

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
                        text = "Jenjang Pengajuan & Pengesahan Laporan",
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

            steps.forEachIndexed { index, step ->
                val circleBg = when {
                    step.isCompleted -> GorontaloTeal600Color()
                    step.isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val circleFg = when {
                    step.isCompleted || step.isActive -> Color.White
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
                            if (step.isCompleted) {
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
                                        if (step.isCompleted) GorontaloTeal600Color()
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (step.isActive || step.isCompleted) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = step.actor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = step.note,
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


