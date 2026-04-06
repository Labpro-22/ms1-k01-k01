package com.tubes.nimons360.data.repository

import android.content.Context
import androidx.room.Room
import com.tubes.nimons360.data.database.FavoriteLocationEntity
import com.tubes.nimons360.data.database.MapDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteLocationRepository(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        MapDatabase::class.java,
        "map_database"
    ).build()
    
    private val dao = database.favoriteLocationDao()
    
    suspend fun addFavoriteLocation(name: String, latitude: Double, longitude: Double, label: String = "") {
        withContext(Dispatchers.IO) {
            dao.insert(FavoriteLocationEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
                label = label
            ))
        }
    }
    
    suspend fun getFavoriteLocations(): List<FavoriteLocationEntity> {
        return withContext(Dispatchers.IO) {
            dao.getAllLocations()
        }
    }
    
    suspend fun deleteFavoriteLocation(id: Int) {
        withContext(Dispatchers.IO) {
            dao.deleteById(id)
        }
    }
    
    suspend fun updateFavoriteLocation(id: Int, name: String, label: String = "") {
        withContext(Dispatchers.IO) {
            val location = dao.getLocationById(id)
            location?.let {
                dao.delete(it)
                dao.insert(it.copy(name = name, label = label))
            }
        }
    }
}
