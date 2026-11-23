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
        description = "Staf Pelaksana / PPPK mengajukan laporan ke Pejabat Pengawas / Fungsional",
        stepIndex = 0
    ),
    PENGAWAS_FUNGSIONAL(
        label = "Pejabat Pengawas / Fungsional",
        shortLabel = "Pengawas/JF",
        description = "Kasubbag / Ketua Tim / Jabatan Fungsional memverifikasi & meneruskan ke Administrator",
        stepIndex = 1
    ),
    ADMINISTRATOR(
        label = "Pejabat Administrator",
        shortLabel = "Administrator",
        description = "Sekretaris Dinas / Kepala Bidang menelaah & meneruskan ke Kepala Dinas",
        stepIndex = 2
    ),
    KEPALA_DINAS(
        label = "Kepala Dinas",
        shortLabel = "Kepala Dinas",
        description = "Kepala Dinas Kominfo & Statistik Provinsi Gorontalo mengesahkan laporan akhir",
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
        label = "Menunggu Pengawas / JF",
        tierLabel = "Tingkat 1: Pejabat Pengawas / Fungsional",
        stepIndex = 1
    ),
    SUBMITTED_ADMINISTRATOR(
        label = "Menunggu Administrator",
        tierLabel = "Tingkat 2: Pejabat Administrator",
        stepIndex = 2
    ),
    SUBMITTED_KADIS(
        label = "Menunggu Kepala Dinas",
        tierLabel = "Tingkat 3: Kepala Dinas",
        stepIndex = 3
    ),
    REVISION(
        label = "Perlu Revisi",
        tierLabel = "Dikembalikan ke Pelapor",
        stepIndex = 0
    ),
    APPROVED_FINAL(
        label = "Disahkan Kepala Dinas",
        tierLabel = "Selesai & Terverifikasi Berjenjang",
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
            name = "Bidang Pengelolaan Informasi dan Komunikasi Publik (IKP)",
            subKegiatans = listOf(
                "Pengelolaan Media Komunikasi Publik & Diseminasi Informasi Pemprov Gorontalo",
                "Layanan Hubungan Media, Peliputan & Konferensi Pers Pimpinan Daerah",
                "Pengelolaan Layanan Informasi Publik (PPID) & Pengaduan SP4N-LAPOR",
                "Penguatan Kemitraan Komunitas Informasi Masyarakat (KIM) Provinsi"
            )
        ),
        BidangUnit(
            name = "Bidang Teknologi Informasi dan Komunikasi (TIK) / E-Gov",
            subKegiatans = listOf(
                "Pengelolaan Infrastruktur Jaringan Intra Pemerintah & Pusat Data Daerah",
                "Pengembangan, Integrasi & Pemeliharaan Aplikasi Layanan Publik (SPBE)",
                "Layanan Dukungan Teknis Video Conference & Sistem Pemerintahan Elektronik",
                "Audit & Evaluasi Arsitektur SPBE Pemerintah Provinsi Gorontalo"
            )
        ),
        BidangUnit(
            name = "Bidang Statistik dan Persandian",
            subKegiatans = listOf(
                "Pengumpulan, Pengolahan & Diseminasi Data Statistik Sektoral (Satu Data Gorontalo)",
                "Pembinaan Wali Data Perangkat Daerah & Penyusunan Publikasi Statistik",
                "Pengelolaan Keamanan Informasi, Persandian & Tim Tanggap Insiden Siber (CSIRT)",
                "Sertifikasi Elektronik & Pengamanan Komunikasi Pimpinan Daerah"
            )
        ),
        BidangUnit(
            name = "UPTD / Gorontalo Command Center",
            subKegiatans = listOf(
                "Operasional Gorontalo Command Center & Pemantauan Layanan Publik Terpadu",
                "Dukungan Teknis Multimedia & Jaringan Agenda Strategis Pemerintah Provinsi"
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
}
