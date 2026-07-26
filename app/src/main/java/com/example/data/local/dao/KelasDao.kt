package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.KelasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KelasDao {
    @Query("SELECT * FROM kelas ORDER BY tingkat ASC, namaKelas ASC")
    fun getAllKelas(): Flow<List<KelasEntity>>

    @Query("SELECT * FROM kelas WHERE id = :id")
    suspend fun getKelasById(id: Long): KelasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKelas(kelas: KelasEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllKelas(kelasList: List<KelasEntity>)

    @Update
    suspend fun updateKelas(kelas: KelasEntity)

    @Delete
    suspend fun deleteKelas(kelas: KelasEntity)

    @Query("DELETE FROM kelas")
    suspend fun deleteAllKelas()
}
