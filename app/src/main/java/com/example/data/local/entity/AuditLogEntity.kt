package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val aksi: String, // "Tambah", "Edit", "Hapus", "AI Generate", "Clear"
    val userRole: String = "Operator Madrasah",
    val keterangan: String,
    val timestamp: Long = System.currentTimeMillis()
)
