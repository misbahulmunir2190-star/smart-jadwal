package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PengaturanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PengaturanDao {
    @Query("SELECT * FROM pengaturan")
    fun getAllPengaturan(): Flow<List<PengaturanEntity>>

    @Query("SELECT value FROM pengaturan WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengaturan(pengaturan: PengaturanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPengaturan(pengaturanList: List<PengaturanEntity>)
}
