package com.example.data.local.entity

data class JadwalDetail(
    val id: Long,
    val hariId: Int,
    val namaHari: String,
    val jpKode: String,
    val jamMulai: String,
    val jamSelesai: String,
    val kelasId: Long,
    val namaKelas: String,
    val tingkat: Int,
    val guruId: Long,
    val namaGuru: String,
    val mapelKode: String,
    val namaMapel: String,
    val warnaHex: String,
    val ruanganId: Long,
    val namaRuangan: String,
    val hasConflict: Boolean = false,
    val conflictMessages: List<String> = emptyList()
)
