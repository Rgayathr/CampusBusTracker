package com.example.campusbustracker

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusbustracker.auth.LoginScreen
import com.example.campusbustracker.auth.SignupScreen
import com.example.campusbustracker.data.auth.SessionManager
import com.example.campusbustracker.management.AnalyticsScreen
import com.example.campusbustracker.nfc.NfcReader
import com.example.campusbustracker.ui.DashboardScreen
import com.example.campusbustracker.ui.DriverScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NfcReader.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        NfcReader.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED
        ) {
            val value = NfcReader.readTagValue(intent)
            if (value != null) {
                NfcReader.lastScannedValue.value = value
            }
        }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loggedIn by remember { mutableStateOf<Boolean?>(null) }
    var userRole by remember { mutableStateOf<String?>("student") }

    LaunchedEffect(Unit) {
        scope.launch { SessionManager.isLoggedIn(context).collect { loggedIn = it } }
        scope.launch { SessionManager.getRole(context).collect { userRole = it } }
    }

    if (loggedIn == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Strict Login Gate
    if (loggedIn == false) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { loggedIn = true },
                    onGoToSignup = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignupSuccess = { loggedIn = true },
                    onGoToLogin = { navController.popBackStack() }
                )
            }
        }
        return
    }

    // Strict Role-gated Navigation Rules
    val currentRole = userRole?.lowercase() ?: "student"
    val isDriverRole = currentRole == "driver"
    val isManagementRole = currentRole == "management"
    val initialDestination = if (isDriverRole) "driver" else "dashboard"

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination

                // 1. Dashboard Tab (Students, Parents, Management)
                if (!isDriverRole) {
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == "dashboard" } == true,
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Live Dashboard") },
                        label = { Text(if (currentRole == "parent") "Parent Monitor" else "Dashboard", fontWeight = FontWeight.SemiBold) }
                    )
                }

                // 2. Driver Console Tab (Driver Role ONLY)
                if (isDriverRole) {
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == "driver" } == true,
                        onClick = {
                            navController.navigate("driver") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Driver Console") },
                        label = { Text("Driver Station", fontWeight = FontWeight.SemiBold) }
                    )
                }

                // 3. Analytics Tab (Management ONLY)
                if (isManagementRole) {
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == "analytics" } == true,
                        onClick = {
                            navController.navigate("analytics") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                        label = { Text("Analytics", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = initialDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(onLogout = { loggedIn = false })
            }
            composable("driver") {
                DriverScreen(onLogout = { loggedIn = false })
            }
            composable("analytics") { AnalyticsScreen() }
        }
    }
}