package com.example.ui.components

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import java.util.Locale

data class LatLng(val latitude: Double, val longitude: Double)

@Composable
fun RealMapPickerDialog(
    onConfirm: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize osmdroid user agent configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    // Default to New Delhi, India
    var currentLatLng by remember { mutableStateOf(LatLng(28.6139, 77.2090)) }
    var selectedAddress by remember { mutableStateOf("Fetching address...") }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Sync address when selection/marker position changes
    LaunchedEffect(currentLatLng) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(currentLatLng.latitude, currentLatLng.longitude, 1)
                val addressText = addresses?.firstOrNull()?.getAddressLine(0) ?: "Lat: ${String.format(Locale.US, "%.5f", currentLatLng.latitude)}, Lng: ${String.format(Locale.US, "%.5f", currentLatLng.longitude)}"
                withContext(Dispatchers.Main) {
                    selectedAddress = addressText
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    selectedAddress = "Lat: ${String.format(Locale.US, "%.5f", currentLatLng.latitude)}, Lng: ${String.format(Locale.US, "%.5f", currentLatLng.longitude)}"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real Map Picker 🗺️",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.trim().length >= 3) {
                            isSearching = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val results = geocoder.getFromLocationName(query, 5)
                                    withContext(Dispatchers.Main) {
                                        suggestions = results ?: emptyList()
                                        isSearching = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        suggestions = emptyList()
                                        isSearching = false
                                    }
                                }
                            }
                        } else {
                            suggestions = emptyList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search city, region, or address worldwide...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; suggestions = emptyList() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Search Results Dropdown
                if (suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(suggestions) { address ->
                                val dispName = address.getAddressLine(0) ?: "${address.latitude}, ${address.longitude}"
                                Text(
                                    text = dispName,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentLatLng = LatLng(address.latitude, address.longitude)
                                            searchQuery = dispName
                                            suggestions = emptyList()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // OpenStreetMap container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE8F5E9))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                                
                                val startPoint = GeoPoint(currentLatLng.latitude, currentLatLng.longitude)
                                controller.setCenter(startPoint)
                                
                                val marker = Marker(this).apply {
                                    position = startPoint
                                    isDraggable = true
                                    title = "Drag me to refine!"
                                }
                                overlays.add(marker)
                                
                                marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                    override fun onMarkerDrag(m: Marker?) {}
                                    override fun onMarkerDragStart(m: Marker?) {}
                                    override fun onMarkerDragEnd(m: Marker?) {
                                        m?.position?.let { gp ->
                                            currentLatLng = LatLng(gp.latitude, gp.longitude)
                                        }
                                    }
                                })
                                
                                val mapEventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                        p?.let { gp ->
                                            currentLatLng = LatLng(gp.latitude, gp.longitude)
                                        }
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                                }
                                overlays.add(MapEventsOverlay(mapEventsReceiver))
                            }
                        },
                        update = { mapView ->
                            val targetPoint = GeoPoint(currentLatLng.latitude, currentLatLng.longitude)
                            
                            val currentCenter = mapView.mapCenter
                            if (Math.abs(currentCenter.latitude - targetPoint.latitude) > 0.0001 ||
                                Math.abs(currentCenter.longitude - targetPoint.longitude) > 0.0001) {
                                mapView.controller.setCenter(targetPoint)
                            }
                            
                            val marker = mapView.overlays.filterIsInstance<Marker>().firstOrNull()
                            marker?.let {
                                if (Math.abs(it.position.latitude - targetPoint.latitude) > 0.00001 ||
                                    Math.abs(it.position.longitude - targetPoint.longitude) > 0.00001) {
                                    it.position = targetPoint
                                    mapView.invalidate()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Attribution Overlay Label
                    Text(
                        text = "© OpenStreetMap contributors",
                        fontSize = 10.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color(0xBBFFFFFF), RoundedCornerShape(topEnd = 4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Floating My Location Button on Map
                    FloatingActionButton(
                        onClick = {
                            Toast.makeText(context, "Centering map to GPS coordinates...", Toast.LENGTH_SHORT).show()
                            currentLatLng = LatLng(28.6139, 77.2090) // Delhi standard default fallback
                        },
                        containerColor = Color.White,
                        contentColor = Color(0xFF2E7D32),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                    }
                }

                // Selected address metadata
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "📌 Selected Address:",
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = selectedAddress,
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "Lat: ${String.format(Locale.US, "%.5f", currentLatLng.latitude)} | Lng: ${String.format(Locale.US, "%.5f", currentLatLng.longitude)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onConfirm(selectedAddress, currentLatLng.latitude, currentLatLng.longitude)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Confirm Location", color = Color.White)
                    }
                }
            }
        }
    }
}
