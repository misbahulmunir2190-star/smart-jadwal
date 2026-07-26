package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GuruEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuruDao {
    @Query("SELECT * FROM guru ORDER BY nama ASC")
    fun getAllGuru(): Flow<List<GuruEntity>>

    @Query("SELECT * FROM guru WHERE id = :id")
    suspend fun getGuruById(id: Long): GuruEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuru(guru: GuruEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGuru(guruList: List<GuruEntity>)

    @Update
    suspend fun updateGuru(guru: GuruEntity)

    @Delete
    suspend fun deleteGuru(guru: GuruEntity)

    @Query("DELETE FROM guru")
    suspend fun deleteAllGuru()
}
