package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.domain.validator.ConflictValidator
import com.example.domain.validator.ScheduleConflict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class SchedulerRepository(private val db: AppDatabase) {

    val allGuru: Flow<List<GuruEntity>> = db.guruDao().getAllGuru()
    val allMapel: Flow<List<MapelEntity>> = db.mapelDao().getAllMapel()
    val allKelas: Flow<List<KelasEntity>> = db.kelasDao().getAllKelas()
    val allHari: Flow<List<HariEntity>> = db.hariDao().getAllHari()
    val allJam: Flow<List<JamEntity>> = db.jamDao().getAllJam()
    val allRuangan: Flow<List<RuanganEntity>> = db.ruanganDao().getAllRuangan()
    val allJadwal: Flow<List<JadwalEntity>> = db.jadwalDao().getAllJadwal()
    val allPengaturan: Flow<List<PengaturanEntity>> = db.pengaturanDao().getAllPengaturan()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllAuditLogs()

    // Combined Flow for enriched Schedule Details + Conflicts
    val enrichedJadwalDetails: Flow<Pair<List<JadwalDetail>, List<ScheduleConflict>>> =
        combine(
            allJadwal,
            allGuru,
            allMapel,
            allKelas,
            allHari
        ) { jList, gList, mList, kList, hList ->
            Pair(jList, listOf(gList, mList, kList, hList))
        }.combine(combine(allJam, allRuangan) { jamList, rList -> Pair(jamList, rList) }) { (jList, rest), (jamList, rList) ->
            @Suppress("UNCHECKED_CAST")
            val gList = rest[0] as List<GuruEntity>
            @Suppress("UNCHECKED_CAST")
            val mList = rest[1] as List<MapelEntity>
            @Suppress("UNCHECKED_CAST")
            val kList = rest[2] as List<KelasEntity>
            @Suppress("UNCHECKED_CAST")
            val hList = rest[3] as List<HariEntity>

            val conflicts = ConflictValidator.validateAll(jList, gList, mList, kList, hList, jamList, rList)
            val enriched = ConflictValidator.enrichJadwalDetails(jList, gList, mList, kList, hList, jamList, rList, conflicts)
            Pair(enriched, conflicts)
        }

    suspend fun logAudit(aksi: String, keterangan: String, role: String = "Operator Madrasah") {
        db.auditLogDao().insertAuditLog(
            AuditLogEntity(
                aksi = aksi,
                userRole = role,
                keterangan = keterangan,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // --- CRUD GURU ---
    suspend fun insertGuru(guru: GuruEntity): Long = withContext(Dispatchers.IO) {
        val id = db.guruDao().insertGuru(guru)
        logAudit("Tambah Guru", "Menambahkan guru baru: ${guru.nama}")
        id
    }

    suspend fun updateGuru(guru: GuruEntity) = withContext(Dispatchers.IO) {
        db.guruDao().updateGuru(guru)
        logAudit("Edit Guru", "Mengubah data guru: ${guru.nama}")
    }

    suspend fun deleteGuru(guru: GuruEntity) = withContext(Dispatchers.IO) {
        db.guruDao().deleteGuru(guru)
        logAudit("Hapus Guru", "Menghapus guru: ${guru.nama}")
    }

    // --- CRUD MAPEL ---
    suspend fun insertMapel(mapel: MapelEntity) = withContext(Dispatchers.IO) {
        db.mapelDao().insertMapel(mapel)
        logAudit("Tambah Mapel", "Menambahkan mapel: ${mapel.namaMapel} (${mapel.kode})")
    }

    suspend fun updateMapel(mapel: MapelEntity) = withContext(Dispatchers.IO) {
        db.mapelDao().updateMapel(mapel)
        logAudit("Edit Mapel", "Mengubah data mapel: ${mapel.namaMapel}")
    }

    suspend fun deleteMapel(mapel: MapelEntity) = withContext(Dispatchers.IO) {
        db.mapelDao().deleteMapel(mapel)
        logAudit("Hapus Mapel", "Menghapus mapel: ${mapel.namaMapel}")
    }

    // --- CRUD KELAS ---
    suspend fun insertKelas(kelas: KelasEntity): Long = withContext(Dispatchers.IO) {
        val id = db.kelasDao().insertKelas(kelas)
        logAudit("Tambah Kelas", "Menambahkan kelas: ${kelas.namaKelas}")
        id
    }

    suspend fun updateKelas(kelas: KelasEntity) = withContext(Dispatchers.IO) {
        db.kelasDao().updateKelas(kelas)
        logAudit("Edit Kelas", "Mengubah data kelas: ${kelas.namaKelas}")
    }

    suspend fun deleteKelas(kelas: KelasEntity) = withContext(Dispatchers.IO) {
        db.kelasDao().deleteKelas(kelas)
        logAudit("Hapus Kelas", "Menghapus kelas: ${kelas.namaKelas}")
    }

    // --- HARI & JAM ---
    suspend fun updateHari(hari: HariEntity) = withContext(Dispatchers.IO) {
        db.hariDao().updateHari(hari)
        logAudit("Edit Hari", "Mengubah status hari: ${hari.namaHari}")
    }

    suspend fun insertJam(jam: JamEntity) = withContext(Dispatchers.IO) {
        db.jamDao().insertJam(jam)
        logAudit("Tambah Jam", "Menambahkan slot jam: ${jam.jpKode}")
    }

    suspend fun updateJam(jam: JamEntity) = withContext(Dispatchers.IO) {
        db.jamDao().updateJam(jam)
        logAudit("Edit Jam", "Mengubah slot jam: ${jam.jpKode}")
    }

    suspend fun deleteJam(jam: JamEntity) = withContext(Dispatchers.IO) {
        db.jamDao().deleteJam(jam)
        logAudit("Hapus Jam", "Menghapus slot jam: ${jam.jpKode}")
    }

    // --- RUANGAN ---
    suspend fun insertRuangan(ruangan: RuanganEntity): Long = withContext(Dispatchers.IO) {
        val id = db.ruanganDao().insertRuangan(ruangan)
        logAudit("Tambah Ruangan", "Menambahkan ruangan: ${ruangan.namaRuangan}")
        id
    }

    suspend fun updateRuangan(ruangan: RuanganEntity) = withContext(Dispatchers.IO) {
        db.ruanganDao().updateRuangan(ruangan)
        logAudit("Edit Ruangan", "Mengubah data ruangan: ${ruangan.namaRuangan}")
    }

    suspend fun deleteRuangan(ruangan: RuanganEntity) = withContext(Dispatchers.IO) {
        db.ruanganDao().deleteRuangan(ruangan)
        logAudit("Hapus Ruangan", "Menghapus ruangan: ${ruangan.namaRuangan}")
    }

    // --- JADWAL ---
    suspend fun saveJadwalSlot(
        hariId: Int,
        jpKode: String,
        kelasId: Long,
        guruId: Long,
        mapelKode: String,
        ruanganId: Long
    ) = withContext(Dispatchers.IO) {
        db.jadwalDao().deleteJadwalSlot(hariId, jpKode, kelasId)
        val id = db.jadwalDao().insertJadwal(
            JadwalEntity(
                hariId = hariId,
                jpKode = jpKode,
                kelasId = kelasId,
                guruId = guruId,
                mapelKode = mapelKode,
                ruanganId = ruanganId
            )
        )
        logAudit("Assign Jadwal", "Menjadwalkan slot Hari $hariId $jpKode untuk Kelas ID $kelasId")
        id
    }

    suspend fun clearJadwalSlot(hariId: Int, jpKode: String, kelasId: Long) = withContext(Dispatchers.IO) {
        db.jadwalDao().deleteJadwalSlot(hariId, jpKode, kelasId)
        logAudit("Hapus Slot", "Mengosongkan slot Hari $hariId $jpKode Kelas ID $kelasId")
    }

    suspend fun saveGeneratedJadwalList(list: List<JadwalEntity>) = withContext(Dispatchers.IO) {
        db.jadwalDao().deleteAllJadwal()
        db.jadwalDao().insertAllJadwal(list)
        logAudit("AI Schedule Apply", "Menerapkan ${list.size} slot jadwal hasil AI / Auto Generator")
    }

    suspend fun clearAllJadwal() = withContext(Dispatchers.IO) {
        db.jadwalDao().deleteAllJadwal()
        logAudit("Clear All Schedule", "Mengosongkan seluruh tabel jadwal")
    }

    // --- PENGATURAN ---
    suspend fun savePengaturan(key: String, value: String) = withContext(Dispatchers.IO) {
        db.pengaturanDao().insertPengaturan(PengaturanEntity(key, value))
        logAudit("Ubah Pengaturan", "Memperbarui $key = $value")
    }

    // --- PREPOPULATION ---
    suspend fun prepopulateDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingHari = db.hariDao().getAllHari().first()
        if (existingHari.isNotEmpty()) return@withContext

        // 1. Prepopulate Settings
        val defaultSettings = listOf(
            PengaturanEntity("nama_madrasah", "MTs Negeri 1 Model"),
            PengaturanEntity("semester", "Ganjil"),
            PengaturanEntity("tahun_pelajaran", "2025/2026"),
            PengaturanEntity("durasi_jp", "40 Menit"),
            PengaturanEntity("jam_masuk", "07.00"),
            PengaturanEntity("jam_pulang", "13.20"),
            PengaturanEntity("role_aktif", "Operator Madrasah")
        )
        db.pengaturanDao().insertAllPengaturan(defaultSettings)

        // 2. Hari
        val defaultHari = listOf(
            HariEntity(1, "Senin", isAktif = true, urutan = 1),
            HariEntity(2, "Selasa", isAktif = true, urutan = 2),
            HariEntity(3, "Rabu", isAktif = true, urutan = 3),
            HariEntity(4, "Kamis", isAktif = true, urutan = 4),
            HariEntity(5, "Jumat", isAktif = true, urutan = 5),
            HariEntity(6, "Sabtu", isAktif = true, urutan = 6)
        )
        db.hariDao().insertAllHari(defaultHari)

        // 3. Jam
        val defaultJam = listOf(
            JamEntity("JP1", "07.00", "07.40", isIstirahat = false, isSholatJumat = false, urutan = 1),
            JamEntity("JP2", "07.40", "08.20", isIstirahat = false, isSholatJumat = false, urutan = 2),
            JamEntity("JP3", "08.20", "09.00", isIstirahat = false, isSholatJumat = false, urutan = 3),
            JamEntity("JP4", "09.00", "09.40", isIstirahat = false, isSholatJumat = false, urutan = 4),
            JamEntity("IST1", "09.40", "10.00", isIstirahat = true, isSholatJumat = false, urutan = 5),
            JamEntity("JP5", "10.00", "10.40", isIstirahat = false, isSholatJumat = false, urutan = 6),
            JamEntity("JP6", "10.40", "11.20", isIstirahat = false, isSholatJumat = false, urutan = 7),
            JamEntity("JP7", "11.20", "12.00", isIstirahat = false, isSholatJumat = false, urutan = 8),
            JamEntity("IST2", "12.00", "12.40", isIstirahat = true, isSholatJumat = true, urutan = 9),
            JamEntity("JP8", "12.40", "13.20", isIstirahat = false, isSholatJumat = false, urutan = 10)
        )
        db.jamDao().insertAllJam(defaultJam)

        // 4. Mapel (Mata Pelajaran MTs Kemenag Standard)
        val defaultMapel = listOf(
            MapelEntity("QH", "Al-Qur'an Hadits", "Kelompok A", 2, "#0D47A1"),
            MapelEntity("AA", "Aqidah Akhlak", "Kelompok A", 2, "#1B5E20"),
            MapelEntity("FK", "Fikih", "Kelompok A", 2, "#BF360C"),
            MapelEntity("SKI", "Sejarah Kebudayaan Islam", "Kelompok A", 2, "#4A148C"),
            MapelEntity("BAR", "Bahasa Arab", "Kelompok A", 3, "#006064"),
            MapelEntity("PP", "Pancasila & Kewarganegaraan", "Kelompok A", 3, "#B71C1C"),
            MapelEntity("BIN", "Bahasa Indonesia", "Kelompok A", 6, "#E65100"),
            MapelEntity("MTK", "Matematika", "Kelompok A", 5, "#1565C0"),
            MapelEntity("IPA", "IPA Terpadu", "Kelompok A", 5, "#2E7D32"),
            MapelEntity("IPS", "IPS Terpadu", "Kelompok A", 4, "#F57F17"),
            MapelEntity("BIG", "Bahasa Inggris", "Kelompok A", 4, "#00838F"),
            MapelEntity("INF", "Informatika", "Kelompok B", 2, "#283593"),
            MapelEntity("PJOK", "PJOK", "Kelompok B", 3, "#C62828"),
            MapelEntity("SBP", "Seni Budaya", "Kelompok B", 2, "#6A1B9A"),
            MapelEntity("BJW", "Bahasa Jawa (Mulok)", "Muatan Lokal", 2, "#4E342E")
        )
        db.mapelDao().insertAllMapel(defaultMapel)

        // 5. Guru
        val defaultGuru = listOf(
            GuruEntity(1, "Drs. H. Ahmad Fauzi, M.Ag.", "196803121994031002", "1234567890123001", "L", "PNS", "Al-Qur'an Hadits / Aqidah Akhlak", 24, "Wali Kelas 7A", true, "fauzi@madrasah.sch.id", "081234567891"),
            GuruEntity(2, "Siti Rahmah, S.Ag., M.Pd.", "197205151998022001", "1234567890123002", "P", "PNS", "Fikih / Bahasa Arab", 24, "Wali Kelas 7B", true, "siti@madrasah.sch.id", "081234567892"),
            GuruEntity(3, "Muhammad Nur, S.Pd.I", "198008202006041003", "1234567890123003", "L", "PPPK", "SKI / Bahasa Arab", 22, "Wali Kelas 8A", true, "nur@madrasah.sch.id", "081234567893"),
            GuruEntity(4, "Budi Santoso, S.Pd.", "198511102010011005", "1234567890123004", "L", "PNS", "Matematika", 26, "Wali Kelas 8B", true, "budi@madrasah.sch.id", "081234567894"),
            GuruEntity(5, "Dewi Lestari, S.Si.", "199002142015032002", "1234567890123005", "P", "PNS", "IPA Terpadu", 24, "Wali Kelas 9A", true, "dewi@madrasah.sch.id", "081234567895"),
            GuruEntity(6, "Ahmad Rizky, S.Pd.", "-", "1234567890123006", "L", "GTT", "IPS Terpadu / PPKn", 20, "Pembina OSIS", false, "rizky@madrasah.sch.id", "081234567896"),
            GuruEntity(7, "Nurul Hidayah, S.Pd.", "-", "1234567890123007", "P", "GTT", "Bahasa Indonesia", 22, "Wali Kelas 9B", false, "nurul@madrasah.sch.id", "081234567897"),
            GuruEntity(8, "Rudi Hermawan, S.Kom.", "-", "1234567890123008", "L", "GTT", "Informatika / Seni Budaya", 18, "Laboran Komputer", false, "rudi@madrasah.sch.id", "081234567898"),
            GuruEntity(9, "Eko Prasetyo, S.Pd.", "-", "1234567890123009", "L", "Honor", "PJOK / Bahasa Jawa", 18, "Guru Piket", false, "eko@madrasah.sch.id", "081234567899")
        )
        db.guruDao().insertAllGuru(defaultGuru)

        // 6. Kelas
        val defaultKelas = listOf(
            KelasEntity(1, "7A", 7, 1, "Drs. H. Ahmad Fauzi, M.Ag.", 32),
            KelasEntity(2, "7B", 7, 2, "Siti Rahmah, S.Ag., M.Pd.", 32),
            KelasEntity(3, "8A", 8, 3, "Muhammad Nur, S.Pd.I", 34),
            KelasEntity(4, "8B", 8, 4, "Budi Santoso, S.Pd.", 34),
            KelasEntity(5, "9A", 9, 5, "Dewi Lestari, S.Si.", 36),
            KelasEntity(6, "9B", 9, 7, "Nurul Hidayah, S.Pd.", 36)
        )
        db.kelasDao().insertAllKelas(defaultKelas)

        // 7. Ruangan
        val defaultRuangan = listOf(
            RuanganEntity(1, "R. Kelas 7A", 36, "Teori"),
            RuanganEntity(2, "R. Kelas 7B", 36, "Teori"),
            RuanganEntity(3, "R. Kelas 8A", 36, "Teori"),
            RuanganEntity(4, "R. Kelas 8B", 36, "Teori"),
            RuanganEntity(5, "R. Kelas 9A", 36, "Teori"),
            RuanganEntity(6, "R. Kelas 9B", 36, "Teori"),
            RuanganEntity(7, "Laboratorium IPA", 40, "Laboratorium"),
            RuanganEntity(8, "Laboratorium Komputer", 40, "Laboratorium"),
            RuanganEntity(9, "Lapangan Olahraga", 100, "Lapangan")
        )
        db.ruanganDao().insertAllRuangan(defaultRuangan)

        // 8. Generate Sample Timetable
        val sampleJadwal = mutableListOf<JadwalEntity>()
        var count = 0L

        // Generate initial non-conflicting slots for 7A & 7B
        val mapelSample = listOf("QH", "AA", "FK", "SKI", "BAR", "BIN", "MTK", "IPA", "IPS", "BIG")
        val jamValid = listOf("JP1", "JP2", "JP3", "JP4", "JP5", "JP6", "JP7", "JP8")

        for (hId in 1..5) {
            for ((idx, jKode) in jamValid.withIndex()) {
                val mKode7A = mapelSample[(hId + idx) % mapelSample.size]
                val gId7A = ((idx % 8) + 1).toLong()
                val rId7A = 1L

                count++
                sampleJadwal.add(
                    JadwalEntity(
                        id = count,
                        hariId = hId,
                        jpKode = jKode,
                        kelasId = 1L, // 7A
                        guruId = gId7A,
                        mapelKode = mKode7A,
                        ruanganId = rId7A
                    )
                )

                // 7B
                val mKode7B = mapelSample[(hId + idx + 2) % mapelSample.size]
                val gId7B = (((idx + 3) % 8) + 1).toLong()
                val rId7B = 2L

                count++
                sampleJadwal.add(
                    JadwalEntity(
                        id = count,
                        hariId = hId,
                        jpKode = jKode,
                        kelasId = 2L, // 7B
                        guruId = gId7B,
                        mapelKode = mKode7B,
                        ruanganId = rId7B
                    )
                )
            }
        }
        db.jadwalDao().insertAllJadwal(sampleJadwal)

        logAudit("Sistem Inisialisasi", "Database Smart Scheduler MTs disiapkan dengan data Kemenag standard")
    }
}
