package com.example.wearwifitools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Text
import com.example.wearwifitools.wifi.DiscoveredDevice
import com.example.wearwifitools.wifi.NearbyWifiNetwork
import com.example.wearwifitools.wifi.WifiDiagnosticData
import com.example.wearwifitools.wifi.WifiManagerHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val wifiHelper = remember { WifiManagerHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var selectedTab by remember { mutableStateOf(0) } // 0: Signal, 1: Radar, 2: Nearby APs, 3: Ping, 4: LAN Scan, 5: About
    var diagnosticData by remember { mutableStateOf(WifiDiagnosticData()) }
    var nearbyNetworks by remember { mutableStateOf(listOf<NearbyWifiNetwork>()) }
    var selectedTargetAp by remember { mutableStateOf<NearbyWifiNetwork?>(null) }

    var isApScanning by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf(listOf<DiscoveredDevice>()) }
    var isLanScanning by remember { mutableStateOf(false) }

    fun refreshData() {
        coroutineScope.launch {
            diagnosticData = wifiHelper.getDiagnosticData()
        }
    }

    fun startApScan() {
        if (isApScanning) return
        isApScanning = true
        coroutineScope.launch {
            val scanned = wifiHelper.getNearbyWifiNetworks()
            nearbyNetworks = scanned

            selectedTargetAp?.let { target ->
                val match = scanned.find { it.bssid == target.bssid }
                if (match != null) {
                    selectedTargetAp = match
                }
            }

            isApScanning = false
        }
    }

    // High-Speed Signal Refresh Loop
    LaunchedEffect(selectedTab, selectedTargetAp) {
        focusRequester.requestFocus()
        while (true) {
            diagnosticData = wifiHelper.getDiagnosticData()

            if (selectedTargetAp != null) {
                val scanned = wifiHelper.getNearbyWifiNetworks()
                val match = scanned.find { it.bssid == selectedTargetAp?.bssid }
                if (match != null) {
                    selectedTargetAp = match
                }
            }

            val interval = if (selectedTab == 0 || selectedTab == 1) 400L else 2000L
            delay(interval)
        }
    }

    // Auto-scan APs when tab 2 is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 && nearbyNetworks.isEmpty()) {
            startApScan()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                if (event.verticalScrollPixels > 0) {
                    if (selectedTab < 5) selectedTab++
                } else if (event.verticalScrollPixels < 0) {
                    if (selectedTab > 0) selectedTab--
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        // Tab Content
        when (selectedTab) {
            0 -> SignalScreen(data = diagnosticData, onRefresh = { refreshData() })
            1 -> RadarScreen(
                data = diagnosticData,
                targetAp = selectedTargetAp,
                onClearTarget = { selectedTargetAp = null }
            )
            2 -> NetworksScreen(
                networks = nearbyNetworks,
                selectedTargetBssid = selectedTargetAp?.bssid,
                isScanning = isApScanning,
                onStartScan = { startApScan() },
                onSelectTargetAp = { ap ->
                    selectedTargetAp = ap
                    selectedTab = 1
                }
            )
            3 -> PingScreen(data = diagnosticData, onRunPing = { refreshData() })
            4 -> ScanScreen(
                devices = discoveredDevices,
                isScanning = isLanScanning,
                onStartScan = {
                    isLanScanning = true
                    discoveredDevices = emptyList()
                    coroutineScope.launch {
                        wifiHelper.scanLocalSubnet { dev ->
                            discoveredDevices = discoveredDevices + dev
                        }
                        isLanScanning = false
                    }
                }
            )
            5 -> AboutScreen()
        }

        // Curved Bezel Top Navigation Bar
        CurvedLayout(
            anchor = 270f,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                TabPill("📶 Sig", selected = selectedTab == 0) { selectedTab = 0 }
            }
            curvedComposable {
                Spacer(modifier = Modifier.width(2.dp))
            }
            curvedComposable {
                TabPill("🧭 Radar", selected = selectedTab == 1) { selectedTab = 1 }
            }
            curvedComposable {
                Spacer(modifier = Modifier.width(2.dp))
            }
            curvedComposable {
                TabPill("🌐 APs", selected = selectedTab == 2) { selectedTab = 2 }
            }
            curvedComposable {
                Spacer(modifier = Modifier.width(2.dp))
            }
            curvedComposable {
                TabPill("⚡ Ping", selected = selectedTab == 3) { selectedTab = 3 }
            }
            curvedComposable {
                Spacer(modifier = Modifier.width(2.dp))
            }
            curvedComposable {
                TabPill("🔍 LAN", selected = selectedTab == 4) { selectedTab = 4 }
            }
            curvedComposable {
                Spacer(modifier = Modifier.width(2.dp))
            }
            curvedComposable {
                TabPill("⚙️ About", selected = selectedTab == 5) { selectedTab = 5 }
            }
        }

        // Bottom Author Credits
        Text(
            text = "By Aju George",
            color = Color.DarkGray,
            fontSize = 7.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        )
    }
}

@Composable
fun TabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF0288D1) else Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 7.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
