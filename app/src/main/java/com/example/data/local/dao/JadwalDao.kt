package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.JadwalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JadwalDao {
    @Query("SELECT * FROM jadwal")
    fun getAllJadwal(): Flow<List<JadwalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJadwal(jadwal: JadwalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllJadwal(jadwalList: List<JadwalEntity>)

    @Update
    suspend fun updateJadwal(jadwal: JadwalEntity)

    @Delete
    suspend fun deleteJadwal(jadwal: JadwalEntity)

    @Query("DELETE FROM jadwal WHERE id = :id")
    suspend fun deleteJadwalById(id: Long)

    @Query("DELETE FROM jadwal")
    suspend fun deleteAllJadwal()

    @Query("DELETE FROM jadwal WHERE hariId = :hariId AND jpKode = :jpKode AND kelasId = :kelasId")
    suspend fun deleteJadwalSlot(hariId: Int, jpKode: String, kelasId: Long)
}
