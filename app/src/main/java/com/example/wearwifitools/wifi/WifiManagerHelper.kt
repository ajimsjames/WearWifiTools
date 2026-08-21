package com.example.wearwifitools.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections

data class WifiDiagnosticData(
    val isConnected: Boolean = false,
    val ssid: String = "Disconnected",
    val bssid: String = "--",
    val rssi: Int = -100,
    val signalPercent: Int = 0,
    val linkSpeedMbps: Int = 0,
    val frequencyMhz: Int = 0,
    val bandStr: String = "--",
    val channel: Int = 0,
    val ipAddress: String = "0.0.0.0",
    val gatewayIp: String = "0.0.0.0",
    val pingGoogleMs: Long = -1,
    val pingGatewayMs: Long = -1,
    val packetLossPercent: Int = 0
)

data class DiscoveredDevice(
    val ip: String,
    val pingMs: Long,
    val hostname: String = "Active Device"
)

data class NearbyWifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val bandStr: String,
    val channel: Int,
    val capabilities: String
)

class WifiManagerHelper(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    suspend fun getDiagnosticData(): WifiDiagnosticData = withContext(Dispatchers.IO) {
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo
        val dhcpInfo = wifiManager.dhcpInfo

        val isConnected = wifiInfo != null && wifiInfo.networkId != -1
        if (!isConnected || wifiInfo == null) {
            return@withContext WifiDiagnosticData()
        }

        var rawSsid = wifiInfo.ssid ?: "Unknown Wi-Fi"
        if (rawSsid.startsWith("\"") && rawSsid.endsWith("\"")) {
            rawSsid = rawSsid.substring(1, rawSsid.length - 1)
        }

        val rssi = wifiInfo.rssi
        val signalPercent = WifiManager.calculateSignalLevel(rssi, 100)
        val linkSpeed = wifiInfo.linkSpeed
        val freq = wifiInfo.frequency

        val (bandStr, channel) = parseFrequencyAndChannel(freq)
        val ipAddress = Formatter.formatIpAddress(wifiInfo.ipAddress)
        val gatewayIp = Formatter.formatIpAddress(dhcpInfo?.gateway ?: 0)
        val bssid = wifiInfo.bssid ?: "--"

        // Perform fast ping tests
        val pingGoogle = testPingHost("8.8.8.8", 80)
        val pingGateway = if (gatewayIp != "0.0.0.0") testPingHost(gatewayIp, 80) else -1L

        WifiDiagnosticData(
            isConnected = true,
            ssid = rawSsid,
            bssid = bssid,
            rssi = rssi,
            signalPercent = signalPercent,
            linkSpeedMbps = linkSpeed,
            frequencyMhz = freq,
            bandStr = bandStr,
            channel = channel,
            ipAddress = ipAddress,
            gatewayIp = gatewayIp,
            pingGoogleMs = pingGoogle,
            pingGatewayMs = pingGateway,
            packetLossPercent = if (pingGoogle < 0) 100 else 0
        )
    }

    @Suppress("DEPRECATION")
    suspend fun getNearbyWifiNetworks(): List<NearbyWifiNetwork> = withContext(Dispatchers.IO) {
        try {
            wifiManager.startScan()
            val results: List<ScanResult>? = wifiManager.scanResults
            if (results.isNullOrEmpty()) return@withContext emptyList()

            results.map { res ->
                var ssid = res.SSID ?: "Hidden Network"
                if (ssid.isEmpty()) ssid = "Hidden Network"
                val (bandStr, channel) = parseFrequencyAndChannel(res.frequency)
                val security = parseCapabilities(res.capabilities)

                NearbyWifiNetwork(
                    ssid = ssid,
                    bssid = res.BSSID ?: "--",
                    rssi = res.level,
                    frequencyMhz = res.frequency,
                    bandStr = bandStr,
                    channel = channel,
                    capabilities = security
                )
            }.sortedByDescending { it.rssi }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseCapabilities(cap: String?): String {
        if (cap.isNullOrEmpty()) return "Open"
        return when {
            cap.contains("WPA3", ignoreCase = true) -> "WPA3"
            cap.contains("WPA2", ignoreCase = true) -> "WPA2"
            cap.contains("WPA", ignoreCase = true) -> "WPA"
            cap.contains("WEP", ignoreCase = true) -> "WEP"
            cap.contains("ESS", ignoreCase = true) -> "Open"
            else -> "Open"
        }
    }

    private fun parseFrequencyAndChannel(freqMhz: Int): Pair<String, Int> {
        return when {
            freqMhz in 2412..2484 -> {
                val chan = if (freqMhz == 2484) 14 else (freqMhz - 2412) / 5 + 1
                Pair("2.4 GHz", chan)
            }
            freqMhz in 5170..5825 -> {
                val chan = (freqMhz - 5000) / 5
                Pair("5 GHz", chan)
            }
            freqMhz > 5925 -> {
                Pair("6 GHz", (freqMhz - 5950) / 5 + 1)
            }
            else -> Pair("Wi-Fi", 0)
        }
    }

    private fun testPingHost(host: String, port: Int): Long {
        val startTime = System.currentTimeMillis()
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 400)
            socket.close()
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            try {
                val inet = InetAddress.getByName(host)
                if (inet.isReachable(300)) {
                    System.currentTimeMillis() - startTime
                } else {
                    -1L
                }
            } catch (e2: Exception) {
                -1L
            }
        }
    }

    suspend fun scanLocalSubnet(onDeviceFound: (DiscoveredDevice) -> Unit): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val foundList = Collections.synchronizedList(mutableListOf<DiscoveredDevice>())
        val dhcpInfo = wifiManager.dhcpInfo ?: return@withContext emptyList()
        val gatewayIpStr = Formatter.formatIpAddress(dhcpInfo.gateway)

        if (gatewayIpStr == "0.0.0.0" || !gatewayIpStr.contains(".")) {
            return@withContext emptyList()
        }

        val prefix = gatewayIpStr.substringBeforeLast(".") + "."

        // Parallel multi-threaded scan (30 concurrent async tasks)
        coroutineScope {
            (1..254).chunked(25).map { chunk ->
                async(Dispatchers.IO) {
                    for (i in chunk) {
                        val targetIp = "$prefix$i"
                        val ping = testPingHost(targetIp, 80)
                        if (ping >= 0) {
                            val dev = DiscoveredDevice(
                                ip = targetIp,
                                pingMs = ping,
                                hostname = if (targetIp == gatewayIpStr) "Router Gateway 🌐" else "Connected Host 💻"
                            )
                            foundList.add(dev)
                            withContext(Dispatchers.Main) {
                                onDeviceFound(dev)
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        foundList
    }

    suspend fun runSpeedtest(onProgress: (Float) -> Unit): Float = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val startTime = System.currentTimeMillis()
        try {
            val url = java.net.URL("https://speed.cloudflare.com/__down?bytes=5000000")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.connect()
            if (conn.responseCode == 200) {
                val input = conn.inputStream
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                    if (elapsedSec > 0) {
                        val currentMbps = ((totalBytes * 8.0) / (elapsedSec * 1000000.0)).toFloat()
                        withContext(Dispatchers.Main) { onProgress(currentMbps) }
                    }
                    if (System.currentTimeMillis() - startTime > 6000) break
                }
                input.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val totalSec = ((System.currentTimeMillis() - startTime) / 1000.0).coerceAtLeast(0.1)
        val finalMbps = ((totalBytes * 8.0) / (totalSec * 1000000.0)).toFloat()
        return@withContext finalMbps
    }
}

