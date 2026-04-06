package com.tubes.nimons360.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tubes.nimons360.R
import com.tubes.nimons360.data.location.LocationCallback
import com.tubes.nimons360.data.location.LocationHandler
import com.tubes.nimons360.data.model.MemberPresenceUpdated
import com.tubes.nimons360.data.model.UserLocation
import com.tubes.nimons360.data.sensor.OrientationCallback
import com.tubes.nimons360.data.sensor.OrientationHandler
import com.tubes.nimons360.data.websocket.WebSocketCallback
import com.tubes.nimons360.data.websocket.WebSocketManager
import com.tubes.nimons360.utils.DeviceInfoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.ConcurrentHashMap

class MapFragment : Fragment(), LocationCallback, OrientationCallback, WebSocketCallback {
    
    private val TAG = "MapFragment"
    private lateinit var mapView: MapView
    private lateinit var chipGroup: ChipGroup
    
    private lateinit var locationHandler: LocationHandler
    private lateinit var orientationHandler: OrientationHandler
    private var webSocketManager: WebSocketManager? = null
    
    private var currentLocation: Location? = null
    private var currentBearing: Float = 0f
    private var currentUserId: String = "user123"  // Mock - dapatkan dari TokenManager
    private var currentFamilyId: String = "family123"  // Mock
    
    // Overlay untuk menampilkan pins
    private var mapOverlay: ItemizedIconOverlay<OverlayItem>? = null
    private val onlineMembers = ConcurrentHashMap<String, UserLocation>()
    private val offlineTimestamps = ConcurrentHashMap<String, Long>()
    
    private var presenceUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Filter
    private var selectedFamily = "all"
    private val familyList = mutableListOf("All Families", "Family 1", "Family 2")
    
    private val LOCATION_PERMISSION_CODE = 100
    private val PERMISSION_REQUEST_CODE = 101
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup OSMDroid configuration
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mapView = view.findViewById(R.id.mapView)
        chipGroup = view.findViewById(R.id.family_chip_group)
        
