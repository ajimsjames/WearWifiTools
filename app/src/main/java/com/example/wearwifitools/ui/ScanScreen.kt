package com.example.wearwifitools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.example.wearwifitools.wifi.DiscoveredDevice

@Composable
fun ScanScreen(
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    onStartScan: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 20.dp)
        ) {
            item {
                Text(
                    text = "🔍 Subnet LAN Scan",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            item {
                Text(
                    text = if (isScanning) "Scanning 1..254 (${devices.size} found)..." else "${devices.size} Active Hosts Found",
                    color = Color(0xFF81D4FA),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isScanning) Color(0xFF333333) else Color(0xFF1565C0))
                        .clickable(enabled = !isScanning) { onStartScan() }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning Subnet...", color = Color.LightGray, fontSize = 10.sp)
                        }
                    } else {
                        Text("🔍 Start LAN Scan", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!isScanning && devices.isEmpty()) {
                item {
                    Text(
                        text = "Tap button to scan local Wi-Fi subnet for active devices.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 16.dp, start = 12.dp, end = 12.dp)
                    )
                }
            } else {
                items(devices) { dev ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1E24))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dev.ip, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(dev.hostname, color = Color(0xFF90CAF9), fontSize = 8.sp)
                            }

                            Text("${dev.pingMs} ms", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
