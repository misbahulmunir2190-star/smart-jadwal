package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MapelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapelDao {
    @Query("SELECT * FROM mapel ORDER BY kode ASC")
    fun getAllMapel(): Flow<List<MapelEntity>>

    @Query("SELECT * FROM mapel WHERE kode = :kode")
    suspend fun getMapelByKode(kode: String): MapelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapel(mapel: MapelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMapel(mapelList: List<MapelEntity>)

    @Update
    suspend fun updateMapel(mapel: MapelEntity)

    @Delete
    suspend fun deleteMapel(mapel: MapelEntity)

    @Query("DELETE FROM mapel")
    suspend fun deleteAllMapel()
}
