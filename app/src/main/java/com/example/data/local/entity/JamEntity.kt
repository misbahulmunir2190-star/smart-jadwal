package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jam")
data class JamEntity(
    @PrimaryKey
    val jpKode: String, // "JP1", "JP2", ...
    val jamMulai: String, // "07.00"
    val jamSelesai: String, // "07.40"
    val isIstirahat: Boolean = false,
    val isSholatJumat: Boolean = false,
    val urutan: Int = 1
)
