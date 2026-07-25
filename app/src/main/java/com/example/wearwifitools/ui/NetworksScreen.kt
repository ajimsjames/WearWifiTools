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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.example.wearwifitools.wifi.NearbyWifiNetwork

@Composable
fun NetworksScreen(
    networks: List<NearbyWifiNetwork>,
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
            contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "Nearby Wi-Fi Networks",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            item {
                Text(
                    text = if (isScanning) "Scanning channels..." else if (networks.isEmpty()) "Tap Rescan to detect APs" else "Found ${networks.size} Access Points",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isScanning) Color(0xFF333333) else Color(0xFF1565C0))
                        .clickable(enabled = !isScanning) { onStartScan() }
                        .padding(vertical = 6.dp),
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
                            Text("Scanning APs...", color = Color.LightGray, fontSize = 10.sp)
                        }
                    } else {
                        Text("🔄 Scan Nearby Wi-Fi", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!isScanning && networks.isEmpty()) {
                item {
                    Text(
                        text = "No Wi-Fi networks found.\nEnsure Location permission is granted.",
                        color = Color.DarkGray,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(networks) { net ->
                    val signalColor = when {
                        net.rssi >= -60 -> Color(0xFF00E676) // Green
                        net.rssi >= -75 -> Color(0xFFFFD600) // Yellow
                        else -> Color(0xFFFF5252) // Red
                    }

                    val signalPercent = ((net.rssi + 100) * 2).coerceIn(0, 100)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = net.ssid,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "${net.rssi} dBm",
                                    color = signalColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ch ${net.channel} (${net.bandStr}) • ${net.capabilities}",
                                    color = Color(0xFF90CAF9),
                                    fontSize = 8.sp
                                )

                                Text(
                                    text = "$signalPercent%",
                                    color = Color.Gray,
                                    fontSize = 8.sp
                                )
                            }

                            // Signal level visual bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .padding(top = 3.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Color(0xFF333333))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(signalPercent / 100f)
                                        .fillMaxHeight()
                                        .background(signalColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
