package com.example.wearwifitools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.wearwifitools.wifi.WifiDiagnosticData

@Composable
fun SignalScreen(
    data: WifiDiagnosticData,
    onRefresh: () -> Unit
) {
    val signalColor = when {
        !data.isConnected -> Color.Red
        data.rssi >= -60 -> Color(0xFF00E676) // Green
        data.rssi >= -75 -> Color(0xFFFFB300) // Amber
        else -> Color(0xFFD32F2F) // Red
    }

    val signalQuality = when {
        !data.isConnected -> "Offline"
        data.rssi >= -60 -> "Excellent"
        data.rssi >= -75 -> "Good"
        else -> "Weak"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Wi-Fi Signal Circle Indicator
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E24)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = (data.signalPercent / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 5.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (data.isConnected) "${data.rssi}" else "OFF",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (data.isConnected) "dBm" else "",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // SSID & Network Quality
            Text(
                text = data.ssid,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "$signalQuality • ${data.signalPercent}% Signal",
                color = signalColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Spec Info Box (Band, Channel, Speed, IP)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Band:", color = Color.Gray, fontSize = 9.sp)
                        Text("${data.bandStr} (Ch ${data.channel})", color = Color(0xFF81D4FA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Link Speed:", color = Color.Gray, fontSize = 9.sp)
                        Text("${data.linkSpeedMbps} Mbps", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("IP Addr:", color = Color.Gray, fontSize = 9.sp)
                        Text(data.ipAddress, color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Refresh Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1565C0))
                    .clickable { onRefresh() }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔄 Refresh Diagnostic", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
