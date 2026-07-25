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

    var selectedTab by remember { mutableStateOf(0) } // 0: Signal, 1: Nearby APs, 2: Ping, 3: LAN Scan
    var diagnosticData by remember { mutableStateOf(WifiDiagnosticData()) }
    var nearbyNetworks by remember { mutableStateOf(listOf<NearbyWifiNetwork>()) }
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
            nearbyNetworks = wifiHelper.getNearbyWifiNetworks()
            isApScanning = false
        }
    }

    // Auto-Refresh Signal Every 4 Seconds
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (true) {
            diagnosticData = wifiHelper.getDiagnosticData()
            delay(4000)
        }
    }

    // Auto-scan APs when tab 1 is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && nearbyNetworks.isEmpty()) {
            startApScan()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                if (event.verticalScrollPixels > 0) {
                    if (selectedTab < 3) selectedTab++
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
            1 -> NetworksScreen(
                networks = nearbyNetworks,
                isScanning = isApScanning,
                onStartScan = { startApScan() }
            )
            2 -> PingScreen(data = diagnosticData, onRunPing = { refreshData() })
            3 -> ScanScreen(
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
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            TabPill("📶 Signal", selected = selectedTab == 0) { selectedTab = 0 }
            TabPill("🌐 APs", selected = selectedTab == 1) { selectedTab = 1 }
            TabPill("⚡ Ping", selected = selectedTab == 2) { selectedTab = 2 }
            TabPill("🔍 LAN", selected = selectedTab == 3) { selectedTab = 3 }
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
            .background(if (selected) Color(0xFF1565C0) else Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
