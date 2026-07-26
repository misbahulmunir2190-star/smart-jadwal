package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kelas")
data class KelasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val namaKelas: String, // e.g. "7A", "7B", "8A", "8B", "9A"
    val tingkat: Int = 7, // 7, 8, 9
    val waliKelasGuruId: Long? = null,
    val waliKelasNama: String = "-",
    val jumlahSiswa: Int = 32
)
