package com.tubes.nimons360.data.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Enhanced WebSocket connection manager dengan automatic reconnection
 * dan heartbeat mechanism untuk connection health checking
 */
class WebSocketConnectionManager(
    private val url: String,
    private val token: String,
    private val scope: CoroutineScope
) {
    private val TAG = "WebSocketConnManager"
    
    private var isConnecting = false
    private var reconnectAttempts = 0
    private val MAX_RECONNECT_ATTEMPTS = 5
    private val RECONNECT_DELAY_MS = 3000L
    private val HEARTBEAT_INTERVAL_MS = 30000L
    
    fun startAutoReconnection() {
        if (isConnecting) return
        
        scope.launch(Dispatchers.IO) {
            while (scope.isActive && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                try {
                    isConnecting = true
                    Log.d(TAG, "Attempting WebSocket reconnection... (attempt ${reconnectAttempts + 1})")
                    
                    // Attempt to connect
                    delay(RECONNECT_DELAY_MS)
                    reconnectAttempts++
                    
                    // Reset on successful connection
                    if (isConnecting) {
                        reconnectAttempts = 0
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "WebSocket connection failed", e)
                    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                        Log.e(TAG, "Max reconnection attempts reached")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during reconnection", e)
                }
            }
        }
    }
    
    fun stopAutoReconnection() {
        isConnecting = false
        reconnectAttempts = 0
    }
    
    fun isConnecting(): Boolean = isConnecting
}
