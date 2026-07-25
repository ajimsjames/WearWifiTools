package com.example.wearwifitools.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.example.wearwifitools.wifi.CompassSensorHelper
import com.example.wearwifitools.wifi.WifiDiagnosticData
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    data: WifiDiagnosticData
) {
    val context = LocalContext.current
    val compassHelper = remember { CompassSensorHelper(context) }
    var currentHeading by remember { mutableStateOf(0f) }

    // Map storing highest RSSI recorded per 45° sector (8 sectors)
    val sectorRssi = remember { mutableStateMapOf<Int, Int>() }
    var peakSector by remember { mutableStateOf<Int?>(null) }

    // Listen to compass orientation sensor
    LaunchedEffect(Unit) {
        compassHelper.getHeadingFlow().collectLatest { azimuth ->
            currentHeading = azimuth

            // Sample current RSSI at current compass azimuth
            if (data.isConnected && data.rssi > -99) {
                val sector = ((azimuth + 22.5f) % 360f / 45f).toInt()
                val prevRssi = sectorRssi[sector] ?: -100
                if (data.rssi > prevRssi) {
                    sectorRssi[sector] = data.rssi
                }

                // Find sector with maximum RSSI
                val maxEntry = sectorRssi.maxByOrNull { it.value }
                if (maxEntry != null && maxEntry.value > -95) {
                    peakSector = maxEntry.key
                }
            }
        }
    }

    // Calculated target direction in degrees (Center of peak sector: 0°, 45°, 90°, etc.)
    val targetAzimuth = remember(peakSector) {
        peakSector?.let { it * 45f } ?: 0f
    }

    // Relative angle between watch's current heading and target direction
    val relativeAngle = remember(currentHeading, targetAzimuth) {
        (targetAzimuth - currentHeading + 360f) % 360f
    }

    // Trigger subtle vibration when facing target direction (within ±22.5°)
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(relativeAngle) {
        if (peakSector != null && (relativeAngle < 25f || relativeAngle > 335f)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            } catch (e: Exception) {}
        }
    }

    val directionHint = when {
        peakSector == null -> "Turn 360° to scan signal"
        relativeAngle in 337.5..360.0 || relativeAngle in 0.0..22.5 -> "Straight Ahead ⬆️"
        relativeAngle in 22.5..67.5 -> "Slight Right ↗️"
        relativeAngle in 67.5..112.5 -> "To your Right ➡️"
        relativeAngle in 112.5..157.5 -> "Behind to Right ↘️"
        relativeAngle in 157.5..202.5 -> "Turn Around ⬇️"
        relativeAngle in 202.5..247.5 -> "Behind to Left ↙️"
        relativeAngle in 247.5..292.5 -> "To your Left ⬅️"
        else -> "Slight Left ↖️"
    }

    val proximityStatus = when {
        !data.isConnected -> "Offline ❌"
        data.rssi >= -55 -> "🔥 HOT / VERY CLOSE"
        data.rssi >= -70 -> "🟢 WARM / APPROACHING"
        else -> "🧊 COLD / FAR AWAY"
    }

    val proximityColor = when {
        !data.isConnected -> Color.Red
        data.rssi >= -55 -> Color(0xFFFF5252)
        data.rssi >= -70 -> Color(0xFF00E676)
        else -> Color(0xFF81D4FA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(38.dp))

            Text(
                text = "🧭 Wi-Fi Direction Radar",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = proximityStatus,
                color = proximityColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Radar Compass Dial Graphic
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 4.dp.toPx()

                    // Draw outer radar circle
                    drawCircle(
                        color = Color(0xFF22222B),
                        radius = radius,
                        center = center
                    )

                    // Draw radar crosshairs
                    drawLine(
                        color = Color(0xFF333344),
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y + radius),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF333344),
                        start = Offset(center.x - radius, center.y),
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw rotating compass arrow pointing to router relative angle
                    rotate(degrees = relativeAngle, pivot = center) {
                        val path = Path().apply {
                            moveTo(center.x, center.y - radius + 6.dp.toPx())
                            lineTo(center.x - 8.dp.toPx(), center.y + 10.dp.toPx())
                            lineTo(center.x, center.y + 4.dp.toPx())
                            lineTo(center.x + 8.dp.toPx(), center.y + 10.dp.toPx())
                            close()
                        }
                        drawPath(path, color = Color(0xFF00E676))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (data.isConnected) "${data.rssi}" else "--",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("dBm", color = Color.Gray, fontSize = 8.sp)
                }
            }

            Text(
                text = directionHint,
                color = Color(0xFF81D4FA),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rescan / Recalibrate Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1565C0))
                    .clickable {
                        sectorRssi.clear()
                        peakSector = null
                    }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔄 Calibrate Sweep", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
