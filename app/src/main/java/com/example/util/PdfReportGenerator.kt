package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.ActivityClassification
import com.example.data.model.ActivityReport
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)

    suspend fun generatePdf(
        context: Context,
        profile: UserProfile?,
        reports: List<ActivityReport>,
        periodLabel: String,
        adminConfig: com.example.data.model.AdminBotConfig? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val timestampTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(dir, "Lapkin_Diskominfotik_Gorontalo_$timestampTag.pdf")

            val navyPaint = Paint().apply {
                color = Color.rgb(22, 55, 90)
                isAntiAlias = true
            }
            val tealPaint = Paint().apply {
                color = Color.rgb(13, 148, 136)
                isAntiAlias = true
            }
            val lightBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.rgb(15, 37, 71)
                strokeWidth = 2f
                isAntiAlias = true
            }

            val kopTitlePaint = TextPaint().apply {
                color = Color.rgb(15, 37, 71)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val kopDinasPaint = TextPaint().apply {
                color = Color.rgb(22, 55, 90)
                textSize = 13.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val kopSubPaint = TextPaint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 8.5f
                isAntiAlias = true
            }
            val sectionHeaderPaint = TextPaint().apply {
                color = Color.rgb(15, 37, 71)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val boldBodyPaint = TextPaint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
                isAntiAlias = true
            }
            val mutedPaint = TextPaint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8f
                isAntiAlias = true
            }
            val badgeTextPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 8f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            var pageNumber = 1
            var page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = page.canvas
            var y = MARGIN_TOP

            fun startNewPage() {
                drawFooter(canvas, pageNumber, mutedPaint)
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = MARGIN_TOP
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNewPage()
                }
            }

            // 1. Official Kop Surat
            canvas.drawRect(MARGIN_H, y, MARGIN_H + 6f, y + 48f, navyPaint)
            canvas.drawRect(MARGIN_H + 6f, y, MARGIN_H + 10f, y + 48f, tealPaint)
            canvas.drawText("PEMERINTAH PROVINSI GORONTALO", MARGIN_H + 18f, y + 12f, kopTitlePaint)
            canvas.drawText(
                "DINAS KOMUNIKASI, INFORMATIKA DAN STATISTIK",
                MARGIN_H + 18f,
                y + 28f,
                kopDinasPaint
            )
            canvas.drawText(
                "Jl. Prof. Dr. H.B. Jassin, Kota Gorontalo • Laporan Pelaksanaan Kegiatan & Aktivitas Harian ASN",
                MARGIN_H + 18f,
                y + 42f,
                kopSubPaint
            )
            y += 54f
            canvas.drawLine(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y, linePaint)
            y += 14f

            // Document Title
            canvas.drawText("LAPORAN PELAKSANAAN KEGIATAN HARIAN ASN", MARGIN_H, y + 10f, sectionHeaderPaint)
            y += 18f

            // 2. Identity & Summary Box
            val authorName = profile?.fullName?.ifBlank { null }
                ?: reports.firstOrNull()?.authorName?.ifBlank { null }
                ?: "Pegawai ASN Diskominfotik"
            val authorNip = profile?.nip?.ifBlank { null }
                ?: reports.firstOrNull()?.authorNip?.ifBlank { "-" }
                ?: "-"
            val authorJabatan = profile?.jabatanTitle?.ifBlank { null }
                ?: profile?.levelEnum()?.label
                ?: reports.firstOrNull()?.authorTierEnum()?.label
                ?: "PPPK / Pelaksana"
            val unitBidang = profile?.unitBidang?.ifBlank { null }
                ?: reports.firstOrNull()?.unitBidang?.ifBlank { "-" }
                ?: "-"
            val printDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

            val infoRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 78f)
            canvas.drawRoundRect(infoRect, 6f, 6f, lightBgPaint)
            canvas.drawRoundRect(infoRect, 6f, 6f, borderPaint)

            val col2X = MARGIN_H + 270f
            canvas.drawText("Nama Pegawai : $authorName", MARGIN_H + 10f, y + 18f, boldBodyPaint)
            canvas.drawText("NIP / NI PPPK : $authorNip", MARGIN_H + 10f, y + 34f, bodyPaint)
            canvas.drawText("Jabatan : $authorJabatan", MARGIN_H + 10f, y + 50f, bodyPaint)
            canvas.drawText("Periode : $periodLabel", MARGIN_H + 10f, y + 66f, bodyPaint)

            canvas.drawText("Unit Kerja : $unitBidang", col2X, y + 18f, bodyPaint)
            canvas.drawText("Total Kegiatan : ${reports.size} Aktivitas", col2X, y + 34f, boldBodyPaint)
            canvas.drawText("Tanggal Cetak : $printDate", col2X, y + 50f, bodyPaint)
            y += 90f

            // 3. Classification Summary Strip
            val counts = ActivityClassification.entries.associateWith { cls ->
                reports.count { it.classificationEnum() == cls }
            }
            val boxW = (CONTENT_WIDTH - 18f) / 4f
            ActivityClassification.entries.forEachIndexed { idx, cls ->
                val x0 = MARGIN_H + idx * (boxW + 6f)
                val rect = RectF(x0, y, x0 + boxW, y + 34f)
                canvas.drawRoundRect(rect, 4f, 4f, borderPaint)
                canvas.drawText(cls.shortLabel.uppercase(Locale("id", "ID")), x0 + 8f, y + 13f, mutedPaint)
                canvas.drawText("${counts[cls] ?: 0} Kegiatan", x0 + 8f, y + 27f, boldBodyPaint)
            }
            y += 46f

            // 4. Activity Items
            canvas.drawText("RINCIAN KEGIATAN, SUB KEGIATAN & BUKTI PELAKSANAAN", MARGIN_H, y + 8f, sectionHeaderPaint)
            y += 16f

            reports.forEachIndexed { index, item ->
                val descLayout = buildStaticLayout(
                    text = item.description.ifBlank { "-" },
                    paint = bodyPaint,
                    width = (CONTENT_WIDTH - 110f).toInt().coerceAtLeast(180)
                )
                val photoBitmap = ImageHelper.decodeBase64ToBitmap(item.photoBase64)
                val hasPhoto = photoBitmap != null
                val cardContentHeight = 96f + descLayout.height.toFloat() + if (hasPhoto) 18f else 0f
                val cardHeight = cardContentHeight.coerceAtLeast(if (hasPhoto) 138f else 110f)

                ensureSpace(cardHeight + 12f)

                val cardRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + cardHeight)
                canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

                // Top header bar inside card
                val headerRect = RectF(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + 22f)
                canvas.drawRoundRect(headerRect, 6f, 6f, lightBgPaint)
                canvas.drawText(
                    "${index + 1}. [${item.activityDate} • ${item.startTime}–${item.endTime}] ${item.summary}",
                    MARGIN_H + 8f,
                    y + 14.5f,
                    boldBodyPaint
                )

                // Classification pill on top right
                val clsLabel = item.classificationEnum().shortLabel
                val pillW = badgeTextPaint.measureText(clsLabel) + 14f
                val pillRect = RectF(PAGE_WIDTH - MARGIN_H - pillW - 6f, y + 4f, PAGE_WIDTH - MARGIN_H - 6f, y + 18f)
                canvas.drawRoundRect(pillRect, 4f, 4f, navyPaint)
                canvas.drawText(clsLabel, pillRect.left + 7f, y + 13.5f, badgeTextPaint)

                var innerY = y + 34f
                val textWidth = if (hasPhoto) (CONTENT_WIDTH - 112f) else (CONTENT_WIDTH - 16f)

                canvas.drawText("Bidang/Sekretariat: ${item.unitBidang}", MARGIN_H + 8f, innerY, bodyPaint)
                innerY += 12f
                canvas.drawText(
                    "Sub Kegiatan: ${item.subKegiatan.take(92)}",
                    MARGIN_H + 8f,
                    innerY,
                    mutedPaint
                )
                innerY += 12f

                val gpsText = if (item.gpsCaptured) {
                    String.format(
                        Locale.US,
                        " (GPS: %.5f, %.5f • ±%.0fm)",
                        item.gpsLatitude,
                        item.gpsLongitude,
                        item.gpsAccuracyMeters
                    )
                } else {
                    " (Tanpa koordinat GPS)"
                }
                canvas.drawText("Lokasi: ${item.locationName}$gpsText", MARGIN_H + 8f, innerY, bodyPaint)
                innerY += 12f

                if (item.gpsAddress.isNotBlank()) {
                    canvas.drawText("Alamat GPS: ${item.gpsAddress.take(88)}", MARGIN_H + 8f, innerY, mutedPaint)
                    innerY += 12f
                }

                // Description multiline
                canvas.save()
                canvas.translate(MARGIN_H + 8f, innerY)
                descLayout.draw(canvas)
                canvas.restore()
                innerY += descLayout.height.toFloat() + 6f

                // Status & Verification trail
                val statusText = "Status Berjenjang: ${item.statusEnum().label} (${item.statusEnum().tierLabel})"
                canvas.drawText(statusText, MARGIN_H + 8f, y + cardHeight - 10f, boldBodyPaint)

                // Photo thumbnail on the right
                if (photoBitmap != null) {
                    val thumbSize = 84f
                    val thumbLeft = PAGE_WIDTH - MARGIN_H - thumbSize - 10f
                    val thumbTop = y + 28f
                    val dstRect = RectF(thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize)
                    canvas.drawBitmap(photoBitmap, null, dstRect, null)
                    canvas.drawRect(dstRect, borderPaint)
                    canvas.drawText("Bukti Foto", thumbLeft + 18f, thumbTop + thumbSize + 10f, mutedPaint)
                    photoBitmap.recycle()
                }

                y += cardHeight + 10f
            }

            // 5. Hierarchical 4-Tier Signature Block
            ensureSpace(145f)
            y += 6f
            canvas.drawText("LEMBAR VERIFIKASI & PENGESAHAN BERJENJANG", MARGIN_H, y + 10f, sectionHeaderPaint)
            y += 18f

            val sigW = (CONTENT_WIDTH - 12f) / 2f
            val sigH = 56f
            val latestPengawas = reports.firstOrNull { it.pengawasReviewerName.isNotBlank() }?.pengawasReviewerName
                ?: "Pejabat Pengawas / Fungsional"
            val latestAdmin = reports.firstOrNull { it.adminReviewerName.isNotBlank() }?.adminReviewerName
                ?: "Pejabat Administrator / Kabid"
            val latestKadis = reports.firstOrNull { it.kadisReviewerName.isNotBlank() }?.kadisReviewerName
                ?: adminConfig?.kadisFullName?.takeIf { it.isNotBlank() }
                ?: "Kepala Dinas Kominfo & Statistik"
            val kadisSubLabel = adminConfig?.kadisNip?.takeIf { it.isNotBlank() }?.let { "NIP. $it" }
                ?: "Diskominfotik Prov. Gorontalo"

            drawSignatureBox(
                canvas, MARGIN_H, y, sigW, sigH, borderPaint, mutedPaint, boldBodyPaint,
                "1. Yang Melaporkan (PPPK / Pelaksana)", authorName, "NIP. $authorNip"
            )
            drawSignatureBox(
                canvas, MARGIN_H + sigW + 12f, y, sigW, sigH, borderPaint, mutedPaint, boldBodyPaint,
                "2. Diverifikasi (Pengawas / Fungsional)", latestPengawas, "Tingkat I"
            )
            y += sigH + 8f
            drawSignatureBox(
                canvas, MARGIN_H, y, sigW, sigH, borderPaint, mutedPaint, boldBodyPaint,
                "3. Disetujui (Pejabat Administrator)", latestAdmin, "Tingkat II"
            )
            drawSignatureBox(
                canvas, MARGIN_H + sigW + 12f, y, sigW, sigH, borderPaint, mutedPaint, boldBodyPaint,
                "4. Mengesahkan (Kepala Dinas)", latestKadis, kadisSubLabel
            )

            drawFooter(canvas, pageNumber, mutedPaint)
            pdfDocument.finishPage(page)

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    suspend fun renderPdfPages(file: File): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 2
                val bmp = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888
                )
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bmp)
            }
        } finally {
            renderer?.close()
            pfd?.close()
        }
        bitmaps
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.0f)
            .setMaxLines(4)
            .build()
    }

    private fun drawSignatureBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        borderPaint: Paint,
        mutedPaint: TextPaint,
        boldPaint: TextPaint,
        roleLabel: String,
        personName: String,
        subLabel: String
    ) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 4f, 4f, borderPaint)
        canvas.drawText(roleLabel, x + 8f, y + 12f, mutedPaint)
        canvas.drawText(personName.take(34), x + 8f, y + h - 18f, boldPaint)
        canvas.drawText(subLabel.take(36), x + 8f, y + h - 7f, mutedPaint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, paint: TextPaint) {
        canvas.drawText(
            "e-Lapkin Diskominfotik Provinsi Gorontalo • Dokumen Pelaporan Berjenjang Resmi",
            MARGIN_H,
            PAGE_HEIGHT - 18f,
            paint
        )
        canvas.drawText(
            "Halaman $pageNumber",
            PAGE_WIDTH - MARGIN_H - 48f,
            PAGE_HEIGHT - 18f,
            paint
        )
    }
}
