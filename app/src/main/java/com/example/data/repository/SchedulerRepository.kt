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

    // --- PREPOPULATION & DARUSSALAM MASTER DATA ---
    suspend fun applyDarussalamMasterData() = withContext(Dispatchers.IO) {
        try {
            // 1. Settings MTs Darussalam
            val darussalamSettings = listOf(
                PengaturanEntity("nama_madrasah", "MTs Darussalam"),
                PengaturanEntity("npsn", "20582511"),
                PengaturanEntity("nsm", "121235200016"),
                PengaturanEntity("alamat", "Lembeyan Kulon, Kec. Lembeyan"),
                PengaturanEntity("kota", "Magetan"),
                PengaturanEntity("provinsi", "Jawa Timur"),
                PengaturanEntity("semester", "Ganjil"),
                PengaturanEntity("tahun_pelajaran", "2025/2026"),
                PengaturanEntity("durasi_jp", "40 Menit"),
                PengaturanEntity("jam_masuk", "07.00"),
                PengaturanEntity("jam_pulang", "13.20"),
                PengaturanEntity("kepala_madrasah", "Misbahul Munir, S.Pd"),
                PengaturanEntity("nip_kepala", "-"),
                PengaturanEntity("waka_kurikulum", "Sabta Andry Desi Saputro, S.Pd"),
                PengaturanEntity("nip_waka", "-"),
                PengaturanEntity("role_aktif", "Operator Madrasah")
            )
            db.pengaturanDao().insertAllPengaturan(darussalamSettings)

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

            // 4. Mapel (Kurikulum MTs Kemenag)
            val defaultMapel = listOf(
                MapelEntity("BIN", "Bahasa Indonesia", "Kelompok A", 6, "#E65100"),
                MapelEntity("MTK", "Matematika", "Kelompok A", 5, "#1565C0"),
                MapelEntity("IPA", "Ilmu Pengetahuan Alam", "Kelompok A", 5, "#2E7D32"),
                MapelEntity("AA", "Aqidah Akhlak", "Kelompok A", 2, "#1B5E20"),
                MapelEntity("BIG", "Bahasa Inggris", "Kelompok A", 4, "#00838F"),
                MapelEntity("IPS", "Ilmu Pengetahuan Sosial", "Kelompok A", 4, "#F57F17"),
                MapelEntity("BAR", "Bahasa Arab", "Kelompok A", 3, "#006064"),
                MapelEntity("QH", "Al-Qur'an Hadits", "Kelompok A", 2, "#0D47A1"),
                MapelEntity("FK", "Fikih", "Kelompok A", 2, "#BF360C"),
                MapelEntity("SKI", "Sejarah Kebudayaan Islam", "Kelompok A", 2, "#4A148C"),
                MapelEntity("PP", "Pancasila & Kewarganegaraan", "Kelompok A", 3, "#B71C1C"),
                MapelEntity("INF", "Informatika", "Kelompok B", 2, "#283593"),
                MapelEntity("PJOK", "PJOK", "Kelompok B", 3, "#C62828"),
                MapelEntity("SBP", "Seni Budaya", "Kelompok B", 2, "#6A1B9A"),
                MapelEntity("BJW", "Bahasa Jawa (Mulok)", "Muatan Lokal", 2, "#4E342E")
            )
            db.mapelDao().insertAllMapel(defaultMapel)

            // 5. Guru MTs Darussalam (Data Resmi Pendidik)
            val darussalamGuru = listOf(
                GuruEntity(
                    id = 1,
                    nama = "DESY ARIANI PUTRI MERCURY, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000001",
                    jk = "P",
                    status = "Non PNS",
                    mapelUtama = "Bahasa Indonesia",
                    mapelTambahan = "Seni Budaya",
                    maxJp = 24,
                    tugasTambahan = "Wali Kelas",
                    ekuivalensiJp = 2,
                    isSertifikasi = true,
                    email = "desy@mtsdarussalam.sch.id",
                    hp = "-"
                ),
                GuruEntity(
                    id = 2,
                    nama = "LUSI NUFITASARI, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000002",
                    jk = "P",
                    status = "Non PNS",
                    mapelUtama = "Matematika",
                    mapelTambahan = "Informatika",
                    maxJp = 24,
                    tugasTambahan = "Wali Kelas",
                    ekuivalensiJp = 2,
                    isSertifikasi = true,
                    email = "lusi@mtsdarussalam.sch.id",
                    hp = "-"
                ),
                GuruEntity(
                    id = 3,
                    nama = "SABTA ANDRY DESI SAPUTRO, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000003",
                    jk = "L",
                    status = "Non PNS",
                    mapelUtama = "Ilmu Pengetahuan Alam",
                    mapelTambahan = "Pancasila & Kewarganegaraan",
                    maxJp = 24,
                    tugasTambahan = "Wakil Kepala Madrasah (Kurikulum)",
                    ekuivalensiJp = 12,
                    isSertifikasi = true,
                    email = "sabta@mtsdarussalam.sch.id",
                    hp = "-"
                ),
                GuruEntity(
                    id = 4,
                    nama = "MISBAHUL MUNIR, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000004",
                    jk = "L",
                    status = "Non PNS",
                    mapelUtama = "Aqidah Akhlak",
                    mapelTambahan = "Al-Qur'an Hadits, Fikih",
                    maxJp = 24,
                    tugasTambahan = "Kepala Madrasah",
                    ekuivalensiJp = 18,
                    isSertifikasi = true,
                    email = "misbahulmunir2190@gmail.com",
                    hp = "081235200016"
                ),
                GuruEntity(
                    id = 5,
                    nama = "AZIZAH DIAN NOVITASARI, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000005",
                    jk = "P",
                    status = "Non PNS",
                    mapelUtama = "Bahasa Inggris",
                    mapelTambahan = "Seni Budaya",
                    maxJp = 24,
                    tugasTambahan = "Wali Kelas",
                    ekuivalensiJp = 2,
                    isSertifikasi = true,
                    email = "azizah@mtsdarussalam.sch.id",
                    hp = "-"
                ),
                GuruEntity(
                    id = 6,
                    nama = "BOIMIN, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000006",
                    jk = "L",
                    status = "Non PNS",
                    mapelUtama = "Ilmu Pengetahuan Sosial",
                    mapelTambahan = "PJOK, Bahasa Jawa (Mulok)",
                    maxJp = 24,
                    tugasTambahan = "Wali Kelas",
                    ekuivalensiJp = 2,
                    isSertifikasi = true,
                    email = "boimin@mtsdarussalam.sch.id",
                    hp = "-"
                ),
                GuruEntity(
                    id = 7,
                    nama = "NELI KHOLIDAH ROHMAWATI, S.Pd",
                    nip = "-",
                    nuptk = "3520010000000007",
                    jk = "P",
                    status = "Non PNS",
                    mapelUtama = "Bahasa Arab",
                    mapelTambahan = "Sejarah Kebudayaan Islam",
                    maxJp = 24,
                    tugasTambahan = "Pembina Keagamaan / Tahfidz",
                    ekuivalensiJp = 2,
                    isSertifikasi = true,
                    email = "neli@mtsdarussalam.sch.id",
                    hp = "-"
                )
            )
            db.guruDao().insertAllGuru(darussalamGuru)

            // 6. Kelas MTs Darussalam
            val darussalamKelas = listOf(
                KelasEntity(1, "7A", 7, 1, "DESY ARIANI PUTRI MERCURY, S.Pd", 32),
                KelasEntity(2, "7B", 7, 2, "LUSI NUFITASARI, S.Pd", 32),
                KelasEntity(3, "8A", 8, 3, "SABTA ANDRY DESI SAPUTRO, S.Pd", 32),
                KelasEntity(4, "8B", 8, 4, "MISBAHUL MUNIR, S.Pd", 32),
                KelasEntity(5, "9A", 9, 5, "AZIZAH DIAN NOVITASARI, S.Pd", 32),
                KelasEntity(6, "9B", 9, 6, "BOIMIN, S.Pd", 32)
            )
            db.kelasDao().insertAllKelas(darussalamKelas)

            // 7. Ruangan
            val darussalamRuangan = listOf(
                RuanganEntity(1, "R. Kelas 7A", 36, "Teori"),
                RuanganEntity(2, "R. Kelas 7B", 36, "Teori"),
                RuanganEntity(3, "R. Kelas 8A", 36, "Teori"),
                RuanganEntity(4, "R. Kelas 8B", 36, "Teori"),
                RuanganEntity(5, "R. Kelas 9A", 36, "Teori"),
                RuanganEntity(6, "R. Kelas 9B", 36, "Teori"),
                RuanganEntity(7, "Laboratorium IPA & Komputer", 40, "Laboratorium"),
                RuanganEntity(8, "Musholla / Ruang Keagamaan", 60, "Khusus"),
                RuanganEntity(9, "Lapangan Olahraga MTs", 100, "Lapangan")
            )
            db.ruanganDao().insertAllRuangan(darussalamRuangan)

            // 8. Generate sample valid timetable using smart consecutive block engine (Maks 3 JP/hari, mapel > 3 JP dibagi ke hari lain)
            val generatedResult = com.example.data.remote.GeminiSchedulerService.generateFallbackSmartSchedule(
                guruList = darussalamGuru,
                mapelList = defaultMapel,
                kelasList = darussalamKelas,
                hariList = defaultHari,
                jamList = defaultJam,
                ruanganList = darussalamRuangan,
                mode = "generate"
            )

            val sampleJadwal = generatedResult.jadwalList.mapIndexed { idx, item ->
                JadwalEntity(
                    id = (idx + 1).toLong(),
                    hariId = item.hariId,
                    jpKode = item.jpKode,
                    kelasId = item.kelasId,
                    guruId = item.guruId,
                    mapelKode = item.mapelKode,
                    ruanganId = item.ruanganId
                )
            }
            db.jadwalDao().insertAllJadwal(sampleJadwal)

            logAudit("Data Pokok MTs Darussalam", "Memuat data pokok MTs Darussalam dan menyusun jadwal otomatis berurutan maks 3 JP per mapel")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun prepopulateDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val existingHari = db.hariDao().getAllHari().first()
            val existingSettings = db.pengaturanDao().getAllPengaturan().first()
            val currentSchool = existingSettings.find { it.key == "nama_madrasah" }?.value

            if (existingHari.isEmpty() || currentSchool == null || currentSchool == "MTs Negeri 1 Model") {
                applyDarussalamMasterData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
