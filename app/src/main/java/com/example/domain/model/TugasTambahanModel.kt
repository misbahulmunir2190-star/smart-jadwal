package com.example.domain.model

import com.example.data.local.entity.GuruEntity
import com.example.data.local.entity.JadwalDetail

/**
 * Model Tugas Tambahan yang Diakui dalam Regulasi Kemenag / Simpatika / Emis
 * untuk Pemenuhan Beban Kerja Guru Minimal 24 JP per Minggu (PMA No. 89 Th 2021 & Kepdirjen Pendis).
 */
data class TugasTambahanOption(
    val id: String,
    val nama: String,
    val ekuivalensiJp: Int,
    val minimalTatapMukaJp: Int,
    val kategori: String,
    val deskripsi: String
)

object TugasTambahanCatalog {
    val RECOGNIZED_OPTIONS = listOf(
        TugasTambahanOption(
            id = "KAMAD",
            nama = "Kepala Madrasah",
            ekuivalensiJp = 18,
            minimalTatapMukaJp = 6,
            kategori = "Pimpinan Struktural",
            deskripsi = "Ekuivalensi 18 JP (Wajib mengajar tatap muka minimal 6 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "WAKA_KUR",
            nama = "Wakil Kepala Madrasah (Kurikulum)",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Pimpinan Satminkal",
            deskripsi = "Ekuivalensi 12 JP (Wajib mengajar tatap muka minimal 12 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "WAKA_KES",
            nama = "Wakil Kepala Madrasah (Kesiswaan)",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Pimpinan Satminkal",
            deskripsi = "Ekuivalensi 12 JP (Wajib mengajar tatap muka minimal 12 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "WAKA_SAR",
            nama = "Wakil Kepala Madrasah (Sarpras)",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Pimpinan Satminkal",
            deskripsi = "Ekuivalensi 12 JP (Wajib mengajar tatap muka minimal 12 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "WAKA_HUM",
            nama = "Wakil Kepala Madrasah (Humas)",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Pimpinan Satminkal",
            deskripsi = "Ekuivalensi 12 JP (Wajib mengajar tatap muka minimal 12 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "KEPALA_PERPUS",
            nama = "Kepala Perpustakaan Madrasah",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Unit Penunjang",
            deskripsi = "Ekuivalensi 12 JP (Wajib tatap muka minimal 12 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "KEPALA_LAB",
            nama = "Kepala Laboratorium (IPA/Komputer)",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Unit Penunjang",
            deskripsi = "Ekuivalensi 12 JP (Wajib tatap muka minimal 12 JP/minggu)"
        ),
        TugasTambahanOption(
            id = "PEMBINA_ASRAMA",
            nama = "Pembina Asrama / Ma'had Madrasah",
            ekuivalensiJp = 12,
            minimalTatapMukaJp = 12,
            kategori = "Kepesantrenan",
            deskripsi = "Ekuivalensi 12 JP (Khusus madrasah berasrama / Islamic Boarding)"
        ),
        TugasTambahanOption(
            id = "WALI_KELAS",
            nama = "Wali Kelas",
            ekuivalensiJp = 2,
            minimalTatapMukaJp = 18,
            kategori = "Akademik",
            deskripsi = "Ekuivalensi 2 JP / Rombel (Diakui dalam Simpatika)"
        ),
        TugasTambahanOption(
            id = "PEMBINA_OSIS",
            nama = "Pembina OSIS / IPNU / IPPNU",
            ekuivalensiJp = 2,
            minimalTatapMukaJp = 18,
            kategori = "Kesiswaan",
            deskripsi = "Ekuivalensi 2 JP / Periode Kepengurusan"
        ),
        TugasTambahanOption(
            id = "PEMBINA_EKSKUL",
            nama = "Pembina Ekstrakurikuler / Pramuka",
            ekuivalensiJp = 2,
            minimalTatapMukaJp = 18,
            kategori = "Pengembangan Diri",
            deskripsi = "Ekuivalensi 2 JP (Pramuka, PMR, Drumband, Olahraga)"
        ),
        TugasTambahanOption(
            id = "PEMBINA_AGAMA",
            nama = "Pembina Keagamaan / Tahfidz / Ibadah",
            ekuivalensiJp = 2,
            minimalTatapMukaJp = 18,
            kategori = "Karakter & Ibadah",
            deskripsi = "Ekuivalensi 2 JP (Koordinator Sholat Dhuha/Dzuhur, Tahfidz Al-Qur'an)"
        ),
        TugasTambahanOption(
            id = "KOORD_P5RA",
            nama = "Koordinator P5RA / Kurikulum Merdeka",
            ekuivalensiJp = 2,
            minimalTatapMukaJp = 18,
            kategori = "Kurikulum",
            deskripsi = "Ekuivalensi 2 JP per rombongan belajar/fase"
        ),
        TugasTambahanOption(
            id = "GURU_PIKET",
            nama = "Guru Piket Harian",
            ekuivalensiJp = 1,
            minimalTatapMukaJp = 18,
            kategori = "Kedisiplinan",
            deskripsi = "Ekuivalensi 1 JP / Minggu"
        ),
        TugasTambahanOption(
            id = "PENGELOLA_IT",
            nama = "Pengelola IT / Operator Emis & Simpatika",
            ekuivalensiJp = 2,
            minimalTatapMukaJp = 18,
            kategori = "Sistem & Data",
            deskripsi = "Ekuivalensi 2 JP (Pengelolaan sistem pangkalan data madrasah)"
        ),
        TugasTambahanOption(
            id = "NON_TUGAS",
            nama = "Tidak Ada / Guru Reguler",
            ekuivalensiJp = 0,
            minimalTatapMukaJp = 24,
            kategori = "Umum",
            deskripsi = "Fokus penuh pada jam mengajar tatap muka 24 JP"
        )
    )

