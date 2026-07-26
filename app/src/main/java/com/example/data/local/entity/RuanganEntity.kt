package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ruangan")
data class RuanganEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val namaRuangan: String, // e.g. "R. Kelas 7A", "Lab IPA", "Lab Komputer"
    val kapasitas: Int = 36,
    val jenisRuangan: String = "Teori" // "Teori", "Laboratorium", "Lapangan", "Aula"
)
