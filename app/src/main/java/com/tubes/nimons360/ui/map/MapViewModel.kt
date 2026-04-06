package com.tubes.nimons360.ui.map

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tubes.nimons360.data.model.MemberPresenceUpdated
import com.tubes.nimons360.data.model.UserLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

class MapViewModel : ViewModel() {
    
    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation
    
    private val _currentBearing = MutableLiveData<Float>(0f)
    val currentBearing: LiveData<Float> = _currentBearing
    
    private val _onlineMembers = MutableLiveData<Map<String, UserLocation>>()
    val onlineMembers: LiveData<Map<String, UserLocation>> = _onlineMembers
    
    private val _webSocketConnected = MutableLiveData<Boolean>(false)
    val webSocketConnected: LiveData<Boolean> = _webSocketConnected
    
    // Internal tracking
    private val memberLocations = ConcurrentHashMap<String, UserLocation>()
    
    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }
    
    fun updateBearing(bearing: Float) {
        _currentBearing.value = bearing
    }
    
    fun addOrUpdateMember(presence: MemberPresenceUpdated) {
        val userLocation = UserLocation(
            userId = presence.userId,
            familyId = presence.familyId,
            name = presence.name,
            email = presence.email,
            latitude = presence.latitude,
            longitude = presence.longitude,
            bearing = presence.bearing,
            batteryLevel = presence.batteryLevel,
            timestamp = presence.timestamp,
            isOnline = true
        )
        
        memberLocations[presence.userId] = userLocation
        _onlineMembers.value = memberLocations.toMap()
    }
    
    fun getMemberLocation(userId: String): UserLocation? {
        return memberLocations[userId]
    }
    
    fun removeMember(userId: String) {
        memberLocations.remove(userId)
        _onlineMembers.value = memberLocations.toMap()
    }
    
    fun setWebSocketConnected(connected: Boolean) {
        _webSocketConnected.value = connected
    }
    
    fun getAllMembers(): Map<String, UserLocation> {
        return memberLocations.toMap()
    }
    
    fun getMembersForFamily(familyId: String): List<UserLocation> {
        return memberLocations.values.filter { it.familyId == familyId }
    }
    
    override fun onCleared() {
        super.onCleared()
        memberLocations.clear()
    }
}
