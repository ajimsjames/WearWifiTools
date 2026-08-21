package com.example.wearwifitools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.wear.compose.material.CircularProgressIndicator

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

    var selectedTab by remember { mutableStateOf(0) } // 0: Signal, 1: Radar, 2: APs, 3: Speedtest, 4: Ping, 5: About
    var diagnosticData by remember { mutableStateOf(WifiDiagnosticData()) }
    var nearbyNetworks by remember { mutableStateOf(listOf<NearbyWifiNetwork>()) }
    var selectedTargetAp by remember { mutableStateOf<NearbyWifiNetwork?>(null) }

    var isApScanning by remember { mutableStateOf(false) }
    var isSpeedtesting by remember { mutableStateOf(false) }
    var speedtestMbps by remember { mutableStateOf(0f) }

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
                if (match != null) selectedTargetAp = match
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
                if (match != null) selectedTargetAp = match
            }
            val interval = if (selectedTab == 0 || selectedTab == 1) 400L else 2000L
            delay(interval)
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 && nearbyNetworks.isEmpty()) {
            startApScan()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E11))
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
            3 -> {
                // Speedtest Screen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 38.dp, bottom = 44.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("🚀 Wi-Fi Speedtest", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Box(
                            modifier = Modifier.size(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSpeedtesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    indicatorColor = Color(0xFF00E5FF),
                                    trackColor = Color(0x3300E5FF),
                                    strokeWidth = 6.dp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (speedtestMbps > 0) "%.1f".format(speedtestMbps) else "--",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Mbps", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSpeedtesting) Color(0xFF21242D) else Color(0xFF00E5FF))
                                .clickable {
                                    if (!isSpeedtesting) {
                                        isSpeedtesting = true
                                        speedtestMbps = 0f
                                        coroutineScope.launch {
                                            val finalSpd = wifiHelper.runSpeedtest { current ->
                                                speedtestMbps = current
                                            }
                                            speedtestMbps = finalSpd
                                            isSpeedtesting = false
                                        }
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSpeedtesting) "Testing Speed..." else "▶ Start Speedtest",
                                color = if (isSpeedtesting) Color.LightGray else Color.Black,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            4 -> PingScreen(data = diagnosticData, onRunPing = { refreshData() })
            5 -> {
                // About Screen with Version & Changelog
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 38.dp, bottom = 44.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text("⚙️ About App", color = Color(0xFFFFAB00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF16181D))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📶 WearWifiTools", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Version 2.3.0 (Code 11)", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Changelog v2.3.0:", color = Color(0xFFFFAB00), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            Text("• Material 3 OLED Bezel-Aligned HUD", color = Color.LightGray, fontSize = 8.sp)
                            Text("• Built-in Wi-Fi Speedtest Meter", color = Color.LightGray, fontSize = 8.sp)
                            Text("• Subnet Scanner & Gateway Ping", color = Color.LightGray, fontSize = 8.sp)
                            Text("• Rotary Bezel navigation", color = Color.LightGray, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        // Top Curved Bezel Title / Status
        CurvedLayout(
            anchor = 270f, // 270° = Top Apex
            anchorType = androidx.wear.compose.foundation.AnchorType.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE16181D))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "📶 Signal • ${diagnosticData.rssi} dBm"
                            1 -> "🧭 Radar Locater"
                            2 -> "🌐 Nearby APs (${nearbyNetworks.size})"
                            3 -> "🚀 Speedtest"
                            4 -> "⚡ Ping / Latency"
                            else -> "⚙️ About v2.3.0"
                        },
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Curved Bezel Navigation Buttons
        CurvedLayout(
            anchor = 90f, // 90° = Bottom Apex
            anchorType = androidx.wear.compose.foundation.AnchorType.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                WifiTabPill("Sig", selected = selectedTab == 0) { selectedTab = 0 }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                WifiTabPill("Radar", selected = selectedTab == 1) { selectedTab = 1 }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                WifiTabPill("APs", selected = selectedTab == 2) { selectedTab = 2 }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                WifiTabPill("Speed", selected = selectedTab == 3) { selectedTab = 3 }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                WifiTabPill("Ping", selected = selectedTab == 4) { selectedTab = 4 }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                WifiTabPill("Info", selected = selectedTab == 5) { selectedTab = 5 }
            }
        }
    }
}

@Composable
fun WifiTabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF00E5FF) else Color(0xEE21242D))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color(0xFFB0B3B8),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


