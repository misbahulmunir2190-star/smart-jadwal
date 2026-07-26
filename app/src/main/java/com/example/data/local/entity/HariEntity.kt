package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hari")
data class HariEntity(
    @PrimaryKey
    val id: Int, // 1: Senin, 2: Selasa, 3: Rabu, 4: Kamis, 5: Jumat, 6: Sabtu
    val namaHari: String,
    val isAktif: Boolean = true,
    val urutan: Int = id
)
