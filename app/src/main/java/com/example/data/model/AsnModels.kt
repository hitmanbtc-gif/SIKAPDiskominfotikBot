package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class JabatanLevel(
    val label: String,
    val shortLabel: String,
    val description: String,
    val stepIndex: Int
) {
    PELAKSANA(
        label = "PPPK / Pelaksana",
        shortLabel = "Pelaksana",
        description = "Jenjang pelaporan hanya sampai ke Pejabat Pengawas / Fungsional untuk disahkan langsung",
        stepIndex = 0
    ),
    PENGAWAS_FUNGSIONAL(
        label = "Pejabat Pengawas / Fungsional",
        shortLabel = "Pengawas/JF",
        description = "Mengesahkan laporan PPPK/Pelaksana, memilih laporan untuk diteruskan ke Administrator, & jenjang pelaporan sampai ke Pejabat Administrator",
        stepIndex = 1
    ),
    ADMINISTRATOR(
        label = "Pejabat Administrator",
        shortLabel = "Administrator",
        description = "Mengesahkan laporan Pengawas/Fungsional, memilih laporan untuk diteruskan ke Kepala Dinas, & jenjang pelaporan ke Kepala Dinas",
        stepIndex = 2
    ),
    KEPALA_DINAS(
        label = "Kepala Dinas",
        shortLabel = "Kepala Dinas",
        description = "Mengesahkan laporan Pejabat Administrator serta laporan pilihan yang diteruskan kepada Kepala Dinas",
        stepIndex = 3
    );

    companion object {
        fun fromRaw(raw: String?): JabatanLevel =
            entries.firstOrNull { it.name == raw } ?: PELAKSANA
    }
}

enum class ActivityClassification(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    RUTIN(
        label = "Kegiatan Rutin",
        shortLabel = "Rutin",
        description = "Tugas pokok & fungsi harian sesuai uraian jabatan / SKP"
    ),
    REAKTIF(
        label = "Reaktif",
        shortLabel = "Reaktif",
        description = "Penanganan gangguan teknis, respon cepat aduan, atau insiden mendesak"
    ),
    ARAHAN_PIMPINAN(
        label = "Arahan Pimpinan",
        shortLabel = "Arahan",
        description = "Disposisi langsung Gubernur, Sekda, Kepala Dinas, atau Kepala Bidang"
    ),
    TUGAS_TAMBAHAN(
        label = "Tugas Tambahan Lainnya",
        shortLabel = "Tambahan",
        description = "Kepanitiaan, tim lintas OPD/bidang, atau pendampingan agenda daerah"
    );

    companion object {
        fun fromRaw(raw: String?): ActivityClassification =
            entries.firstOrNull { it.name == raw } ?: RUTIN
    }
}

