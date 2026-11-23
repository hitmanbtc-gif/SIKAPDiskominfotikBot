package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.model.ActivityReport
import com.example.data.model.AdminBotConfig
import com.example.data.model.DiskominfotikCatalog
import com.example.data.model.JabatanLevel
import com.example.data.model.ReportStatus
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TelegramBotInfo(
    val botId: Long,
    val firstName: String,
    val username: String
)

data class TelegramDiscoveredChat(
    val chatId: String,
    val title: String,
    val username: String,
    val type: String,
    val lastText: String
)

data class TelegramDispatchResult(
    val usedBotApi: Boolean,
    val targetChatIdOrUsername: String,
    val targetRoleLabel: String,
    val message: String
)

object TelegramBotService {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val PREFS_NAME = "telegram_bot_admin_prefs"
    private const val KEY_BOT_TOKEN = "override_bot_token"
    private const val KEY_BOT_USERNAME = "override_bot_username"
    private const val KEY_DEFAULT_CHAT_ID = "override_default_chat_id"

    private const val PLACEHOLDER_TOKEN = "YOUR_TELEGRAM_BOT_TOKEN"
    private const val PLACEHOLDER_CHAT_ID = "YOUR_TELEGRAM_DEFAULT_CHAT_ID"

    private val TELEGRAM_PACKAGES = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram"
    )

    fun getSavedAdminBotToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val local = prefs.getString(KEY_BOT_TOKEN, "").orEmpty().trim()
        if (local.isNotBlank()) return local
        val envToken = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        return if (envToken.isNotBlank() && envToken != PLACEHOLDER_TOKEN) envToken else ""
    }

    fun saveAdminLocalBotCredentials(
        context: Context,
        botToken: String,
        botUsername: String,
        defaultChatId: String
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOT_TOKEN, botToken.trim())
            .putString(KEY_BOT_USERNAME, botUsername.trim())
            .putString(KEY_DEFAULT_CHAT_ID, defaultChatId.trim())
            .apply()
    }

    fun getSavedLocalBotUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BOT_USERNAME, "").orEmpty().trim()
    }

    fun getSavedLocalDefaultChatId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val local = prefs.getString(KEY_DEFAULT_CHAT_ID, "").orEmpty().trim()
        if (local.isNotBlank()) return local
        val envChat = BuildConfig.TELEGRAM_DEFAULT_CHAT_ID.trim()
        return if (envChat.isNotBlank() && envChat != PLACEHOLDER_CHAT_ID) envChat else ""
    }

    fun isBotApiConfigured(context: Context): Boolean {
        val token = getSavedAdminBotToken(context)
        return token.contains(":") && token.length >= 20
    }

    fun isTelegramAppInstalled(context: Context): Boolean {
        return resolveTelegramPackage(context) != null
    }

    fun resolveTelegramPackage(context: Context): String? {
        val pm = context.packageManager
        return TELEGRAM_PACKAGES.firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    fun resolveNextReviewerTarget(
        context: Context,
        report: ActivityReport,
        profile: UserProfile?,
        adminConfig: AdminBotConfig?
    ): Pair<String, String> {
        val fallbackGroup = adminConfig?.defaultGroupChatId?.takeIf { it.isNotBlank() }
            ?: getSavedLocalDefaultChatId(context)

        val st = report.statusEnum()
        return when (st) {
            ReportStatus.SUBMITTED_PENGAWAS -> {
                val target = profile?.pengawasPhone?.takeIf { it.isNotBlank() }
                    ?: adminConfig?.pengawasChatId?.takeIf { it.isNotBlank() }
                    ?: fallbackGroup
                target to "Pejabat Pengawas / Fungsional"
            }
            ReportStatus.SUBMITTED_ADMINISTRATOR -> {
                val target = profile?.adminPhone?.takeIf { it.isNotBlank() }
                    ?: adminConfig?.adminChatId?.takeIf { it.isNotBlank() }
                    ?: fallbackGroup
                target to "Pejabat Administrator (Kabid/Sekretaris)"
            }
            ReportStatus.SUBMITTED_KADIS -> {
                val target = profile?.kadisPhone?.takeIf { it.isNotBlank() }
                    ?: adminConfig?.kadisChatId?.takeIf { it.isNotBlank() }
                    ?: fallbackGroup
                target to "Kepala Dinas Kominfo & Statistik"
            }
            else -> {
                val tier = profile?.levelEnum() ?: JabatanLevel.PELAKSANA
                when (tier) {
                    JabatanLevel.PELAKSANA -> {
                        val target = profile?.pengawasPhone?.takeIf { it.isNotBlank() }
                            ?: adminConfig?.pengawasChatId?.takeIf { it.isNotBlank() }
                            ?: fallbackGroup
                        target to "Pejabat Pengawas / Fungsional"
                    }
                    JabatanLevel.PENGAWAS_FUNGSIONAL -> {
                        val target = profile?.adminPhone?.takeIf { it.isNotBlank() }
                            ?: adminConfig?.adminChatId?.takeIf { it.isNotBlank() }
                            ?: fallbackGroup
                        target to "Pejabat Administrator"
                    }
                    JabatanLevel.ADMINISTRATOR, JabatanLevel.KEPALA_DINAS -> {
                        val target = profile?.kadisPhone?.takeIf { it.isNotBlank() }
                            ?: adminConfig?.kadisChatId?.takeIf { it.isNotBlank() }
                            ?: fallbackGroup
                        target to "Kepala Dinas"
                    }
                }
            }
        }
    }

    private fun escapeHtml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    fun formatTelegramHtmlReport(
        report: ActivityReport,
        profile: UserProfile?,
        targetRoleLabel: String,
        botUsername: String = DiskominfotikCatalog.DEFAULT_BOT_USERNAME
    ): String {
        val nip = report.authorNip.ifBlank { profile?.nip.orEmpty() }.ifBlank { "-" }
        val jabatan = report.authorJabatan.ifBlank { profile?.jabatanTitle.orEmpty() }
            .ifBlank { report.authorTierEnum().label }
        val gpsHtml = if (report.gpsCaptured) {
            val mapsUrl = String.format(
                Locale.US,
                "https://maps.google.com/?q=%.6f,%.6f",
                report.gpsLatitude,
                report.gpsLongitude
            )
            String.format(
                Locale.US,
                "<code>%.6f, %.6f</code> (±%.0fm)\n🗺️ <a href=\"%s\">Buka Titik Lokasi di Google Maps</a>",
                report.gpsLatitude,
                report.gpsLongitude,
                report.gpsAccuracyMeters,
                mapsUrl
            )
        } else {
            "Tidak dilampirkan"
        }

        return buildString {
            append("<b>✈️ [BOT TELEGRAM SIKAP DISKOMINFOTIK PROV. GORONTALO]</b>\n")
            append("Kepada Yth. <b>${escapeHtml(targetRoleLabel)}</b>\n")
            append("──────────────────────────────\n")
            append("👤 <b>Pegawai</b>: ${escapeHtml(report.authorName)}\n")
            append("🆔 <b>NIP/NI PPPK</b>: <code>${escapeHtml(nip)}</code>\n")
            append("🏛️ <b>Jabatan</b>: ${escapeHtml(jabatan)}\n")
            append("🏢 <b>Unit Kerja</b>: ${escapeHtml(report.unitBidang)}\n")
            append("📅 <b>Waktu</b>: ${escapeHtml(report.activityDate)} (${escapeHtml(report.startTime)} - ${escapeHtml(report.endTime)} WITA)\n")
            append("🏷️ <b>Klasifikasi</b>: <b>${escapeHtml(report.classificationEnum().label)}</b>\n")
            append("🔗 <b>Sub Kegiatan</b>: ${escapeHtml(report.subKegiatan)}\n")
            append("──────────────────────────────\n")
            append("📝 <b>Ringkasan Kegiatan</b>:\n${escapeHtml(report.summary)}\n\n")
            append("📌 <b>Uraian &amp; Output</b>:\n${escapeHtml(report.description.ifBlank { "-" })}\n\n")
            append("📍 <b>Lokasi</b>: ${escapeHtml(report.locationName)}\n")
            append("🛰️ <b>Koordinat GPS</b>: $gpsHtml\n")
            if (report.gpsAddress.isNotBlank()) {
                append("🏠 <b>Alamat GPS</b>: ${escapeHtml(report.gpsAddress)}\n")
            }
            append("📷 <b>Bukti Foto</b>: ${if (report.photoBase64.isNotBlank()) "Terlampir di Sistem &amp; PDF" else "Tidak ada"}\n")
            append("⏳ <b>Status Berjenjang</b>: <b>${escapeHtml(report.statusEnum().label)}</b>\n")
            append("──────────────────────────────\n")
            append("<i>Dikirim otomatis via Sistem Bot Telegram ${escapeHtml(botUsername)} Dinas Kominfo &amp; Statistik Provinsi Gorontalo.</i>")
        }
    }

    fun formatTelegramPlainTextReport(
        report: ActivityReport,
        profile: UserProfile?,
        targetRoleLabel: String,
        botUsername: String = DiskominfotikCatalog.DEFAULT_BOT_USERNAME
    ): String {
        val nip = report.authorNip.ifBlank { profile?.nip.orEmpty() }.ifBlank { "-" }
        val jabatan = report.authorJabatan.ifBlank { profile?.jabatanTitle.orEmpty() }
            .ifBlank { report.authorTierEnum().label }
        val gpsLine = if (report.gpsCaptured) {
            val mapsUrl = String.format(
                Locale.US,
                "https://maps.google.com/?q=%.6f,%.6f",
                report.gpsLatitude,
                report.gpsLongitude
            )
            String.format(
                Locale.US,
                "%.6f, %.6f (±%.0fm)\n🗺️ Maps: %s",
                report.gpsLatitude,
                report.gpsLongitude,
                report.gpsAccuracyMeters,
                mapsUrl
            )
        } else {
            "Tidak dilampirkan"
        }

        return buildString {
            append("✈️ [BOT TELEGRAM SIKAP DISKOMINFOTIK PROV. GORONTALO]\n")
            append("Kepada Yth. $targetRoleLabel\n")
            append("──────────────────────────────\n")
            append("👤 Pegawai: ${report.authorName}\n")
            append("🆔 NIP/NI PPPK: $nip\n")
            append("🏛️ Jabatan: $jabatan\n")
            append("🏢 Unit Kerja: ${report.unitBidang}\n")
            append("📅 Waktu: ${report.activityDate} (${report.startTime} - ${report.endTime} WITA)\n")
            append("🏷️ Klasifikasi: ${report.classificationEnum().label}\n")
            append("🔗 Sub Kegiatan: ${report.subKegiatan}\n")
            append("──────────────────────────────\n")
            append("📝 Ringkasan Kegiatan:\n${report.summary}\n\n")
            append("📌 Uraian & Output:\n${report.description.ifBlank { "-" }}\n\n")
            append("📍 Lokasi: ${report.locationName}\n")
            append("🛰️ Koordinat GPS: $gpsLine\n")
            if (report.gpsAddress.isNotBlank()) {
                append("🏠 Alamat GPS: ${report.gpsAddress}\n")
            }
            append("📷 Bukti Foto: ${if (report.photoBase64.isNotBlank()) "Terlampir di Sistem & PDF" else "Tidak ada"}\n")
            append("⏳ Status Berjenjang: ${report.statusEnum().label}\n")
            append("──────────────────────────────\n")
            append("Dikirim melalui Bot Telegram $botUsername Dinas Kominfo & Statistik Provinsi Gorontalo.")
        }
    }

    /**
     * Calls GET https://api.telegram.org/bot<token>/getMe to verify the BotFather token live.
     */
    suspend fun verifyBotTokenGetMe(rawToken: String): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        try {
            val token = rawToken.trim()
            require(token.contains(":") && token.length >= 20) {
                "Format Token Bot Telegram tidak valid. Salin token dari @BotFather (contoh: 123456789:AAH...)."
            }
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/getMe")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Telegram API HTTP ${response.code}: ${bodyStr.take(160)}")
                    )
                }
                val json = JSONObject(bodyStr)
                if (!json.optBoolean("ok", false)) {
                    return@withContext Result.failure(
                        IllegalStateException(json.optString("description", "Token ditolak oleh Telegram"))
                    )
                }
                val res = json.getJSONObject("result")
                val info = TelegramBotInfo(
                    botId = res.optLong("id", 0L),
                    firstName = res.optString("first_name", "Telegram Bot"),
                    username = "@" + res.optString("username", "bot").removePrefix("@")
                )
                Result.success(info)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calls GET https://api.telegram.org/bot<token>/getUpdates to discover Chat IDs of users/groups
     * who have sent /start or messages to the bot.
     */
    suspend fun fetchRecentChatsFromUpdates(rawToken: String): Result<List<TelegramDiscoveredChat>> =
        withContext(Dispatchers.IO) {
            try {
                val token = rawToken.trim()
                require(token.contains(":") && token.length >= 20) {
                    "Masukkan Token Bot Telegram (@BotFather) terlebih dahulu."
                }
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$token/getUpdates?limit=30")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Telegram getUpdates HTTP ${response.code}: ${bodyStr.take(160)}")
                        )
                    }
                    val json = JSONObject(bodyStr)
                    if (!json.optBoolean("ok", false)) {
                        return@withContext Result.failure(
                            IllegalStateException(json.optString("description", "Gagal mengambil getUpdates"))
                        )
                    }
                    val arr = json.optJSONArray("result")
                    val discovered = linkedMapOf<String, TelegramDiscoveredChat>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val msg = item.optJSONObject("message")
                                ?: item.optJSONObject("my_chat_member")
                                ?: item.optJSONObject("channel_post")
                                ?: continue
                            val chat = msg.optJSONObject("chat") ?: continue
                            val chatId = chat.optLong("id", 0L).toString()
                            if (chatId == "0") continue
                            val title = chat.optString("title").ifBlank {
                                (chat.optString("first_name") + " " + chat.optString("last_name")).trim()
                            }.ifBlank { "Chat Telegram" }
                            val username = chat.optString("username").let {
                                if (it.isNotBlank()) "@$it" else ""
                            }
                            val type = chat.optString("type", "private")
                            val lastText = msg.optString("text", "")
                            discovered[chatId] = TelegramDiscoveredChat(
                                chatId = chatId,
                                title = title,
                                username = username,
                                type = type,
                                lastText = lastText
                            )
                        }
                    }
                    Result.success(discovered.values.toList().reversed())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Sends an HTML-formatted message directly via Telegram Bot API (`POST /sendMessage`).
     */
    suspend fun sendMessageViaBotApi(
        context: Context,
        chatId: String,
        htmlText: String,
        overrideToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = overrideToken?.takeIf { it.isNotBlank() } ?: getSavedAdminBotToken(context)
            require(token.contains(":") && token.length >= 20) {
                "Token Bot Telegram belum diatur. Buka tab Admin untuk mengisi Token @BotFather."
            }
            val cleanChatId = chatId.trim()
            require(cleanChatId.isNotBlank()) {
                "Chat ID / Username Channel Telegram tujuan belum diisi."
            }

            val payload = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("text", htmlText)
                put("parse_mode", "HTML")
                put("disable_web_page_preview", false)
            }

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendMessage")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Terkirim otomatis via Telegram Bot API ke Chat ID $cleanChatId")
                } else {
                    Result.failure(
                        IllegalStateException("Telegram API HTTP ${response.code}: ${respBody.take(180)}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a live GPS map pin via Telegram Bot API (`POST /sendLocation`).
     */
    suspend fun sendLocationViaBotApi(
        context: Context,
        chatId: String,
        latitude: Double,
        longitude: Double,
        overrideToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = overrideToken?.takeIf { it.isNotBlank() } ?: getSavedAdminBotToken(context)
            val cleanChatId = chatId.trim()
            if (!token.contains(":") || cleanChatId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Token atau Chat ID kosong"))
            }

            val payload = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("latitude", latitude)
                put("longitude", longitude)
            }

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendLocation")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Pin Lokasi GPS terkirim ke Telegram ($cleanChatId)")
                } else {
                    Result.failure(IllegalStateException("sendLocation HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads and sends the generated official A4 PDF file directly to a Telegram chat
     * via `POST https://api.telegram.org/bot<token>/sendDocument` (multipart/form-data).
     */
    suspend fun sendPdfDocumentViaBotApi(
        context: Context,
        chatId: String,
        pdfFile: File,
        captionText: String,
        overrideToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = overrideToken?.takeIf { it.isNotBlank() } ?: getSavedAdminBotToken(context)
            require(token.contains(":") && token.length >= 20) {
                "Token Bot Telegram belum dikonfigurasi."
            }
            val cleanChatId = chatId.trim()
            require(cleanChatId.isNotBlank()) {
                "Chat ID tujuan belum diisi."
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", cleanChatId)
                .addFormDataPart("caption", captionText.take(1000))
                .addFormDataPart(
                    "document",
                    pdfFile.name,
                    pdfFile.asRequestBody("application/pdf".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendDocument")
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Dokumen PDF Resmi (${pdfFile.name}) berhasil diunggah ke Telegram ($cleanChatId)")
                } else {
                    Result.failure(
                        IllegalStateException("Telegram sendDocument HTTP ${response.code}: ${respBody.take(180)}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the installed Telegram app (or t.me deep link) directly with the formatted report text.
     */
    fun launchRealTelegramChat(
        context: Context,
        targetChatOrUsername: String,
        plainMessageBody: String,
        botUsername: String = DiskominfotikCatalog.DEFAULT_BOT_USERNAME
    ) {
        val encodedText = URLEncoder.encode(plainMessageBody, "UTF-8")
        val encodedUrl = URLEncoder.encode("https://gorontaloprov.go.id", "UTF-8")
        val cleanTarget = targetChatOrUsername.trim()
        val tgPkg = resolveTelegramPackage(context)

        val deepLinkUri = when {
            cleanTarget.startsWith("@") && cleanTarget.length > 2 -> {
                val domain = cleanTarget.removePrefix("@")
                Uri.parse("https://t.me/$domain?text=$encodedText")
            }
            cleanTarget.any { it.isLetter() } -> {
                Uri.parse("https://t.me/$cleanTarget?text=$encodedText")
            }
            else -> {
                Uri.parse("https://t.me/share/url?url=$encodedUrl&text=$encodedText")
            }
        }

        val viewIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tgPkg != null) {
                setPackage(tgPkg)
            }
        }

        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            val fallbackBrowser = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://t.me/share/url?url=$encodedUrl&text=$encodedText")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackBrowser)
            } catch (_: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, plainMessageBody)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Kirim Laporan ke Telegram ($botUsername)")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    /**
     * Opens the installed Telegram client directly with the generated A4 PDF file attached.
     */
    fun sendPdfFileToRealTelegramApp(
        context: Context,
        pdfFile: File,
        captionText: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val tgPkg = resolveTelegramPackage(context)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, captionText)
            if (tgPkg != null) {
                setPackage(tgPkg)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(sendIntent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, captionText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Kirim Dokumen PDF ke Telegram"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    /**
     * Dispatches an ASN activity report to Telegram:
     * 1. If a valid Telegram Bot Token and Chat ID exist, sends HTML report + GPS pin directly via Bot API.
     * 2. Also mirrors to the Admin's `defaultGroupChatId` if configured and different from the personal target.
     * 3. Falls back to launching the installed Telegram app / t.me deep link if Bot API is not configured.
     */
    suspend fun dispatchReportToTelegram(
        context: Context,
        report: ActivityReport,
        profile: UserProfile?,
        adminConfig: AdminBotConfig?,
        forceOpenTelegramApp: Boolean = false
    ): TelegramDispatchResult {
        val (targetChat, roleLabel) = resolveNextReviewerTarget(context, report, profile, adminConfig)
        val botHandle = adminConfig?.botUsername?.ifBlank { DiskominfotikCatalog.DEFAULT_BOT_USERNAME }
            ?: getSavedLocalBotUsername(context).ifBlank { DiskominfotikCatalog.DEFAULT_BOT_USERNAME }

        val htmlBody = formatTelegramHtmlReport(report, profile, roleLabel, botHandle)
        val plainBody = formatTelegramPlainTextReport(report, profile, roleLabel, botHandle)

        val canUseBotApi = !forceOpenTelegramApp &&
            isBotApiConfigured(context) &&
            targetChat.isNotBlank() &&
            (targetChat.startsWith("-") || targetChat.all { it.isDigit() } || targetChat.startsWith("@"))

        if (canUseBotApi) {
            val sendRes = sendMessageViaBotApi(context, targetChat, htmlBody)
            if (sendRes.isSuccess) {
                if (report.gpsCaptured && (adminConfig?.autoSendGpsPin != false)) {
                    sendLocationViaBotApi(context, targetChat, report.gpsLatitude, report.gpsLongitude)
                }
                val groupChat = adminConfig?.defaultGroupChatId?.trim().orEmpty()
                if (groupChat.isNotBlank() && groupChat != targetChat) {
                    sendMessageViaBotApi(context, groupChat, htmlBody)
                }
                return TelegramDispatchResult(
                    usedBotApi = true,
                    targetChatIdOrUsername = targetChat,
                    targetRoleLabel = roleLabel,
                    message = sendRes.getOrThrow()
                )
            }
        }

        withContext(Dispatchers.Main) {
            launchRealTelegramChat(
                context = context,
                targetChatOrUsername = targetChat,
                plainMessageBody = plainBody,
                botUsername = botHandle
            )
        }

        val targetInfo = if (targetChat.isNotBlank()) "$targetChat ($roleLabel)" else roleLabel
        return TelegramDispatchResult(
            usedBotApi = false,
            targetChatIdOrUsername = targetChat,
            targetRoleLabel = roleLabel,
            message = "Membuka aplikasi Telegram langsung menuju $targetInfo"
        )
    }

    /**
     * Dispatches the generated A4 PDF report to Telegram:
     * 1. If Bot API Token & Chat ID are configured, uploads the PDF via `POST /sendDocument`.
     * 2. Otherwise opens the installed Telegram app with the PDF attached via FileProvider.
     */
    suspend fun dispatchPdfToTelegram(
        context: Context,
        pdfFile: File,
        report: ActivityReport,
        profile: UserProfile?,
        adminConfig: AdminBotConfig?,
        forceOpenTelegramApp: Boolean = false
    ): TelegramDispatchResult {
        val (targetChat, roleLabel) = resolveNextReviewerTarget(context, report, profile, adminConfig)
        val caption = "📄 [DOKUMEN PDF RESMI SIKAP DISKOMINFOTIK PROV. GORONTALO]\n" +
            "Pegawai: ${report.authorName}\n" +
            "Klasifikasi: ${report.classificationEnum().label}\n" +
            "Ringkasan: ${report.summary}\n" +
            "Tujuan Berjenjang: $roleLabel"

        val canUseBotApi = !forceOpenTelegramApp &&
            isBotApiConfigured(context) &&
            targetChat.isNotBlank() &&
            (targetChat.startsWith("-") || targetChat.all { it.isDigit() } || targetChat.startsWith("@"))

        if (canUseBotApi) {
            val docRes = sendPdfDocumentViaBotApi(context, targetChat, pdfFile, caption)
            if (docRes.isSuccess) {
                return TelegramDispatchResult(
                    usedBotApi = true,
                    targetChatIdOrUsername = targetChat,
                    targetRoleLabel = roleLabel,
                    message = docRes.getOrThrow()
                )
            }
        }

        withContext(Dispatchers.Main) {
            sendPdfFileToRealTelegramApp(context, pdfFile, caption)
        }

        return TelegramDispatchResult(
            usedBotApi = false,
            targetChatIdOrUsername = targetChat,
            targetRoleLabel = roleLabel,
            message = "Membuka aplikasi Telegram dengan lampiran PDF Resmi (${pdfFile.name})"
        )
    }
}
