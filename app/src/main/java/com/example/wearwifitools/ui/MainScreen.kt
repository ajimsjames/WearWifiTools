package com.example.wearwifitools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
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

    var selectedTab by remember { mutableStateOf(0) } // 0: Signal, 1: Ping, 2: Scan
    var diagnosticData by remember { mutableStateOf(WifiDiagnosticData()) }
    var discoveredDevices by remember { mutableStateOf(listOf<DiscoveredDevice>()) }
    var isScanning by remember { mutableStateOf(false) }

    fun refreshData() {
        coroutineScope.launch {
            diagnosticData = wifiHelper.getDiagnosticData()
        }
    }

    // Auto-Refresh Every 4 Seconds
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (true) {
            diagnosticData = wifiHelper.getDiagnosticData()
            delay(4000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                if (event.verticalScrollPixels > 0) {
                    if (selectedTab < 2) selectedTab++
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
            1 -> PingScreen(data = diagnosticData, onRunPing = { refreshData() })
            2 -> ScanScreen(
                devices = discoveredDevices,
                isScanning = isScanning,
                onStartScan = {
                    isScanning = true
                    discoveredDevices = emptyList()
                    coroutineScope.launch {
                        val found = wifiHelper.scanLocalSubnet { dev ->
                            discoveredDevices = discoveredDevices + dev
                        }
                        isScanning = false
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
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabPill("📶 Signal", selected = selectedTab == 0) { selectedTab = 0 }
            TabPill("⚡ Ping", selected = selectedTab == 1) { selectedTab = 1 }
            TabPill("🔍 Scan", selected = selectedTab == 2) { selectedTab = 2 }
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
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
