package com.tubes.nimons360.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.tubes.nimons360.data.model.MemberPresenceUpdated
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

interface WebSocketCallback {
    fun onMemberPresenceUpdated(presence: MemberPresenceUpdated)
    fun onWebSocketConnected()
    fun onWebSocketDisconnected()
    fun onWebSocketError(error: String)
}

class WebSocketManager(
    private val websocketUrl: String,
    private val token: String
) : WebSocketListener() {

    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private var callback: WebSocketCallback? = null
    private val TAG = "WebSocketManager"

    fun setCallback(callback: WebSocketCallback) {
        this.callback = callback
    }

    fun connect() {
        try {
            val request = Request.Builder()
                .url(websocketUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()
            
            webSocket = okHttpClient.newWebSocket(request, this)
            okHttpClient.dispatcher.executorService.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to WebSocket", e)
            callback?.onWebSocketError(e.message ?: "Unknown error")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun sendPresenceUpdate(
        latitude: Double,
        longitude: Double,
        bearing: Float,
        batteryLevel: Int
    ) {
        val message = mapOf(
            "type" to "update_presence",
            "data" to mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "bearing" to bearing,
                "batteryLevel" to batteryLevel,
                "timestamp" to System.currentTimeMillis()
            )
        )
        
        webSocket?.send(gson.toJson(message))
    }

    override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
        Log.d(TAG, "WebSocket connected")
        callback?.onWebSocketConnected()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val jsonElement = gson.fromJson(text, JsonElement::class.java)
            val jsonObject = jsonElement.asJsonObject
            val messageType = jsonObject.get("type")?.asString
            
            when (messageType) {
                "member_presence_updated" -> {
                    val data = jsonObject.get("data").asJsonObject
                    val presence = MemberPresenceUpdated(
                        userId = data.get("userId").asString,
                        familyId = data.get("familyId").asString,
                        name = data.get("name").asString,
                        email = data.get("email").asString,
                        latitude = data.get("latitude").asDouble,
                        longitude = data.get("longitude").asDouble,
                        bearing = data.get("bearing").asFloat,
                        batteryLevel = data.get("batteryLevel").asInt,
                        timestamp = data.get("timestamp").asLong
                    )
                    callback?.onMemberPresenceUpdated(presence)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebSocket message", e)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        Log.e(TAG, "WebSocket failure", t)
        callback?.onWebSocketError(t.message ?: "Unknown error")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closed: $code $reason")
        callback?.onWebSocketDisconnected()
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }
}
