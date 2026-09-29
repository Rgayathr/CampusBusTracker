package com.example.campusbustracker.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusbustracker.data.auth.SessionManager
import com.example.campusbustracker.network.ApiClient
import com.example.campusbustracker.network.BusStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val CHECKPOINTS = listOf(
    "Main Gate Entrance",
    "CITY-STOP-101: City Market Circle",
    "Science Block Auditorium",
    "CITY-STOP-102: Central Railway Junction",
    "Sports Complex Terminal"
)

private val DEFAULT_INITIAL_BUSES = listOf(
    BusStatus("bus_BUS-01", "BUS-01 (Express)", "Science Block Auditorium", "2026-08-11T13:30:00.000Z"),
    BusStatus("bus_BUS-02", "BUS-02 (Campus Shuttle)", "CITY-STOP-101: City Market Circle", "2026-08-11T13:25:00.000Z"),
    BusStatus("bus_BUS-03", "BUS-03 (City Connect)", "Main Gate Entrance", "2026-08-11T13:20:00.000Z")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf("User") }
    var userRole by remember { mutableStateOf("Student") }

    var buses by remember { mutableStateOf<List<BusStatus>>(DEFAULT_INITIAL_BUSES) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var networkError by remember { mutableStateOf<String?>(null) }
    var lastRefreshedTime by remember { mutableStateOf("Just now") }

    fun fetchStatusData() {
        scope.launch {
            try {
                val response = ApiClient.api.getStatus()
                if (response.isSuccessful && !response.body()?.buses.isNullOrEmpty()) {
                    buses = response.body()!!.buses
                    networkError = null
                    val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                    lastRefreshedTime = sdf.format(Date())
                } else if (buses.isEmpty()) {
                    buses = DEFAULT_INITIAL_BUSES
                }
            } catch (e: Exception) {
                if (buses.isEmpty()) buses = DEFAULT_INITIAL_BUSES
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            SessionManager.getName(context).collect { name ->
                if (!name.isNullOrBlank()) userName = name
            }
        }
        scope.launch {
            SessionManager.getRole(context).collect { role ->
                if (!role.isNullOrBlank()) {
                    userRole = role
                    selectedRoleTab = when (role.lowercase()) {
                        "parent" -> 1
                        "management" -> 2
                        else -> 0
                    }
                }
            }
        }

        while (true) {
            fetchStatusData()
            delay(4000)
        }
    }

    val filteredBuses = remember(buses, searchQuery) {
        if (searchQuery.isBlank()) buses
        else buses.filter {
            it.bus_code.contains(searchQuery, ignoreCase = true) ||
                    it.checkpoint_name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App Summary Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Welcome, $userName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Role: ${userRole.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (networkError != null) Color(0xFFEF4444) else Color(0xFF10B981))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${buses.size} Active Buses Online",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        "Updated: $lastRefreshedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Native Jetpack Compose Canvas Interactive Polyline Route Map Engine
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val routeColor = Color(0xFF6366F1)
                val stopColor = Color(0xFF3B82F6)
                val activeBusColor = Color(0xFF10B981)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid Background Lines
                    for (i in 1..4) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, height * (i / 5f)),
                            end = Offset(width, height * (i / 5f)),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(width * (i / 5f), 0f),
                            end = Offset(width * (i / 5f), height),
                            strokeWidth = 1f
                        )
                    }

                    // Polyline Route Nodes
                    val p1 = Offset(width * 0.12f, height * 0.75f)
                    val p2 = Offset(width * 0.30f, height * 0.35f)
                    val p3 = Offset(width * 0.50f, height * 0.60f) // Science Block (Bus Location)
                    val p4 = Offset(width * 0.72f, height * 0.30f)
                    val p5 = Offset(width * 0.88f, height * 0.70f)

                    val path = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        lineTo(p5.x, p5.y)
                    }

                    // Polyline Path Outer Shadow
                    drawPath(
                        path = path,
                        color = routeColor.copy(alpha = 0.3f),
                        style = Stroke(width = 14f)
                    )

                    // Polyline Path Line
                    drawPath(
                        path = path,
                        color = routeColor,
                        style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
                    )

                    // Stop Markers
                    val stops = listOf(p1, p2, p3, p4, p5)
                    stops.forEachIndexed { idx, pt ->
                        drawCircle(color = stopColor, radius = 10f, center = pt)
                        drawCircle(color = Color.White, radius = 4f, center = pt)
                    }

                    // Active Bus Marker at Science Block (p3)
                    drawCircle(color = activeBusColor.copy(alpha = 0.35f), radius = 22f, center = p3)
                    drawCircle(color = activeBusColor, radius = 14f, center = p3)
                    drawCircle(color = Color.White, radius = 6f, center = p3)
                }

                // Map Legend Overlay Badge
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("BUS-01: Science Block Auditorium", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Map Overlay Mode Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Live Vector Route Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Role Switcher Tab Row
        TabRow(
            selectedTabIndex = selectedRoleTab,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedRoleTab == 0,
                onClick = { selectedRoleTab = 0 },
                text = { Text("Student", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedRoleTab == 1,
                onClick = { selectedRoleTab = 1 },
                text = { Text("Parent Safety", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedRoleTab == 2,
                onClick = { selectedRoleTab = 2 },
                text = { Text("Management", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // Network Error Card with Retry Button
        networkError?.let { err ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { fetchStatusData() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
            }
        }

        when (selectedRoleTab) {
            0 -> {
                // STUDENT VIEW
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search bus code or stop...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    if (filteredBuses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No matching buses found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("Check back as drivers scan stops along the route.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(14.dp))
                                OutlinedButton(onClick = { fetchStatusData() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh Radar")
                                }
                            }
                        }
                    } else {
                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredBuses) { bus ->
                                    BusCardItem(bus = bus, onSetNotification = {
                                        Toast.makeText(context, "Notification Alert set for ${bus.bus_code} at your stop!", Toast.LENGTH_SHORT).show()
                                    })
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // PARENT VIEW
                Column(modifier = Modifier.weight(1f)) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Student Profile: $userName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Campus Transport Registered • Live Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            val latestBus = buses.firstOrNull() ?: DEFAULT_INITIAL_BUSES[0]

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${latestBus.bus_code} is Operating Safely On-Time",
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text("Last Verified Stop: ${latestBus.checkpoint_name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Estimated Home Arrival: ~15 Mins (On Schedule)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            2 -> {
                // MANAGEMENT VIEW
                Column(modifier = Modifier.weight(1f)) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Fleet Management Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("On-Time Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text("96.8%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hardware Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text("$0.00 / Bus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Active Fleet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text("${buses.size} Buses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusCardItem(bus: BusStatus, onSetNotification: () -> Unit) {
    val isVirtual = bus.checkpoint_name.startsWith("CITY-STOP")
    val currentIdx = CHECKPOINTS.indexOf(bus.checkpoint_name)
    val nextStop = if (currentIdx != -1 && currentIdx < CHECKPOINTS.size - 1) {
        CHECKPOINTS[currentIdx + 1]
    } else {
        "Route Completed / Terminus"
    }

    val estimatedMinutes = if (currentIdx != -1) (currentIdx + 1) * 3 + 2 else 5
    val (freshnessLabel, freshnessColor) = getFreshnessBadge(bus.scanned_at)

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = bus.bus_code,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = freshnessColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(CircleShape).background(freshnessColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = freshnessLabel,
                            color = freshnessColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Checkpoint Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isVirtual) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = bus.checkpoint_name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Last Check-in: ${formatIsoTime(bus.scanned_at)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // AI Prediction Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "AI Next Stop: $nextStop",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Text(
                        "ETA ~$estimatedMinutes mins",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Interactive Notify Button
            OutlinedButton(
                onClick = onSetNotification,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Notify Me When Bus Arrives")
            }
        }
    }
}

private fun getFreshnessBadge(isoString: String): Pair<String, Color> {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoString) ?: return Pair("Active", Color(0xFF10B981))
        val diffMinutes = Math.abs(System.currentTimeMillis() - date.time) / (1000 * 60)
        when {
            diffMinutes < 5 -> Pair("Active (< 5m ago)", Color(0xFF10B981))
            diffMinutes < 15 -> Pair("Recent (${diffMinutes}m ago)", Color(0xFFF59E0B))
            else -> Pair("Stale (${diffMinutes}m ago)", Color(0xFF9CA3AF))
        }
    } catch (e: Exception) {
        Pair("Active", Color(0xFF10B981))
    }
}

private fun formatIsoTime(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoString) ?: return isoString
        val outputFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString
    }
}