enum class ReportStatus(
    val label: String,
    val tierLabel: String,
    val stepIndex: Int
) {
    DRAFT(
        label = "Draf",
        tierLabel = "Belum Dikirim",
        stepIndex = 0
    ),
    SUBMITTED_PENGAWAS(
        label = "Menunggu Pengesahan Pengawas/JF",
        tierLabel = "Jenjang Pengesahan: Pejabat Pengawas / Fungsional",
        stepIndex = 1
    ),
    SUBMITTED_ADMINISTRATOR(
        label = "Menunggu Pengesahan Administrator",
        tierLabel = "Jenjang Pengesahan: Pejabat Administrator",
        stepIndex = 2
    ),
    SUBMITTED_KADIS(
        label = "Menunggu Pengesahan Kepala Dinas",
        tierLabel = "Jenjang Pengesahan: Kepala Dinas",
        stepIndex = 3
    ),
    REVISION(
        label = "Perlu Revisi",
        tierLabel = "Dikembalikan ke Pelapor",
        stepIndex = 0
    ),
    APPROVED_FINAL(
        label = "Disahkan",
        tierLabel = "Telah Disahkan Pejabat Berwenang",
        stepIndex = 4
    );

    companion object {
        fun fromRaw(raw: String?): ReportStatus =
            entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

data class BidangUnit(
    val name: String,
    val subKegiatans: List<String>
)

object DiskominfotikCatalog {
    const val ADMIN_EMAIL = "hitmanbtc@gmail.com"
    const val DEFAULT_BOT_USERNAME = "@SIKAPDiskominfotikBot"
    const val DEFAULT_KADIS_NAME = "Kepala Dinas Kominfo & Statistik"
    const val DEFAULT_KADIS_NIP = "196808171994031009"

    val units: List<BidangUnit> = listOf(
        BidangUnit(
            name = "Sekretariat",
            subKegiatans = listOf(
                "Penyusunan Dokumen Perencanaan, Penganggaran & Evaluasi Kinerja Dinas",
                "Administrasi Kepegawaian, Pembinaan ASN & PPPK Lingkup Diskominfotik",
                "Pengelolaan Keuangan, Penatausahaan Barang Milik Daerah & Ketatausahaan",
                "Koordinasi Kehumasan Internal & Layanan Umum Perkantoran"
            )
        ),
        BidangUnit(
            name = "Bidang PISKP",
            subKegiatans = listOf(
                "Pengelolaan Informasi dan Saluran Komunikasi Publik Pemprov Gorontalo",
                "Layanan Hubungan Media, Peliputan & Konferensi Pers Pimpinan Daerah",
                "Pengelolaan Layanan Informasi Publik (PPID) & Pengaduan SP4N-LAPOR",
                "Diseminasi Informasi Pemerintahan & Konten Kreatif Media Sosial Resmi"
            )
        ),
        BidangUnit(
            name = "Bidang SDKI",
            subKegiatans = listOf(
                "Pengembangan Sumber Daya Komunikasi dan Informasi Masyarakat",
                "Penguatan Kemitraan Komunitas Informasi Masyarakat (KIM) Provinsi",
                "Literasi Digital, Edukasi Publik & Pemberdayaan Ekosistem Komunikasi",
                "Fasilitasi Kelembagaan Komunikasi dan Kemitraan Media Daerah"
            )
        ),
        BidangUnit(
            name = "Bidang e-Gov",
            subKegiatans = listOf(
                "Pengelolaan Infrastruktur Jaringan Intra Pemerintah & Pusat Data Daerah",
                "Pengembangan, Integrasi & Pemeliharaan Aplikasi Layanan Publik (SPBE)",
                "Layanan Dukungan Teknis Video Conference & Sistem Pemerintahan Elektronik",
                "Audit & Evaluasi Arsitektur SPBE Pemerintah Provinsi Gorontalo"
            )
        ),
        BidangUnit(
            name = "Bidang Persandian",
            subKegiatans = listOf(
                "Pengelolaan Keamanan Informasi, Persandian & Tim Tanggap Insiden Siber (CSIRT)",
                "Sertifikasi Elektronik (TTE BSrE) & Pengamanan Komunikasi Pimpinan Daerah",
                "Audit Keamanan Aplikasi, Uji Kerentanan & Pemantauan Trafik Siber Daerah",
                "Pengawasan & Evaluasi Standar Keamanan Informasi Perangkat Daerah"
            )
        ),
        BidangUnit(
            name = "Bidang Statistik",
            subKegiatans = listOf(
                "Pengumpulan, Pengolahan & Diseminasi Data Statistik Sektoral (Satu Data Gorontalo)",
                "Pembinaan Wali Data Perangkat Daerah & Penyusunan Publikasi Statistik Daerah",
                "Verifikasi, Validasi & Standarisasi Metadata Statistik Sektoral Provinsi",
                "Layanan Portal Satu Data & Analisis Statistik Pembangunan Daerah"
            )
        )
    )

    fun subKegiatansFor(unitName: String): List<String> =
        units.firstOrNull { it.name == unitName }?.subKegiatans ?: units.first().subKegiatans
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val nip: String = "",
    val jabatanLevel: String = JabatanLevel.PELAKSANA.name,
    val jabatanTitle: String = "",
    val unitBidang: String = DiskominfotikCatalog.units.first().name,
    val email: String = "",
    val pengawasEmail: String = "",
    val adminEmail: String = "",
    val kadisEmail: String = "",
    val myPhone: String = "",
    val pengawasPhone: String = "",
    val adminPhone: String = "",
    val kadisPhone: String = "",
    val profilePhotoBase64: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun levelEnum(): JabatanLevel = JabatanLevel.fromRaw(jabatanLevel)
}

@Keep
@IgnoreExtraProperties
data class AdminBotConfig(
    @DocumentId val id: String = "global",
    val botUsername: String = DiskominfotikCatalog.DEFAULT_BOT_USERNAME,
    val defaultGroupChatId: String = "",
    val pengawasChatId: String = "",
    val adminChatId: String = "",
    val kadisChatId: String = "",
    val kadisFullName: String = DiskominfotikCatalog.DEFAULT_KADIS_NAME,
    val kadisNip: String = DiskominfotikCatalog.DEFAULT_KADIS_NIP,
    val broadcastAnnouncement: String = "Pelaporan kegiatan harian ASN terhubung langsung ke Bot Telegram Dinas Kominfo & Statistik Provinsi Gorontalo.",
    val requireGpsLocation: Boolean = false,
    val requirePhotoEvidence: Boolean = false,
    val autoSendPdfToTelegram: Boolean = true,
    val autoSendGpsPin: Boolean = true,
    val updatedByEmail: String = "",
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorNip: String = "",
    val authorJabatan: String = "",
    val authorTier: String = JabatanLevel.PELAKSANA.name,
    val authorEmail: String = "",
    val activityDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val summary: String = "",
    val description: String = "",
    val classification: String = ActivityClassification.RUTIN.name,
    val unitBidang: String = "",
    val subKegiatan: String = "",
    val locationName: String = "",
    val gpsLatitude: Double = 0.0,
    val gpsLongitude: Double = 0.0,
    val gpsAccuracyMeters: Double = 0.0,
    val gpsAddress: String = "",
    val gpsCaptured: Boolean = false,
    val photoBase64: String = "",
    val photoCaption: String = "",
    val status: String = ReportStatus.DRAFT.name,
    val currentTier: String = JabatanLevel.PELAKSANA.name,
    val reviewerEmails: List<String> = emptyList(),
    val pengawasEmail: String = "",
    val adminEmail: String = "",
    val kadisEmail: String = "",
    val pengawasReviewerName: String = "",
    val pengawasNote: String = "",
    val pengawasReviewedAtMillis: Long = 0L,
    val adminReviewerName: String = "",
    val adminNote: String = "",
    val adminReviewedAtMillis: Long = 0L,
    val kadisReviewerName: String = "",
    val kadisNote: String = "",
    val kadisReviewedAtMillis: Long = 0L,
    val lastRevisionNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun classificationEnum(): ActivityClassification = ActivityClassification.fromRaw(classification)
    fun statusEnum(): ReportStatus = ReportStatus.fromRaw(status)
    fun authorTierEnum(): JabatanLevel = JabatanLevel.fromRaw(authorTier)

    fun canPengawasForwardToAdmin(): Boolean {
        if (statusEnum() != ReportStatus.APPROVED_FINAL) return false
        return currentTier == JabatanLevel.PENGAWAS_FUNGSIONAL.name ||
            (kadisReviewerName.isBlank() && adminReviewerName.isBlank() && pengawasReviewerName.isNotBlank())
    }

    fun canAdminForwardToKadis(): Boolean {
        if (statusEnum() != ReportStatus.APPROVED_FINAL) return false
        return currentTier == JabatanLevel.ADMINISTRATOR.name ||
            (kadisReviewerName.isBlank() && adminReviewerName.isNotBlank())
    }

    fun approvalStatusLabel(): String {
        val st = statusEnum()
        if (st != ReportStatus.APPROVED_FINAL) return st.label
        return when {
            canPengawasForwardToAdmin() -> "Disahkan Pengawas / JF"
            canAdminForwardToKadis() -> "Disahkan Administrator"
            else -> "Disahkan Kepala Dinas"
        }
    }

    fun approvalTierDetailLabel(): String {
        val st = statusEnum()
        if (st != ReportStatus.APPROVED_FINAL) return st.tierLabel
        return when {
            canPengawasForwardToAdmin() ->
                "Disahkan Pejabat Pengawas / Fungsional (Dapat Dipilih untuk Diteruskan ke Administrator)"
            canAdminForwardToKadis() ->
                "Disahkan Pejabat Administrator (Dapat Dipilih untuk Diteruskan ke Kepala Dinas)"
            else ->
                "Disahkan Final oleh Kepala Dinas"
        }
    }
}
