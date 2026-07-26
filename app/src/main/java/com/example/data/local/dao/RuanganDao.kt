package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.RuanganEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuanganDao {
    @Query("SELECT * FROM ruangan ORDER BY namaRuangan ASC")
    fun getAllRuangan(): Flow<List<RuanganEntity>>

    @Query("SELECT * FROM ruangan WHERE id = :id")
    suspend fun getRuanganById(id: Long): RuanganEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuangan(ruangan: RuanganEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRuangan(ruanganList: List<RuanganEntity>)

    @Update
    suspend fun updateRuangan(ruangan: RuanganEntity)

    @Delete
    suspend fun deleteRuangan(ruangan: RuanganEntity)

    @Query("DELETE FROM ruangan")
    suspend fun deleteAllRuangan()
}
