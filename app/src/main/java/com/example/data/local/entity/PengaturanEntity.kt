package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pengaturan")
data class PengaturanEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
