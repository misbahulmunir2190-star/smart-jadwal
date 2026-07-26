package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guru")
data class GuruEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val nip: String = "-",
    val nuptk: String = "-",
    val jk: String = "L", // "L" or "P"
    val status: String = "PNS", // PNS, PPPK, GTT, Honor
    val mapelUtama: String = "",
    val maxJp: Int = 24,
    val tugasTambahan: String = "-",
    val isSertifikasi: Boolean = false,
    val email: String = "",
    val hp: String = ""
)