    fun findOptionByName(nama: String): TugasTambahanOption? {
        return RECOGNIZED_OPTIONS.find { opt ->
            nama.contains(opt.nama, ignoreCase = true) ||
            opt.nama.contains(nama, ignoreCase = true)
        }
    }
}

/**
 * Hasil Analisis Kelayakan Sertifikasi TPG (24 JP)
 */
data class AnalisisSertifikasi(
    val guruId: Long,
    val guruNama: String,
    val isSertifikasi: Boolean,
    val jpTatapMuka: Int,
    val jpEkuivalensi: Int,
    val totalJpBeban: Int,
    val targetMinJp: Int = 24,
    val isLayak: Boolean,
    val statusLabel: String,
    val kekuranganJp: Int,
    val kelebihanJp: Int,
    val persentasePemenuhan: Float,
    val rekomendasi: String
)

object SertifikasiAnalyzer {
    /**
     * Hitung kelayakan sertifikasi guru berdasarkan jadwal tatap muka & tugas tambahan ekuivalen
     */
    fun hitungKelayakan(
        guru: GuruEntity,
        jadwalDetails: List<JadwalDetail>
    ): AnalisisSertifikasi {
        val jpTatapMuka = jadwalDetails.count { it.guruId == guru.id }
        val jpEkuivalensi = guru.ekuivalensiJp
        val totalBeban = jpTatapMuka + jpEkuivalensi
        val targetMin = 24

        val isLayak = totalBeban >= targetMin
        val kekurangan = (targetMin - totalBeban).coerceAtLeast(0)
        val kelebihan = (totalBeban - targetMin).coerceAtLeast(0)
        val pct = (totalBeban.toFloat() / targetMin.toFloat()).coerceIn(0f, 1.5f)

        val statusLabel = when {
            !guru.isSertifikasi -> "Non-Sertifikasi (Belum Sertifikasi)"
            totalBeban >= 40 -> "Melebihi Beban Maksimal (${totalBeban} JP)"
            totalBeban >= targetMin -> "LAYAK TPG (Memenuhi ${totalBeban}/24 JP)"
            else -> "BELUM MEMENUHI (Kurang $kekurangan JP)"
        }

        val rekomendasi = when {
            !guru.isSertifikasi -> "Guru berstatus Non-Sertifikasi. Total beban saat ini $totalBeban JP (Tatap Muka: $jpTatapMuka JP, Ekuivalensi: $jpEkuivalensi JP)."
            totalBeban >= targetMin -> "Beban kerja telah memenuhi regulasi Kemenag/Simpatika (Minimal 24 JP/minggu). Syarat pencairan TPG terpenuhi."
            else -> {
                val saran = mutableListOf<String>()
                if (guru.mapelTambahan.isNotBlank()) {
                    saran.add("Tambah alokasi rombel kelas untuk mapel tambahan (${guru.mapelTambahan})")
                } else {
                    saran.add("Tambahkan Mapel Tambahan yang serumpun/bisa diampu")
                }
                if (guru.ekuivalensiJp == 0) {
                    saran.add("Beri tugas tambahan yang diakui (Wali Kelas +2 JP, Pembina Ekskul +2 JP, atau Piket +1 JP)")
                }
                "Kekurangan $kekurangan JP untuk memenuhi syarat 24 JP Simpatika. Saran: ${saran.joinToString(" atau ")}."
            }
        }

        return AnalisisSertifikasi(
            guruId = guru.id,
            guruNama = guru.nama,
            isSertifikasi = guru.isSertifikasi,
            jpTatapMuka = jpTatapMuka,
            jpEkuivalensi = jpEkuivalensi,
            totalJpBeban = totalBeban,
            targetMinJp = targetMin,
            isLayak = isLayak,
            statusLabel = statusLabel,
            kekuranganJp = kekurangan,
            kelebihanJp = kelebihan,
            persentasePemenuhan = pct,
            rekomendasi = rekomendasi
        )
    }
}
