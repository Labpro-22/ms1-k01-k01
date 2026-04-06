package com.tubes.nimons360.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.util.Log

object DeviceInfoUtil {
    private val TAG = "DeviceInfoUtil"

    fun getBatteryLevel(context: Context): Int {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            
            if (level == -1 || scale == -1) {
                -1
            } else {
                (level * 100 / scale)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            -1
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            caps != null && 
            (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
             caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
             caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) {
            Log.e(TAG, "Error checking internet availability", e)
            false
        }
    }

    fun getBatteryStatus(context: Context): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            
            when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                BatteryManager.BATTERY_STATUS_UNKNOWN -> "Unknown"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery status", e)
            "Unknown"
        }
    }

    fun getLocationString(latitude: Double, longitude: Double): String {
        return String.format("%.4f, %.4f", latitude, longitude)
    }
}
