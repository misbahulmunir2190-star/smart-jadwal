package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.HariEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HariDao {
    @Query("SELECT * FROM hari ORDER BY urutan ASC")
    fun getAllHari(): Flow<List<HariEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHari(hariList: List<HariEntity>)

    @Update
    suspend fun updateHari(hari: HariEntity)
}
