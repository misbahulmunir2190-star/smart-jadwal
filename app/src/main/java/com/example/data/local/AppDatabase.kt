package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        GuruEntity::class,
        MapelEntity::class,
        KelasEntity::class,
        HariEntity::class,
        JamEntity::class,
        RuanganEntity::class,
        JadwalEntity::class,
        PengaturanEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guruDao(): GuruDao
    abstract fun mapelDao(): MapelDao
    abstract fun kelasDao(): KelasDao
    abstract fun hariDao(): HariDao
    abstract fun jamDao(): JamDao
    abstract fun ruanganDao(): RuanganDao
    abstract fun jadwalDao(): JadwalDao
    abstract fun pengaturanDao(): PengaturanDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_scheduler_mts.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
