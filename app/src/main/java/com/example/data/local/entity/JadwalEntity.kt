package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jadwal")
data class JadwalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hariId: Int, // 1..6
    val jpKode: String, // "JP1"..
    val kelasId: Long,
    val guruId: Long,
    val mapelKode: String,
    val ruanganId: Long
)
