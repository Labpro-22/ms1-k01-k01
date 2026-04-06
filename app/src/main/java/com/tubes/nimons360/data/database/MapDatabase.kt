package com.tubes.nimons360.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tubes.nimons360.data.model.FavoriteLocation

@Entity(tableName = "favorite_locations")
data class FavoriteLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FavoriteLocationDao {
    @Insert
    suspend fun insert(location: FavoriteLocationEntity)
    
    @Delete
    suspend fun delete(location: FavoriteLocationEntity)
    
    @Query("SELECT * FROM favorite_locations ORDER BY createdAt DESC")
    suspend fun getAllLocations(): List<FavoriteLocationEntity>
    
    @Query("SELECT * FROM favorite_locations WHERE id = :id")
    suspend fun getLocationById(id: Int): FavoriteLocationEntity?
    
    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Database(entities = [FavoriteLocationEntity::class], version = 1)
abstract class MapDatabase : RoomDatabase() {
    abstract fun favoriteLocationDao(): FavoriteLocationDao
}
