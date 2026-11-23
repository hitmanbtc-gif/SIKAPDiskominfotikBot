package com.example.ui.screens

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.ReportStatus
import com.example.ui.components.ActivityReportCard
import com.example.ui.components.GorontaloProvinceLogo
import com.example.ui.components.lapkinTextFieldColors
import com.example.ui.components.lapkinTextFieldTextStyle
import com.example.ui.viewmodel.UiState

@Composable
fun ReportListScreen(
    reportsState: UiState<List<ActivityReport>>,
    selectedClassification: ActivityClassification?,
    selectedStatus: ReportStatus?,
    searchQuery: String,
    onSelectClassification: (ActivityClassification?) -> Unit,
    onSelectStatus: (ReportStatus?) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenReport: (String) -> Unit,
    onCreateReport: () -> Unit,
    onExportPdf: (List<ActivityReport>) -> Unit,
    onDeleteReport: (ActivityReport) -> Unit
) {
    val allReports = (reportsState as? UiState.Success)?.data.orEmpty()
    val filteredReports = remember(allReports, selectedClassification, selectedStatus, searchQuery) {
        allReports.filter { item ->
            val matchClass = selectedClassification == null || item.classificationEnum() == selectedClassification
            val matchStatus = selectedStatus == null || item.statusEnum() == selectedStatus
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isBlank() ||
                item.summary.lowercase().contains(q) ||
                item.subKegiatan.lowercase().contains(q) ||
                item.unitBidang.lowercase().contains(q) ||
                item.locationName.lowercase().contains(q)
            matchClass && matchStatus && matchQuery
        }
    }

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        GorontaloProvinceLogo(size = 38.dp)
                        Column {
                            Text(
                                text = "Daftar Laporan Kegiatan",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${filteredReports.size} dari ${allReports.size} aktivitas harian",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { onExportPdf(filteredReports) },
                        enabled = filteredReports.isNotEmpty(),
                        modifier = Modifier.testTag("list_export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor PDF")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cari kegiatan, sub kegiatan, atau lokasi...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = lapkinTextFieldColors(),
                    textStyle = lapkinTextFieldTextStyle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_search_input")
                )

                // Classification filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedClassification == null,
                            onClick = { onSelectClassification(null) },
                            label = { Text("Semua Klasifikasi") }
                        )
                    }
                    items(ActivityClassification.entries) { cls ->
                        FilterChip(
                            selected = selectedClassification == cls,
                            onClick = {
                                onSelectClassification(if (selectedClassification == cls) null else cls)
                            },
                            label = { Text(cls.label) }
                        )
                    }
                }

                // Status filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { onSelectStatus(null) },
                            label = { Text("Semua Status") }
                        )
                    }
                    items(ReportStatus.entries) { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = {
                                onSelectStatus(if (selectedStatus == st) null else st)
                            },
                            label = { Text(st.label) }
                        )
                    }
                }
            }

            when (reportsState) {
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
                            text = reportsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredReports.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyReportsCard(onCreateReport = onCreateReport)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredReports, key = { it.id }) { report ->
                                ActivityReportCard(
                                    report = report,
                                    onClick = { onOpenReport(report.id) },
                                    onDelete = { onDeleteReport(report) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
