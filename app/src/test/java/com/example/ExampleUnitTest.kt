package com.example

import com.example.data.local.entity.*
import com.example.data.remote.GeminiSchedulerService
import com.example.domain.validator.ConflictValidator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBlockSplitRules() {
        assertEquals(listOf(3, 3), GeminiSchedulerService.splitJpIntoDailyBlocks(6))
        assertEquals(listOf(3, 2), GeminiSchedulerService.splitJpIntoDailyBlocks(5))
        assertEquals(listOf(2, 2), GeminiSchedulerService.splitJpIntoDailyBlocks(4))
        assertEquals(listOf(3), GeminiSchedulerService.splitJpIntoDailyBlocks(3))
        assertEquals(listOf(2), GeminiSchedulerService.splitJpIntoDailyBlocks(2))
    }

    @Test
    fun testSmartScheduleZeroConflicts() {
        val defaultHari = listOf(
            HariEntity(1, "Senin", isAktif = true, urutan = 1),
            HariEntity(2, "Selasa", isAktif = true, urutan = 2),
            HariEntity(3, "Rabu", isAktif = true, urutan = 3),
            HariEntity(4, "Kamis", isAktif = true, urutan = 4),
            HariEntity(5, "Jumat", isAktif = true, urutan = 5),
            HariEntity(6, "Sabtu", isAktif = true, urutan = 6)
        )

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
            MapelEntity("SBP", "Seni Budaya", "Kelompok B", 2, "#6A1B9A")
        )

        val darussalamGuru = listOf(
            GuruEntity(1, "DESY ARIANI PUTRI MERCURY, S.Pd", "-", "3520010000000001", "P", "Non PNS", "Bahasa Indonesia", "Seni Budaya", 24, "Wali Kelas", 2, true, "desy@mtsdarussalam.sch.id", "-"),
            GuruEntity(2, "LUSI NUFITASARI, S.Pd", "-", "3520010000000002", "P", "Non PNS", "Matematika", "Informatika", 24, "Wali Kelas", 2, true, "lusi@mtsdarussalam.sch.id", "-"),
            GuruEntity(3, "SABTA ANDRY DESI SAPUTRO, S.Pd", "-", "3520010000000003", "L", "Non PNS", "Ilmu Pengetahuan Alam", "Pancasila & Kewarganegaraan", 24, "Wakil Kepala Madrasah", 12, true, "sabta@mtsdarussalam.sch.id", "-"),
            GuruEntity(4, "MISBAHUL MUNIR, S.Pd", "-", "3520010000000004", "L", "Non PNS", "Aqidah Akhlak", "Al-Qur'an Hadits, Fikih", 24, "Kepala Madrasah", 18, true, "misbahulmunir2190@gmail.com", "081235200016"),
            GuruEntity(5, "AZIZAH DIAN NOVITASARI, S.Pd", "-", "3520010000000005", "P", "Non PNS", "Bahasa Inggris", "Seni Budaya", 24, "Wali Kelas", 2, true, "azizah@mtsdarussalam.sch.id", "-"),
            GuruEntity(6, "BOIMIN, S.Pd", "-", "3520010000000006", "L", "Non PNS", "Ilmu Pengetahuan Sosial", "PJOK", 24, "Wali Kelas", 2, true, "boimin@mtsdarussalam.sch.id", "-"),
            GuruEntity(7, "NELI KHOLIDAH ROHMAWATI, S.Pd", "-", "3520010000000007", "P", "Non PNS", "Bahasa Arab", "Sejarah Kebudayaan Islam", 24, "Pembina Keagamaan", 2, true, "neli@mtsdarussalam.sch.id", "-")
        )

        val darussalamKelas = listOf(
            KelasEntity(1, "7A", 7, 1, "DESY ARIANI PUTRI MERCURY, S.Pd", 32),
            KelasEntity(2, "8A", 8, 3, "SABTA ANDRY DESI SAPUTRO, S.Pd", 32),
            KelasEntity(3, "9A", 9, 5, "AZIZAH DIAN NOVITASARI, S.Pd", 32)
        )

        val darussalamRuangan = listOf(
            RuanganEntity(1, "R. Kelas 7A", 36, "Teori"),
            RuanganEntity(2, "R. Kelas 8A", 36, "Teori"),
            RuanganEntity(3, "R. Kelas 9A", 36, "Teori")
        )

        val result = GeminiSchedulerService.generateFallbackSmartSchedule(
            guruList = darussalamGuru,
            mapelList = defaultMapel,
            kelasList = darussalamKelas,
            hariList = defaultHari,
            jamList = defaultJam,
            ruanganList = darussalamRuangan
        )

        assertTrue(result.success)
        assertTrue("Expected generated schedules but got none", result.jadwalList.isNotEmpty())

        val entities = result.jadwalList.mapIndexed { idx, it ->
            JadwalEntity(
                id = idx.toLong() + 1,
                hariId = it.hariId,
                jpKode = it.jpKode,
                kelasId = it.kelasId,
                guruId = it.guruId,
                mapelKode = it.mapelKode,
                ruanganId = it.ruanganId
            )
        }

        val conflicts = ConflictValidator.validateAll(
            jadwalList = entities,
            guruList = darussalamGuru,
            mapelList = defaultMapel,
            kelasList = darussalamKelas,
            hariList = defaultHari,
            jamList = defaultJam,
            ruanganList = darussalamRuangan
        )

        assertEquals("Expected 0 conflicts but found: ${conflicts.joinToString { it.message }}", 0, conflicts.size)
    }
}


