package com.tubes.nimons360.data.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.util.Log

interface LocationCallback {
    fun onLocationUpdated(location: Location)
    fun onLocationError(error: String)
}

class LocationHandler(private val context: Context) {
    private val TAG = "LocationHandler"
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null
    private var callback: LocationCallback? = null

    fun setCallback(callback: LocationCallback) {
        this.callback = callback
    }

    fun startLocationUpdates() {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                callback?.onLocationError("Location permission not granted")
                return
            }

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                    callback?.onLocationUpdated(location)
                }

                override fun onProviderEnabled(provider: String) {
                    Log.d(TAG, "Provider enabled: $provider")
                }

                override fun onProviderDisabled(provider: String) {
                    Log.d(TAG, "Provider disabled: $provider")
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    // Called on API 30 or lower
                }
            }

            // Request location updates setiap 1 detik (untuk perlu update_presence)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,  // minTime (1 detik)
                0f,     // minDistance (0 meter)
                locationListener!!
            )

            // Fallback ke network provider jika GPS tidak tersedia
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,  // minTime (5 detik)
                    0f,
                    locationListener!!
                )
            }

            Log.d(TAG, "Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
            callback?.onLocationError(e.message ?: "Unknown error")
        }
    }

    fun stopLocationUpdates() {
        try {
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener!!)
                locationListener = null
                Log.d(TAG, "Location updates stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    fun getLastKnownLocation(): Location? {
        return try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Try GPS first
                val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLocation != null) return gpsLocation

                // Fallback to network
                return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location", e)
            null
        }
    }
}
