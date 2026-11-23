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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminBotConfig
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.UiState
import com.example.util.TelegramBotService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileState: UiState<UserProfile?>,
    adminConfig: AdminBotConfig,
    currentUserEmail: String,
    currentUserDisplayName: String,
    isBusy: Boolean,
    onSaveProfile: (UserProfile) -> Unit,
    onOpenAdminSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = (profileState as? UiState.Success)?.data
    var fullName by remember(existing?.userId, currentUserDisplayName) {
        mutableStateOf(existing?.fullName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName)
    }
    var nip by remember(existing?.userId) { mutableStateOf(existing?.nip.orEmpty()) }
    var jabatanLevel by remember(existing?.userId) {
        mutableStateOf(existing?.levelEnum() ?: JabatanLevel.PELAKSANA)
    }
    var jabatanTitle by remember(existing?.userId) { mutableStateOf(existing?.jabatanTitle.orEmpty()) }
    var unitBidang by remember(existing?.userId) {
        mutableStateOf(
            existing?.unitBidang?.ifBlank { DiskominfotikCatalog.units.first().name }
                ?: DiskominfotikCatalog.units.first().name
        )
    }
    var pengawasEmail by remember(existing?.userId) { mutableStateOf(existing?.pengawasEmail.orEmpty()) }
    var adminEmail by remember(existing?.userId) { mutableStateOf(existing?.adminEmail.orEmpty()) }
    var kadisEmail by remember(existing?.userId) { mutableStateOf(existing?.kadisEmail.orEmpty()) }

    var myTgChatId by remember(existing?.userId) { mutableStateOf(existing?.myPhone.orEmpty()) }
    var pengawasTgChatId by remember(existing?.userId) { mutableStateOf(existing?.pengawasPhone.orEmpty()) }
    var adminTgChatId by remember(existing?.userId) { mutableStateOf(existing?.adminPhone.orEmpty()) }
    var kadisTgChatId by remember(existing?.userId) { mutableStateOf(existing?.kadisPhone.orEmpty()) }

    var bidangExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var tgTestStatus by remember { mutableStateOf<String?>(null) }
    var isTestingTg by remember { mutableStateOf(false) }

    val botApiConfigured = remember(context) { TelegramBotService.isBotApiConfigured(context) }
    val tgAppInstalled = remember(context) { TelegramBotService.isTelegramAppInstalled(context) }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                    Icon(
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Profil ASN & Chat ID Telegram",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = currentUserEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onOpenAdminSettings,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("profile_open_admin_button")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Panel Admin")
                }
            }

            // Real Telegram Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEAF3FA)
                ),
                border = BorderStroke(1.dp, Color(0xFF2481CC))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1B619B)
                        )
                        Text(
                            text = "Status Operasional Bot Telegram (${adminConfig.botUsername})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1B619B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (botApiConfigured) {
                            "• Telegram Bot HTTP API: AKTIF (Pesan HTML, Pin Peta GPS, dan Dokumen PDF A4 terkirim otomatis ke Chat ID atasan/grup)"
                        } else {
                            "• Telegram Bot HTTP API: Mode Direct Telegram Bridge (Buka Panel Admin untuk mengisi Token @BotFather jika ingin pengiriman otomatis di latar belakang)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0F3B5F)
                    )
                    Text(
                        text = if (tgAppInstalled) {
                            "• Aplikasi Telegram Perangkat: TERDETEKSI (Mendukung kirim langsung laporan & PDF ke Telegram)"
                        } else {
                            "• Aplikasi Telegram Perangkat: Menggunakan jembatan Deep-Link t.me / Share Intent Resmi"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0F3B5F)
                    )
                }
            }

            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Telegram Chat ID / @username Configuration Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color(0xFF2481CC)
                        )
                        Text(
                            text = "Chat ID / Username Telegram Berjenjang",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Masukkan Chat ID Telegram (contoh: 123456789 atau -1001928374650) atau @username Telegram atasan Anda. Jika dikosongkan, sistem akan menggunakan Chat ID default yang diatur pada Panel Admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = myTgChatId,
                        onValueChange = { myTgChatId = it },
                        label = { Text("Chat ID / @username Telegram Anda (Pelapor)") },
                        placeholder = { Text("Contoh: 123456789 atau @asn_kominfo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_my_phone_input")
                    )

                    OutlinedTextField(
                        value = pengawasTgChatId,
                        onValueChange = { pengawasTgChatId = it },
                        label = { Text("Chat ID / @username Pejabat Pengawas / Fungsional (Tingkat 1)") },
                        placeholder = { Text("Default Admin: ${adminConfig.pengawasChatId.ifBlank { "Belum diatur" }}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_pengawas_phone_input")
                    )

                    OutlinedTextField(
                        value = adminTgChatId,
                        onValueChange = { adminTgChatId = it },
                        label = { Text("Chat ID / @username Pejabat Administrator / Kabid (Tingkat 2)") },
                        placeholder = { Text("Default Admin: ${adminConfig.adminChatId.ifBlank { "Belum diatur" }}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_admin_phone_input")
                    )

                    OutlinedTextField(
                        value = kadisTgChatId,
                        onValueChange = { kadisTgChatId = it },
                        label = { Text("Chat ID / @username Kepala Dinas Kominfo & Statistik (Tingkat 3)") },
                        placeholder = { Text("Default Admin: ${adminConfig.kadisChatId.ifBlank { "Belum diatur" }}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_kadis_phone_input")
                    )

                    if (tgTestStatus != null) {
                        Text(
                            text = tgTestStatus!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B619B)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val testTarget = pengawasTgChatId
                                .ifBlank { myTgChatId }
                                .ifBlank { adminConfig.defaultGroupChatId }
                                .ifBlank { adminConfig.pengawasChatId }
                            val testHtml = "<b>✈️ [UJI KONEKSI BOT TELEGRAM e-LAPKIN DISKOMINFOTIK]</b>\n" +
                                "Pegawai: <b>${fullName.ifBlank { "ASN Diskominfotik" }}</b>\n" +
                                "Unit Kerja: <b>$unitBidang</b>\n" +
                                "Integrasi Bot Telegram untuk pelaporan kegiatan harian berjenjang telah aktif."
                            scope.launch {
                                isTestingTg = true
                                if (TelegramBotService.isBotApiConfigured(context) && testTarget.isNotBlank()) {
                                    TelegramBotService.sendMessageViaBotApi(context, testTarget, testHtml).fold(
                                        onSuccess = { msg -> tgTestStatus = "✅ $msg" },
                                        onFailure = { err ->
                                            tgTestStatus = "⚠️ Bot API gagal (${err.localizedMessage}), membuka aplikasi Telegram..."
                                            TelegramBotService.launchRealTelegramChat(
                                                context = context,
                                                targetChatOrUsername = testTarget,
                                                plainMessageBody = testHtml.replace(Regex("<[^>]*>"), ""),
                                                botUsername = adminConfig.botUsername
                                            )
                                        }
                                    )
                                } else {
                                    tgTestStatus = "✅ Membuka aplikasi Telegram langsung..."
                                    TelegramBotService.launchRealTelegramChat(
                                        context = context,
                                        targetChatOrUsername = testTarget,
                                        plainMessageBody = testHtml.replace(Regex("<[^>]*>"), ""),
                                        botUsername = adminConfig.botUsername
                                    )
                                }
                                isTestingTg = false
                            }
                        },
                        enabled = !isTestingTg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_real_tg_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uji Kirim Pesan ke Telegram Sekarang")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Identitas Pegawai Diskominfotik", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap & Gelar *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    OutlinedTextField(
                        value = nip,
                        onValueChange = { nip = it },
                        label = { Text("NIP / NI PPPK") },
                        placeholder = { Text("Contoh: 199205142022032001") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_nip_input")
                    )

                    OutlinedTextField(
                        value = jabatanTitle,
                        onValueChange = { jabatanTitle = it },
                        label = { Text("Nama Jabatan") },
                        placeholder = { Text("Contoh: Pranata Komputer Ahli Pertama") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = bidangExpanded,
                        onExpandedChange = { bidangExpanded = !bidangExpanded }
                    ) {
                        OutlinedTextField(
                            value = unitBidang,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bidang / Sekretariat *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bidangExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = bidangExpanded,
                            onDismissRequest = { bidangExpanded = false }
                        ) {
                            DiskominfotikCatalog.units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = {
                                        unitBidang = unit.name
                                        bidangExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

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
                    Text("Tingkatan Jabatan Pelaporan Berjenjang", style = MaterialTheme.typography.titleMedium)
                    JabatanLevel.entries.forEach { level ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = jabatanLevel == level,
                                onClick = { jabatanLevel = level }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(level.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    level.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

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
                    Text("Email Atasan Verifikator (Sinkronisasi Akun)", style = MaterialTheme.typography.titleMedium)
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

            Button(
                onClick = {
                    if (fullName.trim().length < 2) {
                        errorMsg = "Nama lengkap minimal 2 karakter."
                        return@Button
                    }
                    errorMsg = null
                    onSaveProfile(
                        UserProfile(
                            fullName = fullName.trim(),
                            nip = nip.trim(),
                            jabatanLevel = jabatanLevel.name,
                            jabatanTitle = jabatanTitle.trim(),
                            unitBidang = unitBidang,
                            email = currentUserEmail,
                            pengawasEmail = pengawasEmail.trim().lowercase(),
                            adminEmail = adminEmail.trim().lowercase(),
                            kadisEmail = kadisEmail.trim().lowercase(),
                            myPhone = myTgChatId.trim(),
                            pengawasPhone = pengawasTgChatId.trim(),
                            adminPhone = adminTgChatId.trim(),
                            kadisPhone = kadisTgChatId.trim()
                        )
                    )
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Profil & Chat ID Telegram")
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 8.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun")
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
