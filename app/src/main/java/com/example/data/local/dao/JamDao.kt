package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.JamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JamDao {
    @Query("SELECT * FROM jam ORDER BY urutan ASC")
    fun getAllJam(): Flow<List<JamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJam(jam: JamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllJam(jamList: List<JamEntity>)

    @Update
    suspend fun updateJam(jam: JamEntity)

    @Delete
    suspend fun deleteJam(jam: JamEntity)
}
