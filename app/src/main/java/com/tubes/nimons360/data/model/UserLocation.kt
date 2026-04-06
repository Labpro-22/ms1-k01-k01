package com.tubes.nimons360.data.model

data class UserLocation(
    val userId: String,
    val familyId: String,
    val name: String,
    val email: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float = 0f,  // Arah smartphone (0-360 derajat)
    val batteryLevel: Int = 0,  // Persentase baterai
    val timestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
)

data class PresenceUpdate(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val batteryLevel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemberPresenceUpdated(
    val userId: String,
    val familyId: String,
    val name: String,
    val email: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val batteryLevel: Int,
    val timestamp: Long
)

data class FamilyMember(
    val userId: String,
    val familyId: String,
    val name: String,
    val email: String,
    val location: String = "Unknown"
)

data class FavoriteLocation(
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
