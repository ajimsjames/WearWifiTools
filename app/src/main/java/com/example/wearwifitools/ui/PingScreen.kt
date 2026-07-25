package com.example.wearwifitools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.example.wearwifitools.wifi.WifiDiagnosticData

@Composable
fun PingScreen(
    data: WifiDiagnosticData,
    onRunPing: () -> Unit
) {
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
            Text(
                text = "⚡ Ping & Latency",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Live Latency Test",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Google DNS Ping Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Google DNS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("8.8.8.8", color = Color.Gray, fontSize = 9.sp)
                    }

                    Text(
                        text = if (data.pingGoogleMs >= 0) "${data.pingGoogleMs} ms" else "Timeout ❌",
                        color = if (data.pingGoogleMs in 0..100) Color(0xFF00E676) else if (data.pingGoogleMs > 100) Color(0xFFFFB300) else Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Router Gateway Ping Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Router Gateway", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(data.gatewayIp, color = Color.Gray, fontSize = 9.sp)
                    }

                    Text(
                        text = if (data.pingGatewayMs >= 0) "${data.pingGatewayMs} ms" else "Timeout ❌",
                        color = if (data.pingGatewayMs in 0..50) Color(0xFF00E676) else if (data.pingGatewayMs > 50) Color(0xFFFFB300) else Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Test Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF00E676))
                    .clickable { onRunPing() }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡ Test Latency Now", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
