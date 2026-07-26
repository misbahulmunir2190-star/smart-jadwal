package com.example.domain.validator

import com.example.data.local.entity.*

data class ScheduleConflict(
    val type: ConflictType,
    val hariId: Int,
    val jpKode: String,
    val message: String,
    val affectedJadwalIds: List<Long>
)

enum class ConflictType {
    GURU_BENTROK,
    KELAS_BENTROK,
    RUANGAN_BENTROK,
    BEBAN_GURU_MELAMPAUI,
    JAM_ISTIRAHAT
}

object ConflictValidator {

    fun validateAll(
        jadwalList: List<JadwalEntity>,
        guruList: List<GuruEntity>,
        mapelList: List<MapelEntity>,
        kelasList: List<KelasEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        ruanganList: List<RuanganEntity>
    ): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()

        val guruMap = guruList.associateBy { it.id }
        val mapelMap = mapelList.associateBy { it.kode }
        val kelasMap = kelasList.associateBy { it.id }
        val hariMap = hariList.associateBy { it.id }
        val jamMap = jamList.associateBy { it.jpKode }
        val ruanganMap = ruanganList.associateBy { it.id }

        // 1. Group by Hari + JP + Guru (Check Guru Bentrok)
        val guruSlotGroup = jadwalList.groupBy { "${it.hariId}_${it.jpKode}_${it.guruId}" }
        guruSlotGroup.forEach { (key, list) ->
            if (list.size > 1) {
                val sample = list.first()
                val guruNama = guruMap[sample.guruId]?.nama ?: "Guru #${sample.guruId}"
                val hariNama = hariMap[sample.hariId]?.namaHari ?: "Hari #${sample.hariId}"
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.GURU_BENTROK,
                        hariId = sample.hariId,
                        jpKode = sample.jpKode,
                        message = "Guru $guruNama mengajar di ${list.size} kelas bersamaan pada hari $hariNama, ${sample.jpKode}",
                        affectedJadwalIds = list.map { it.id }
                    )
                )
            }
        }

        // 2. Group by Hari + JP + Kelas (Check Kelas Bentrok)
        val kelasSlotGroup = jadwalList.groupBy { "${it.hariId}_${it.jpKode}_${it.kelasId}" }
        kelasSlotGroup.forEach { (key, list) ->
            if (list.size > 1) {
                val sample = list.first()
                val kelasNama = kelasMap[sample.kelasId]?.namaKelas ?: "Kelas #${sample.kelasId}"
                val hariNama = hariMap[sample.hariId]?.namaHari ?: "Hari #${sample.hariId}"
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.KELAS_BENTROK,
                        hariId = sample.hariId,
                        jpKode = sample.jpKode,
                        message = "Kelas $kelasNama memiliki ${list.size} jadwal bersamaan pada hari $hariNama, ${sample.jpKode}",
                        affectedJadwalIds = list.map { it.id }
                    )
                )
            }
        }

        // 3. Group by Hari + JP + Ruangan (Check Ruangan Bentrok)
        val ruanganSlotGroup = jadwalList.groupBy { "${it.hariId}_${it.jpKode}_${it.ruanganId}" }
        ruanganSlotGroup.forEach { (key, list) ->
            if (list.size > 1) {
                val sample = list.first()
                val ruanganNama = ruanganMap[sample.ruanganId]?.namaRuangan ?: "Ruangan #${sample.ruanganId}"
                val hariNama = hariMap[sample.hariId]?.namaHari ?: "Hari #${sample.hariId}"
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.RUANGAN_BENTROK,
                        hariId = sample.hariId,
                        jpKode = sample.jpKode,
                        message = "Ruangan $ruanganNama dipakai oleh ${list.size} kelas bersamaan pada hari $hariNama, ${sample.jpKode}",
                        affectedJadwalIds = list.map { it.id }
                    )
                )
            }
        }

        // 4. Check Guru Max JP
        val guruJpCount = jadwalList.groupBy { it.guruId }.mapValues { it.value.size }
        guruJpCount.forEach { (guruId, count) ->
            val guru = guruMap[guruId]
            if (guru != null && count > guru.maxJp) {
                val affected = jadwalList.filter { it.guruId == guruId }.map { it.id }
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.BEBAN_GURU_MELAMPAUI,
                        hariId = 0,
                        jpKode = "-",
                        message = "Guru ${guru.nama} mengajar $count JP (Maksimal ${guru.maxJp} JP)",
                        affectedJadwalIds = affected
                    )
                )
            }
        }

        // 5. Check Istirahat / Sholat Jumat slots
        jadwalList.forEach { j ->
            val jam = jamMap[j.jpKode]
            if (jam != null && (jam.isIstirahat || jam.isSholatJumat)) {
                val type = if (jam.isSholatJumat) "Sholat Jumat" else "Jam Istirahat"
                val hariNama = hariMap[j.hariId]?.namaHari ?: ""
                conflicts.add(
                    ScheduleConflict(
                        type = ConflictType.JAM_ISTIRAHAT,
                        hariId = j.hariId,
                        jpKode = j.jpKode,
                        message = "Jadwal terisi pada $type ($hariNama ${j.jpKode})",
                        affectedJadwalIds = listOf(j.id)
                    )
                )
            }
        }

        return conflicts
    }

    fun enrichJadwalDetails(
        jadwalList: List<JadwalEntity>,
        guruList: List<GuruEntity>,
        mapelList: List<MapelEntity>,
        kelasList: List<KelasEntity>,
        hariList: List<HariEntity>,
        jamList: List<JamEntity>,
        ruanganList: List<RuanganEntity>,
        conflicts: List<ScheduleConflict>
    ): List<JadwalDetail> {
        val guruMap = guruList.associateBy { it.id }
        val mapelMap = mapelList.associateBy { it.kode }
        val kelasMap = kelasList.associateBy { it.id }
        val hariMap = hariList.associateBy { it.id }
        val jamMap = jamList.associateBy { it.jpKode }
        val ruanganMap = ruanganList.associateBy { it.id }

        val conflictMap = mutableMapOf<Long, MutableList<String>>()
        conflicts.forEach { c ->
            c.affectedJadwalIds.forEach { jId ->
                conflictMap.getOrPut(jId) { mutableListOf() }.add(c.message)
            }
        }

        return jadwalList.map { j ->
            val g = guruMap[j.guruId]
            val m = mapelMap[j.mapelKode]
            val k = kelasMap[j.kelasId]
            val h = hariMap[j.hariId]
            val jam = jamMap[j.jpKode]
            val r = ruanganMap[j.ruanganId]

            val cMsgs = conflictMap[j.id] ?: emptyList()

            JadwalDetail(
                id = j.id,
                hariId = j.hariId,
                namaHari = h?.namaHari ?: "Hari ${j.hariId}",
                jpKode = j.jpKode,
                jamMulai = jam?.jamMulai ?: "",
                jamSelesai = jam?.jamSelesai ?: "",
                kelasId = j.kelasId,
                namaKelas = k?.namaKelas ?: "Kelas ${j.kelasId}",
                tingkat = k?.tingkat ?: 7,
                guruId = j.guruId,
                namaGuru = g?.nama ?: "Guru #${j.guruId}",
                mapelKode = j.mapelKode,
                namaMapel = m?.namaMapel ?: j.mapelKode,
                warnaHex = m?.warnaHex ?: "#1E88E5",
                ruanganId = j.ruanganId,
                namaRuangan = r?.namaRuangan ?: "Ruangan #${j.ruanganId}",
                hasConflict = cMsgs.isNotEmpty(),
                conflictMessages = cMsgs
            )
        }
    }
}
