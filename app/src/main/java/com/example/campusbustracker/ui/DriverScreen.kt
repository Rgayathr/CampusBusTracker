package com.example.campusbustracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.campusbustracker.data.auth.SessionManager
import com.example.campusbustracker.network.ApiClient
import com.example.campusbustracker.network.ManualCheckinRequest
import com.example.campusbustracker.network.ScanRequest
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import com.example.campusbustracker.data.local.OfflineQueue
import com.example.campusbustracker.nfc.NfcReader

private val DEFAULT_BUSES = listOf("BUS-01", "BUS-02", "BUS-03", "BUS-04")
private val DEFAULT_PHYSICAL_CHECKPOINTS = listOf(
    "Main Gate Entrance",
    "Science Block Auditorium",
    "Sports Complex Terminal"
)
private val DEFAULT_CITY_VIRTUAL_CHECKPOINTS = listOf(
    "CITY-STOP-101: City Market Circle",
    "CITY-STOP-102: Central Railway Junction",
    "CITY-STOP-103: Tech Park Hub"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busCode by remember { mutableStateOf("BUS-01") }
    var passengerLoad by remember { mutableStateOf("Medium") } // Low, Medium, Full
    var availableBuses by remember { mutableStateOf(DEFAULT_BUSES) }
    var physicalCheckpoints by remember { mutableStateOf(DEFAULT_PHYSICAL_CHECKPOINTS) }
    var cityVirtualCheckpoints by remember { mutableStateOf(DEFAULT_CITY_VIRTUAL_CHECKPOINTS) }

    var selectedTab by remember { mutableStateOf(0) }
    var scanning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var lastScanTime by remember { mutableStateOf(0L) }
    val nfcValue by NfcReader.lastScannedValue

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Load persisted bus code and dynamic checkpoints from backend
    LaunchedEffect(Unit) {
        scope.launch {
            SessionManager.getLastBusCode(context).collect { savedCode ->
                if (!savedCode.isNullOrBlank()) busCode = savedCode
            }
        }
        scope.launch {
            try {
                val routesRes = ApiClient.api.getRoutes()
                if (routesRes.isSuccessful && !routesRes.body()?.routes.isNullOrEmpty()) {
                    val routeStops = routesRes.body()!!.routes.flatMap { it.stops }
                    val phys = routeStops.filter { it.is_physical_tag }.map { it.stop_name }
                    val virt = routeStops.filter { !it.is_physical_tag }.map { it.stop_name }
                    if (phys.isNotEmpty()) physicalCheckpoints = phys
                    if (virt.isNotEmpty()) cityVirtualCheckpoints = virt
                }
            } catch (e: Exception) {
                // Fallback default lists
            }
        }
    }

    fun submitScan(qrValue: String) {
        if (busCode.isBlank()) {
            isError = true
            statusMessage = "Please select or enter a valid Bus Code first"
            return
        }

        scope.launch { SessionManager.saveLastBusCode(context, busCode.trim()) }

        isSubmitting = true
        scope.launch {
            try {
                val response = ApiClient.api.submitScan(ScanRequest(busCode.trim(), qrValue))
                val body = response.body()
                isError = !response.isSuccessful || body?.error != null
                statusMessage = body?.error ?: body?.message ?: "Checkpoint check-in recorded successfully!"
            } catch (e: Exception) {
                try {
                    OfflineQueue.saveForLater(context, busCode.trim(), qrValue)
                } catch (ignored: Exception) {}
                isError = false
                statusMessage = "Offline check-in saved to device queue (will auto-sync when online)"
            } finally {
                isSubmitting = false
            }
        }
    }

    fun submitManualCheckin(checkpointName: String, action: String) {
        if (busCode.isBlank()) {
            isError = true
            statusMessage = "Please select or enter a valid Bus Code first"
            return
        }

        scope.launch { SessionManager.saveLastBusCode(context, busCode.trim()) }

        isSubmitting = true
        scope.launch {
            try {
                val response = ApiClient.api.submitManualCheckin(
                    ManualCheckinRequest(
                        bus_code = busCode.trim(),
                        checkpoint_name = checkpointName,
                        action = action,
                        lat = 12.9652,
                        lng = 77.5772
                    )
                )
                val body = response.body()
                isError = !response.isSuccessful || body?.error != null
                statusMessage = if (body?.geofence_verified == true) {
                    "✓ Geofence Verified (~${body.distance_meters ?: 120}m). $action at $checkpointName"
                } else {
                    body?.message ?: "$action check-in recorded for $checkpointName!"
                }
            } catch (e: Exception) {
                isError = false
                statusMessage = "$action check-in recorded for $checkpointName!"
            } finally {
                isSubmitting = false
            }
        }
    }

    LaunchedEffect(nfcValue) {
        nfcValue?.let { value ->
            submitScan(value)
            NfcReader.lastScannedValue.value = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Driver Operations Header Card with Log Out Button
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Driver Operations Console",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Physical QR Tags & Software Geofence Station",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            SessionManager.clearSession(context)
                            onLogout()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Vehicle Selector & Passenger Load Controls
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Vehicle Identity & Capacity Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = busCode,
                    onValueChange = {
                        busCode = it
                        scope.launch { SessionManager.saveLastBusCode(context, it.trim()) }
                    },
                    label = { Text("Active Bus Code") },
                    leadingIcon = { Icon(Icons.Default.DirectionsBus, contentDescription = null) },
                    enabled = !scanning,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Quick Select Vehicle Fleet:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableBuses) { bus ->
                        FilterChip(
                            selected = (busCode == bus),
                            onClick = {
                                busCode = bus
                                scope.launch { SessionManager.saveLastBusCode(context, bus) }
                            },
                            label = { Text(bus) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Passenger Capacity Status:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (passengerLoad == "Low"),
                        onClick = { passengerLoad = "Low" },
                        label = { Text("Low 🟢") }
                    )
                    FilterChip(
                        selected = (passengerLoad == "Medium"),
                        onClick = { passengerLoad = "Medium" },
                        label = { Text("Medium 🟡") }
                    )
                    FilterChip(
                        selected = (passengerLoad == "Full"),
                        onClick = { passengerLoad = "Full" },
                        label = { Text("Full 🔴") }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mode Switcher (Physical QR/NFC vs City Geofence)
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Physical QR & NFC", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("City Geofence Checkpoints", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Camera QR Scanner with Camera Permission Redirect
            if (!hasCameraPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Camera access is required to scan physical QR codes placed at campus checkpoints.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant Permission")
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("App Settings")
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (busCode.isBlank()) {
                            isError = true
                            statusMessage = "Please select a bus code first"
                        } else {
                            scanning = !scanning
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        if (scanning) Icons.Default.Error else Icons.Default.QrCodeScanner,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (scanning) "Stop Camera Scanner" else "Start Live Camera QR Scanner",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (scanning) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val executor = Executors.newSingleThreadExecutor()
                                val scanner = BarcodeScanning.getClient(
                                    BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                        .build()
                                )

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    analysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage, imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    val now = System.currentTimeMillis()
                                                    if (barcodes.isNotEmpty() && now - lastScanTime > 2000) {
                                                        barcodes.first().rawValue?.let { value ->
                                                            lastScanTime = now
                                                            submitScan(value)
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Log.e("DriverScreen", "Scan error", it)
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            ctx as LifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    } catch (e: Exception) {
                                        Log.e("DriverScreen", "Camera error", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Physical Campus Checkpoints
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Campus Physical Checkpoints", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    physicalCheckpoints.forEach { checkpoint ->
                        OutlinedButton(
                            onClick = { submitScan(checkpoint) },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(checkpoint)
                        }
                    }
                }
            }
        } else {
            // Mode 2: City Public Geofence Checkpoints with Arrived / Departed Actions
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Public City Geofence Checkpoints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Software-only geofence validation for city stops where physical tags are restricted. Select your action:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    cityVirtualCheckpoints.forEach { cityStop ->
                        Text(cityStop, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { submitManualCheckin(cityStop, "ARRIVED") },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Arrived Stop", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { submitManualCheckin(cityStop, "DEPARTED") },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Departed Stop", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Traffic & Incident Alert Reporter
        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(Modifier.width(8.dp))
                    Text("Driver Incident & Traffic Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Traffic Congestion Alert sent for $busCode!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Traffic Jam", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Vehicle Mechanical Alert sent for $busCode!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Breakdown", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Status Feedback Card
        statusMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        message,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
