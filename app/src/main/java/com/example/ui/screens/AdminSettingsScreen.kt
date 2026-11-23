package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityReport
import com.example.data.model.AdminBotConfig
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.ReportStatus
import com.example.data.model.UserProfile
import com.example.ui.components.GorontaloProvinceLogo
import com.example.ui.components.lapkinTextFieldColors
import com.example.ui.components.lapkinTextFieldTextStyle
import com.example.util.PdfReportGenerator
import com.example.util.TelegramBotService
import com.example.util.TelegramDiscoveredChat
import kotlinx.coroutines.launch

private val TgBlue = Color(0xFF2481CC)
private val TgDarkBlue = Color(0xFF1B619B)
private val TgLightSurface = Color(0xFFEAF3FA)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSettingsScreen(
    adminConfig: AdminBotConfig,
    profile: UserProfile?,
    allReports: List<ActivityReport>,
    currentUserEmail: String,
    currentUserDisplayName: String,
    isBusy: Boolean,
    onSaveAdminConfig: (AdminBotConfig) -> Unit,
    onAdminReviewDecision: (ActivityReport, Boolean, String, Boolean) -> Unit,
    onExportAllPdf: (List<ActivityReport>, String) -> Unit,
    onOpenReportDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var botToken by remember {
        mutableStateOf(TelegramBotService.getSavedAdminBotToken(context))
    }
    var showToken by remember { mutableStateOf(false) }

    var botUsername by remember(adminConfig.botUsername) {
        mutableStateOf(
            adminConfig.botUsername.ifBlank {
                TelegramBotService.getSavedLocalBotUsername(context)
                    .ifBlank { DiskominfotikCatalog.DEFAULT_BOT_USERNAME }
            }
        )
    }
    var defaultGroupChatId by remember(adminConfig.defaultGroupChatId) {
        mutableStateOf(
            adminConfig.defaultGroupChatId.ifBlank {
                TelegramBotService.getSavedLocalDefaultChatId(context)
            }
        )
    }
    var pengawasChatId by remember(adminConfig.pengawasChatId) {
        mutableStateOf(adminConfig.pengawasChatId)
    }
    var adminChatId by remember(adminConfig.adminChatId) {
        mutableStateOf(adminConfig.adminChatId)
    }
    var kadisChatId by remember(adminConfig.kadisChatId) {
        mutableStateOf(adminConfig.kadisChatId)
    }
    var kadisFullName by remember(adminConfig.kadisFullName) {
        mutableStateOf(
            adminConfig.kadisFullName.ifBlank { DiskominfotikCatalog.DEFAULT_KADIS_NAME }
        )
    }
    var kadisNip by remember(adminConfig.kadisNip) {
        mutableStateOf(
            adminConfig.kadisNip.ifBlank { DiskominfotikCatalog.DEFAULT_KADIS_NIP }
        )
    }
    var broadcastAnnouncement by remember(adminConfig.broadcastAnnouncement) {
        mutableStateOf(adminConfig.broadcastAnnouncement)
    }
    var requireGpsLocation by remember(adminConfig.requireGpsLocation) {
        mutableStateOf(adminConfig.requireGpsLocation)
    }
    var requirePhotoEvidence by remember(adminConfig.requirePhotoEvidence) {
        mutableStateOf(adminConfig.requirePhotoEvidence)
    }
    var autoSendPdfToTelegram by remember(adminConfig.autoSendPdfToTelegram) {
        mutableStateOf(adminConfig.autoSendPdfToTelegram)
    }
    var autoSendGpsPin by remember(adminConfig.autoSendGpsPin) {
        mutableStateOf(adminConfig.autoSendGpsPin)
    }

    var apiStatusBanner by remember { mutableStateOf<String?>(null) }
    var isCheckingBot by remember { mutableStateOf(false) }
    var discoveredChats by remember { mutableStateOf<List<TelegramDiscoveredChat>>(emptyList()) }

    val isSuperAdmin = currentUserEmail.equals(DiskominfotikCatalog.ADMIN_EMAIL, ignoreCase = true) ||
        profile?.levelEnum() == JabatanLevel.KEPALA_DINAS ||
        profile?.levelEnum() == JabatanLevel.ADMINISTRATOR

    val tgInstalled = remember(context) { TelegramBotService.isTelegramAppInstalled(context) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Admin Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TgDarkBlue)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GorontaloProvinceLogo(size = 46.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Panel Admin SIKAPDiskominfotikBot",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dinas Kominfo & Statistik Provinsi Gorontalo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isSuperAdmin) {
                                    "🛡️ Hak Akses: Administrator Utama / Pejabat Pengesah ($currentUserEmail)"
                                } else {
                                    "ℹ️ Mode Konfigurasi Perangkat ($currentUserEmail) • Ubah jabatan ke Administrator/Kepala Dinas di Profil untuk sinkronisasi global"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Text(
                                text = buildString {
                                    append(
                                        if (botToken.contains(":") && botToken.length >= 20) {
                                            "🟢 Telegram Bot HTTP API: SIAP ($botUsername)"
                                        } else {
                                            "🟡 Telegram Bot HTTP API: Belum ada Token @BotFather (Mode Jembatan Aplikasi Telegram Aktif)"
                                        }
                                    )
                                    append(" • ")
                                    append(if (tgInstalled) "Aplikasi Telegram Terdeteksi" else "Deep-Link t.me Aktif")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            if (apiStatusBanner != null) {
                Surface(
                    color = TgLightSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TgBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = apiStatusBanner!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0F3B5F),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 1. Konfigurasi Kredensial Bot Telegram (@BotFather)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = TgBlue)
                        Text(
                            text = "1. Kredensial Bot Telegram (@BotFather)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Masukkan Token Bot dari @BotFather agar aplikasi dapat mengirim laporan HTML, Pin Peta GPS (sendLocation), dan mengunggah berkas PDF Resmi A4 (sendDocument) secara otomatis ke Telegram.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Telegram Bot Token (@BotFather)") },
                        placeholder = { Text("Contoh: 1234567890:AAHxyz1234567890abcdef") },
                        singleLine = true,
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Tampilkan Token"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_bot_token_input")
                    )

                    OutlinedTextField(
                        value = botUsername,
                        onValueChange = { botUsername = it },
                        label = { Text("Username Bot Telegram") },
                        placeholder = { Text("@SIKAPDiskominfotikBot") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_bot_username_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                TelegramBotService.saveAdminLocalBotCredentials(
                                    context = context,
                                    botToken = botToken,
                                    botUsername = botUsername,
                                    defaultChatId = defaultGroupChatId
                                )
                                scope.launch {
                                    isCheckingBot = true
                                    TelegramBotService.verifyBotTokenGetMe(botToken).fold(
                                        onSuccess = { info ->
                                            botUsername = info.username
                                            TelegramBotService.saveAdminLocalBotCredentials(
                                                context = context,
                                                botToken = botToken,
                                                botUsername = info.username,
                                                defaultChatId = defaultGroupChatId
                                            )
                                            apiStatusBanner = "✅ Koneksi Bot Berhasil! Terhubung ke ${info.firstName} (${info.username} • ID: ${info.botId})"
                                        },
                                        onFailure = { err ->
                                            apiStatusBanner = "⚠️ Validasi getMe gagal: ${err.localizedMessage}"
                                        }
                                    )
                                    isCheckingBot = false
                                }
                            },
                            enabled = !isCheckingBot,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_verify_bot_button")
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cek Token (getMe)")
                        }

                        OutlinedButton(
                            onClick = {
                                TelegramBotService.saveAdminLocalBotCredentials(
                                    context = context,
                                    botToken = botToken,
                                    botUsername = botUsername,
                                    defaultChatId = defaultGroupChatId
                                )
                                scope.launch {
                                    isCheckingBot = true
                                    TelegramBotService.fetchRecentChatsFromUpdates(botToken).fold(
                                        onSuccess = { list ->
                                            discoveredChats = list
                                            apiStatusBanner = if (list.isEmpty()) {
                                                "ℹ️ Belum ada chat masuk pada getUpdates. Kirim pesan /start ke $botUsername dari Telegram Anda lalu ketuk tombol ini lagi."
                                            } else {
                                                "✅ Ditemukan ${list.size} Chat ID dari pesan masuk ke Bot! Ketuk salah satu di bawah untuk mengisi Chat ID otomatis."
                                            }
                                        },
                                        onFailure = { err ->
                                            apiStatusBanner = "⚠️ Gagal memindai Chat ID (getUpdates): ${err.localizedMessage}"
                                        }
                                    )
                                    isCheckingBot = false
                                }
                            },
                            enabled = !isCheckingBot,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_scan_updates_button")
                        ) {
                            Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deteksi Chat ID")
                        }
                    }

                    if (discoveredChats.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Chat ID Terdeteksi dari Telegram (getUpdates):",
                            style = MaterialTheme.typography.labelLarge,
                            color = TgDarkBlue
                        )
                        discoveredChats.forEach { chat ->
                            Surface(
                                color = TgLightSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${chat.title} ${chat.username} • ID: ${chat.chatId} (${chat.type})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF0F3B5F)
                                    )
                                    if (chat.lastText.isNotBlank()) {
                                        Text(
                                            text = "Pesan terakhir: \"${chat.lastText.take(60)}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AssistChip(
                                            onClick = { defaultGroupChatId = chat.chatId },
                                            label = { Text("Set Grup Dinas") }
                                        )
                                        AssistChip(
                                            onClick = { pengawasChatId = chat.chatId },
                                            label = { Text("Set Pengawas") }
                                        )
                                        AssistChip(
                                            onClick = { adminChatId = chat.chatId },
                                            label = { Text("Set Admin") }
                                        )
                                        AssistChip(
                                            onClick = { kadisChatId = chat.chatId },
                                            label = { Text("Set Kadis") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Routing Chat ID Berjenjang & Grup Monitoring Dinas
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
                    Text(
                        text = "2. Routing Chat ID Telegram Berjenjang (Default Dinas)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Atur Chat ID Telegram (contoh: 123456789, -1001928374650, atau @username) untuk Grup Monitoring Dinas dan Pejabat Verifikator Tingkat 1 hingga Kepala Dinas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = defaultGroupChatId,
                        onValueChange = { defaultGroupChatId = it },
                        label = { Text("Chat ID Grup / Channel Monitoring Diskominfotik") },
                        placeholder = { Text("Contoh: -1001928374650 atau @lapkin_gorontalo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_group_chat_id_input")
                    )

                    OutlinedTextField(
                        value = pengawasChatId,
                        onValueChange = { pengawasChatId = it },
                        label = { Text("Chat ID Default Pejabat Pengawas / Fungsional (Pengesah PPPK/Pelaksana)") },
                        placeholder = { Text("Contoh: 112233445 atau @pengawas_kominfo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminChatId,
                        onValueChange = { adminChatId = it },
                        label = { Text("Chat ID Default Pejabat Administrator / Kabid (Pengesah Pengawas/JF)") },
                        placeholder = { Text("Contoh: 223344556 atau @kabid_tik_gorontalo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = kadisChatId,
                        onValueChange = { kadisChatId = it },
                        label = { Text("Chat ID Default Kepala Dinas Kominfo & Statistik (Pengesah Akhir)") },
                        placeholder = { Text("Contoh: 334455667 atau @kadis_kominfo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        onClick = {
                            TelegramBotService.saveAdminLocalBotCredentials(
                                context = context,
                                botToken = botToken,
                                botUsername = botUsername,
                                defaultChatId = defaultGroupChatId
                            )
                            val targetChat = defaultGroupChatId
                                .ifBlank { pengawasChatId }
                                .ifBlank { adminChatId }
                                .ifBlank { kadisChatId }
                            val testHtml = "<b>✈️ [UJI KONFIGURASI ADMIN BOT TELEGRAM e-LAPKIN]</b>\n" +
                                "Instansi: <b>Dinas Kominfo &amp; Statistik Provinsi Gorontalo</b>\n" +
                                "Admin: <b>${currentUserDisplayName.ifBlank { currentUserEmail }}</b>\n" +
                                "Bot Handle: <code>$botUsername</code>\n" +
                                "Status: <b>Integrasi Pelaporan Berjenjang &amp; PDF A4 Aktif</b>"
                            scope.launch {
                                isCheckingBot = true
                                if (botToken.contains(":") && targetChat.isNotBlank()) {
                                    TelegramBotService.sendMessageViaBotApi(
                                        context = context,
                                        chatId = targetChat,
                                        htmlText = testHtml,
                                        overrideToken = botToken
                                    ).fold(
                                        onSuccess = { msg -> apiStatusBanner = "✅ $msg" },
                                        onFailure = { err ->
                                            apiStatusBanner = "⚠️ Bot API gagal (${err.localizedMessage}), membuka aplikasi Telegram langsung..."
                                            TelegramBotService.launchRealTelegramChat(
                                                context = context,
                                                targetChatOrUsername = targetChat,
                                                plainMessageBody = testHtml.replace(Regex("<[^>]*>"), ""),
                                                botUsername = botUsername
                                            )
                                        }
                                    )
                                } else {
                                    apiStatusBanner = "✅ Membuka aplikasi Telegram langsung..."
                                    TelegramBotService.launchRealTelegramChat(
                                        context = context,
                                        targetChatOrUsername = targetChat,
                                        plainMessageBody = testHtml.replace(Regex("<[^>]*>"), ""),
                                        botUsername = botUsername
                                    )
                                }
                                isCheckingBot = false
                            }
                        },
                        enabled = !isCheckingBot,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_test_telegram_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uji Kirim Pesan Admin ke Telegram Sekarang")
                    }
                }
            }

            // 3. Kebijakan Pelaporan & Penandatangan PDF Resmi
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
                    Text(
                        text = "3. Pejabat Pengesah PDF & Kebijakan Pelaporan ASN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = kadisFullName,
                        onValueChange = { kadisFullName = it },
                        label = { Text("Nama Lengkap & Gelar Kepala Dinas (Cetakan PDF A4)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_kadis_name_input")
                    )

                    OutlinedTextField(
                        value = kadisNip,
                        onValueChange = { kadisNip = it },
                        label = { Text("NIP Kepala Dinas (Cetakan PDF A4)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_kadis_nip_input")
                    )

                    HorizontalDivider()

                    PolicyToggleRow(
                        title = "Otomatis Unggah Berkas PDF A4 ke Telegram",
                        subtitle = "Setiap laporan yang dikirim melalui Bot otomatis men-generate PDF ber-Kop Resmi dan mengirimkannya via sendDocument",
                        checked = autoSendPdfToTelegram,
                        onCheckedChange = { autoSendPdfToTelegram = it }
                    )

                    PolicyToggleRow(
                        title = "Otomatis Kirim Pin Peta Lokasi GPS ke Telegram",
                        subtitle = "Kirim koordinat lintang/bujur GPS sebagai titik lokasi interaktif (sendLocation) di chat Telegram",
                        checked = autoSendGpsPin,
                        onCheckedChange = { autoSendGpsPin = it }
                    )

                    PolicyToggleRow(
                        title = "Wajibkan Titik Koordinat GPS Valid",
                        subtitle = "ASN wajib mengunci sinyal GPS sebelum laporan dapat dikirim ke Pejabat Pengawas",
                        checked = requireGpsLocation,
                        onCheckedChange = { requireGpsLocation = it }
                    )

                    PolicyToggleRow(
                        title = "Wajibkan Bukti Foto Kegiatan",
                        subtitle = "ASN wajib melampirkan foto kamera/galeri sebagai bukti fisik pelaksanaan kegiatan",
                        checked = requirePhotoEvidence,
                        onCheckedChange = { requirePhotoEvidence = it }
                    )
                }
            }

            // 4. Siaran Pengumuman Admin (Broadcast ke Bot & Grup Telegram)
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
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = TgBlue)
                        Text(
                            text = "4. Siaran Pengumuman Pimpinan / Admin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = broadcastAnnouncement,
                        onValueChange = { broadcastAnnouncement = it },
                        label = { Text("Teks Pengumuman Resmi di Banner Bot Telegram") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = lapkinTextFieldColors(),
                        textStyle = lapkinTextFieldTextStyle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val targetChat = defaultGroupChatId
                                .ifBlank { pengawasChatId }
                                .ifBlank { adminChatId }
                                .ifBlank { kadisChatId }
                            val htmlBroadcast = "<b>📢 [PENGUMUMAN RESMI DISKOMINFOTIK PROV. GORONTALO]</b>\n" +
                                "──────────────────────────────\n" +
                                "${broadcastAnnouncement.ifBlank { "Harap seluruh ASN segera melengkapi laporan pelaksanaan kegiatan harian." }}\n" +
                                "──────────────────────────────\n" +
                                "<i>Diterbitkan oleh Admin e-Lapkin (${currentUserDisplayName.ifBlank { currentUserEmail }})</i>"
                            scope.launch {
                                if (botToken.contains(":") && targetChat.isNotBlank()) {
                                    TelegramBotService.sendMessageViaBotApi(
                                        context = context,
                                        chatId = targetChat,
                                        htmlText = htmlBroadcast,
                                        overrideToken = botToken
                                    ).fold(
                                        onSuccess = { msg -> apiStatusBanner = "📢 Pengumuman berhasil disiarkan: $msg" },
                                        onFailure = {
                                            TelegramBotService.launchRealTelegramChat(
                                                context = context,
                                                targetChatOrUsername = targetChat,
                                                plainMessageBody = htmlBroadcast.replace(Regex("<[^>]*>"), ""),
                                                botUsername = botUsername
                                            )
                                        }
                                    )
                                } else {
                                    TelegramBotService.launchRealTelegramChat(
                                        context = context,
                                        targetChatOrUsername = targetChat,
                                        plainMessageBody = htmlBroadcast.replace(Regex("<[^>]*>"), ""),
                                        botUsername = botUsername
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Siarkan Pengumuman ke Grup Telegram Sekarang")
                    }
                }
            }

            // Save All Admin Configuration CTA
            Button(
                onClick = {
                    TelegramBotService.saveAdminLocalBotCredentials(
                        context = context,
                        botToken = botToken,
                        botUsername = botUsername,
                        defaultChatId = defaultGroupChatId
                    )
                    val updatedConfig = AdminBotConfig(
                        botUsername = botUsername.trim().ifBlank { DiskominfotikCatalog.DEFAULT_BOT_USERNAME },
                        defaultGroupChatId = defaultGroupChatId.trim(),
                        pengawasChatId = pengawasChatId.trim(),
                        adminChatId = adminChatId.trim(),
                        kadisChatId = kadisChatId.trim(),
                        kadisFullName = kadisFullName.trim().ifBlank { DiskominfotikCatalog.DEFAULT_KADIS_NAME },
                        kadisNip = kadisNip.trim(),
                        broadcastAnnouncement = broadcastAnnouncement.trim(),
                        requireGpsLocation = requireGpsLocation,
                        requirePhotoEvidence = requirePhotoEvidence,
                        autoSendPdfToTelegram = autoSendPdfToTelegram,
                        autoSendGpsPin = autoSendGpsPin,
                        updatedByEmail = currentUserEmail
                    )
                    onSaveAdminConfig(updatedConfig)
                    apiStatusBanner = "✅ Pengaturan Admin Bot Telegram & Token @BotFather berhasil disimpan!"
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = TgBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_save_config_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Seluruh Pengaturan Admin Bot Telegram")
                }
            }

            // 5. Konsol Supervisi & Pengesahan Admin (Seluruh Laporan)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.FactCheck, contentDescription = null, tint = TgBlue)
                            Text(
                                text = "5. Konsol Supervisi Laporan (${allReports.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onExportAllPdf(allReports, "Rekap Supervisi Admin Diskominfotik")
                            },
                            enabled = allReports.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rekap PDF", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    val pendingCount = allReports.count {
                        val st = it.statusEnum()
                        st == ReportStatus.SUBMITTED_PENGAWAS ||
                            st == ReportStatus.SUBMITTED_ADMINISTRATOR ||
                            st == ReportStatus.SUBMITTED_KADIS
                    }
                    val approvedCount = allReports.count { it.statusEnum() == ReportStatus.APPROVED_FINAL }
                    val revisionCount = allReports.count { it.statusEnum() == ReportStatus.REVISION }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminStatPill("Total", "${allReports.size}", TgLightSurface, TgDarkBlue, Modifier.weight(1f))
                        AdminStatPill("Antrean", "$pendingCount", Color(0xFFFFF4E5), Color(0xFF9A5B00), Modifier.weight(1f))
                        AdminStatPill("Revisi", "$revisionCount", Color(0xFFFDE8E8), Color(0xFFB42318), Modifier.weight(1f))
                        AdminStatPill("Disahkan", "$approvedCount", Color(0xFFE6F4EA), Color(0xFF0D652D), Modifier.weight(1f))
                    }

                    if (allReports.isEmpty()) {
                        Text(
                            text = "Belum ada laporan aktivitas harian yang masuk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        allReports.take(8).forEach { rep ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenReportDetail(rep.id) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${rep.authorName} • ${rep.activityDate}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TgDarkBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = rep.approvalStatusLabel(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rep.statusEnum() == ReportStatus.APPROVED_FINAL) {
                                                Color(0xFF0D652D)
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                    Text(
                                        text = "[${rep.classificationEnum().shortLabel}] ${rep.summary}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${rep.unitBidang} • ${rep.subKegiatan}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (rep.statusEnum() != ReportStatus.APPROVED_FINAL) {
                                            Button(
                                                onClick = {
                                                    onAdminReviewDecision(
                                                        rep,
                                                        true,
                                                        "Disahkan melalui Panel Admin Bot Telegram oleh $kadisFullName",
                                                        true
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D652D)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sahkan Final", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    PdfReportGenerator.generatePdf(
                                                        context = context,
                                                        profile = profile,
                                                        reports = listOf(rep),
                                                        periodLabel = "Laporan Resmi e-Lapkin",
                                                        adminConfig = adminConfig
                                                    ).onSuccess { pdfFile ->
                                                        val res = TelegramBotService.dispatchPdfToTelegram(
                                                            context = context,
                                                            pdfFile = pdfFile,
                                                            report = rep,
                                                            profile = profile,
                                                            adminConfig = adminConfig
                                                        )
                                                        apiStatusBanner = "📤 ${res.message}"
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF ke Telegram", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun PolicyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AdminStatPill(
    label: String,
    value: String,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor
            )
        }
    }
}
