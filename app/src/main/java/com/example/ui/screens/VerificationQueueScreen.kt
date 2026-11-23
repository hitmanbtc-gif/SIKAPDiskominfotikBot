package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityReport
import com.example.data.model.ReportStatus
import com.example.ui.components.ActivityReportCard
import com.example.ui.components.GorontaloProvinceLogo
import com.example.ui.viewmodel.UiState

@Composable
fun VerificationQueueScreen(
    queueState: UiState<List<ActivityReport>>,
    isBusy: Boolean = false,
    onOpenReport: (String) -> Unit,
    onQuickReviewDecision: (ActivityReport, Boolean, String, Boolean) -> Unit = { _, _, _, _ -> }
) {
    var selectedTierFilter by remember { mutableStateOf<ReportStatus?>(null) }
    val allQueue = (queueState as? UiState.Success)?.data.orEmpty()
    val pendingOnly = remember(allQueue, selectedTierFilter) {
        val base = allQueue.filter {
            val st = it.statusEnum()
            st == ReportStatus.SUBMITTED_PENGAWAS ||
                st == ReportStatus.SUBMITTED_ADMINISTRATOR ||
                st == ReportStatus.SUBMITTED_KADIS ||
                st == ReportStatus.APPROVED_FINAL
        }
        if (selectedTierFilter == null) base
        else base.filter { it.statusEnum() == selectedTierFilter }
    }

    val tierFilters = listOf(
        ReportStatus.SUBMITTED_PENGAWAS to "Pengesahan Pengawas/JF",
        ReportStatus.SUBMITTED_ADMINISTRATOR to "Pengesahan Administrator",
        ReportStatus.SUBMITTED_KADIS to "Pengesahan Kepala Dinas",
        ReportStatus.APPROVED_FINAL to "Sudah Disahkan (Bisa Diteruskan)"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GorontaloProvinceLogo(size = 42.dp)
                    Column {
                        Text(
                            text = "Pengesahan & Penerusan Laporan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• PPPK/Pelaksana → Disahkan Pengawas/JF (Pengawas memilih laporan untuk diteruskan ke Administrator)\n" +
                                "• Pengawas/JF → Disahkan Administrator (Administrator memilih laporan untuk diteruskan ke Kepala Dinas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTierFilter == null,
                            onClick = { selectedTierFilter = null },
                            label = { Text("Semua (${allQueue.size})") }
                        )
                    }
                    items(tierFilters) { (status, label) ->
                        val count = allQueue.count { it.statusEnum() == status }
                        FilterChip(
                            selected = selectedTierFilter == status,
                            onClick = {
                                selectedTierFilter = if (selectedTierFilter == status) null else status
                            },
                            label = { Text("$label ($count)") }
                        )
                    }
                }
            }

            when (queueState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = queueState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (pendingOnly.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Belum Ada Antrean Pengesahan",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Laporan ASN yang mencantumkan email Anda sebagai Pejabat Pengawas/Fungsional, Pejabat Administrator, atau Kepala Dinas akan muncul secara real-time di sini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pendingOnly, key = { it.id }) { report ->
                                val st = report.statusEnum()
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ActivityReportCard(
                                        report = report,
                                        showAuthor = true,
                                        onClick = { onOpenReport(report.id) }
                                    )
                                    // Quick Pengesahan & Selective Forwarding Action Bar
                                    when {
                                        st == ReportStatus.SUBMITTED_PENGAWAS -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onQuickReviewDecision(
                                                            report,
                                                            true,
                                                            "Disahkan langsung oleh Pejabat Pengawas/Fungsional.",
                                                            false
                                                        )
                                                    },
                                                    enabled = !isBusy,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("quick_approve_pengawas_${report.id}")
                                                ) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Sahkan (Pengawas/JF)", style = MaterialTheme.typography.labelMedium)
                                                }
                                                FilledTonalButton(
                                                    onClick = {
                                                        onQuickReviewDecision(
                                                            report,
                                                            true,
                                                            "Disahkan oleh Pejabat Pengawas/Fungsional & diteruskan ke Pejabat Administrator.",
                                                            true
                                                        )
                                                    },
                                                    enabled = !isBusy,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(1.1f)
                                                        .testTag("quick_forward_admin_${report.id}")
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Sahkan & Teruskan ke Admin", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                        report.canPengawasForwardToAdmin() -> {
                                            FilledTonalButton(
                                                onClick = {
                                                    onQuickReviewDecision(
                                                        report,
                                                        true,
                                                        "Diteruskan oleh Pejabat Pengawas/Fungsional ke Pejabat Administrator.",
                                                        true
                                                    )
                                                },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("select_forward_admin_${report.id}")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pilih & Teruskan Laporan Ini ke Pejabat Administrator")
                                            }
                                        }
                                        st == ReportStatus.SUBMITTED_ADMINISTRATOR -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onQuickReviewDecision(
                                                            report,
                                                            true,
                                                            "Disahkan langsung oleh Pejabat Administrator.",
                                                            false
                                                        )
                                                    },
                                                    enabled = !isBusy,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("quick_approve_admin_${report.id}")
                                                ) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Sahkan (Administrator)", style = MaterialTheme.typography.labelMedium)
                                                }
                                                FilledTonalButton(
                                                    onClick = {
                                                        onQuickReviewDecision(
                                                            report,
                                                            true,
                                                            "Disahkan oleh Pejabat Administrator & diteruskan ke Kepala Dinas.",
                                                            true
                                                        )
                                                    },
                                                    enabled = !isBusy,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(1.1f)
                                                        .testTag("quick_forward_kadis_${report.id}")
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Sahkan & Teruskan ke Kadis", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                        report.canAdminForwardToKadis() -> {
                                            FilledTonalButton(
                                                onClick = {
                                                    onQuickReviewDecision(
                                                        report,
                                                        true,
                                                        "Diteruskan oleh Pejabat Administrator ke Kepala Dinas.",
                                                        true
                                                    )
                                                },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("select_forward_kadis_${report.id}")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pilih & Teruskan Laporan Ini ke Kepala Dinas")
                                            }
                                        }
                                        st == ReportStatus.SUBMITTED_KADIS -> {
                                            Button(
                                                onClick = {
                                                    onQuickReviewDecision(
                                                        report,
                                                        true,
                                                        "Disahkan oleh Kepala Dinas Kominfo & Statistik Provinsi Gorontalo.",
                                                        false
                                                    )
                                                },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("quick_approve_kadis_${report.id}")
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sahkan Laporan (Kepala Dinas)")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
