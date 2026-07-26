package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mapel")
data class MapelEntity(
    @PrimaryKey
    val kode: String, // e.g. "QH", "AA", "FK", "SKI", "BAR", "PP", "BIN", "MTK", "IPA", "IPS", "BIG", "INF", "PJOK", "SBP", "BJW"
    val namaMapel: String,
    val kelompok: String = "Kelompok A", // "Kelompok A", "Kelompok B", "Kelompok C", "Muatan Lokal"
    val jpPerMinggu: Int = 2,
    val warnaHex: String = "#1E88E5"
)