        initializeHandlers()
        setupMap()
        setupFamilyFilter()
        requestLocationPermissions()
        setupWebSocket()
    }

    private fun initializeHandlers() {
        locationHandler = LocationHandler(requireContext())
        locationHandler.setCallback(this)
        
        orientationHandler = OrientationHandler(requireContext())
        orientationHandler.setCallback(this)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.isVerticalMapRepetitionEnabled = false
        
        // Zoom ke Jakarta sebagai default (lat, lon, zoom)
        val startPoint = GeoPoint(-6.2088, 106.8456)
        mapView.controller.setCenter(startPoint)
        mapView.controller.setZoom(16)
        
        // Create overlay untuk pins
        mapOverlay = ItemizedIconOverlay(requireContext(), mutableListOf(), object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                item?.let { showUserInfo(it) }
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                return false
            }
        })
        mapView.overlays.add(mapOverlay)
    }

    private fun setupFamilyFilter() {
        familyList.forEach { family ->
            val chip = Chip(requireContext()).apply {
                text = family
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedFamily = if (family == "All Families") "all" else family
                        updateMapPins()
                    }
                }
            }
            chipGroup.addView(chip)
            
            // Select "All Families" by default
            if (family == "All Families") {
                chip.isChecked = true
            }
        }
    }

    private fun setupWebSocket() {
        // Mock WebSocket URL - replace with actual server URL
        val websocketUrl = "ws://your-server-url:8080/ws"
        val token = "mock-token"  // Dapatkan dari TokenManager
        
        webSocketManager = WebSocketManager(websocketUrl, token)
        webSocketManager?.setCallback(this)
        // Note: Jangan connect sampai app benar-benar siap
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationAndOrientation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    private fun startLocationAndOrientation() {
        locationHandler.startLocationUpdates()
        orientationHandler.startListening()
        
        // Mulai mengirim presence update setiap 1 detik
        startPresenceUpdates()
        
        // Coba connect ke WebSocket
        try {
            webSocketManager?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting WebSocket", e)
        }
    }

    private fun startPresenceUpdates() {
        presenceUpdateJob = scope.launch {
            while (true) {
                currentLocation?.let { location ->
                    val batteryLevel = DeviceInfoUtil.getBatteryLevel(requireContext())
                    
                    webSocketManager?.sendPresenceUpdate(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        bearing = currentBearing,
                        batteryLevel = batteryLevel
                    )
                    
                    Log.d(TAG, "Sent presence update: ${location.latitude}, ${location.longitude}")
                }
                
                delay(1000)  // Setiap 1 detik
            }
        }
    }

    private fun updateMapPins() {
        mapOverlay?.let {
            it.removeAllItems()
            
            // Tambahkan current user pin
            currentLocation?.let { location ->
                val currentUserItem = createUserPin(
                    userId = currentUserId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    name = "Me",
                    bearing = currentBearing,
                    isCurrentUser = true
                )
                it.addItem(currentUserItem)
            }
            
            // Tambahkan member pins (filter berdasarkan family)
            onlineMembers.values.forEach { member ->
                if (selectedFamily == "all" || member.familyId == selectedFamily) {
                    val memberItem = createUserPin(
                        userId = member.userId,
                        latitude = member.latitude,
                        longitude = member.longitude,
                        name = member.name,
                        bearing = member.bearing,
                        isCurrentUser = false
                    )
                    it.addItem(memberItem)
                }
            }
            
            mapView.invalidate()
        }
    }

    private fun createUserPin(
        userId: String,
        latitude: Double,
        longitude: Double,
        name: String,
        bearing: Float,
        isCurrentUser: Boolean
    ): OverlayItem {
        val point = GeoPoint(latitude, longitude)
        val item = OverlayItem(name, "ID: $userId", point)
        
        // Set marker image (dengan rotation untuk bearing)
        // TODO: Setup custom marker dengan arrow
        
        return item
    }

    private fun showUserInfo(item: OverlayItem) {
        val userLocation = onlineMembers[item.snippet.split(": ").last()] ?: return
        
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.dialog_user_info, null)
        
        popupView.findViewById<TextView>(R.id.user_name).text = userLocation.name
        popupView.findViewById<TextView>(R.id.user_email).text = userLocation.email
        popupView.findViewById<TextView>(R.id.user_location).text = 
            DeviceInfoUtil.getLocationString(userLocation.latitude, userLocation.longitude)
        
        val batteryText = if (userLocation.batteryLevel >= 0) {
            "${userLocation.batteryLevel}%"
        } else {
            "N/A"
        }
        popupView.findViewById<TextView>(R.id.user_battery).text = batteryText
        
        val isOnlineStatus = if (userLocation.isOnline) "Online" else "Offline"
        popupView.findViewById<TextView>(R.id.user_status).text = isOnlineStatus
        
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        
        popupView.findViewById<Button>(R.id.btn_close).setOnClickListener {
            popupWindow.dismiss()
        }
        
        popupWindow.showAtLocation(mapView, android.view.Gravity.CENTER, 0, 0)
    }

    // ===== Callbacks =====

    override fun onLocationUpdated(location: Location) {
        currentLocation = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
        
        // Update map center ke current location
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        mapView.controller.setCenter(geoPoint)
        
        updateMapPins()
    }

    override fun onLocationError(error: String) {
        Log.e(TAG, "Location error: $error")
        AlertDialog.Builder(requireContext())
            .setTitle("Location Error")
            .setMessage(error)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    override fun onOrientationChanged(bearing: Float) {
        currentBearing = bearing
        Log.d(TAG, "Bearing changed: $bearing")
        // Update map pins dengan bearing baru
        updateMapPins()
    }

    override fun onMemberPresenceUpdated(presence: MemberPresenceUpdated) {
        Log.d(TAG, "Member presence updated: ${presence.name} at ${presence.latitude}, ${presence.longitude}")
        
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
        
        onlineMembers[presence.userId] = userLocation
        offlineTimestamps[presence.userId] = System.currentTimeMillis()
        
        updateMapPins()
    }

    override fun onWebSocketConnected() {
        Log.d(TAG, "WebSocket connected")
    }

    override fun onWebSocketDisconnected() {
        Log.d(TAG, "WebSocket disconnected")
        // Reconnect setelah beberapa detik
        scope.launch {
            delay(5000)
            try {
                webSocketManager?.connect()
            } catch (e: Exception) {
                Log.e(TAG, "Error reconnecting WebSocket", e)
            }
        }
    }

    override fun onWebSocketError(error: String) {
        Log.e(TAG, "WebSocket error: $error")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationAndOrientation()
                } else {
                    Log.w(TAG, "Location permission denied")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        orientationHandler.startListening()
        locationHandler.startLocationUpdates()
    }

    override fun onPause() {
        mapView.onPause()
        orientationHandler.stopListening()
        locationHandler.stopLocationUpdates()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView.onDetach()
        presenceUpdateJob?.cancel()
        webSocketManager?.disconnect()
        super.onDestroyView()
    }
}
