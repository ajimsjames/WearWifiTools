package com.example.wearwifitools.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

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
            socket.connect(InetSocketAddress(host, port), 1200)
            socket.close()
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            // Try fallback ICMP ping check
            try {
                val inet = InetAddress.getByName(host)
                if (inet.isReachable(1000)) {
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
        val foundList = mutableListOf<DiscoveredDevice>()
        val dhcpInfo = wifiManager.dhcpInfo ?: return@withContext emptyList()
        val gatewayIpStr = Formatter.formatIpAddress(dhcpInfo.gateway)

        if (gatewayIpStr == "0.0.0.0" || !gatewayIpStr.contains(".")) {
            return@withContext emptyList()
        }

        val prefix = gatewayIpStr.substringBeforeLast(".") + "."

        // Fast scan subnet range (1 to 254)
        for (i in 1..254) {
            val targetIp = "$prefix$i"
            val ping = testPingHost(targetIp, 80)
            if (ping >= 0) {
                val dev = DiscoveredDevice(
                    ip = targetIp,
                    pingMs = ping,
                    hostname = if (targetIp == gatewayIpStr) "Router Gateway 🌐" else "Connected Host 💻"
                )
                foundList.add(dev)
                onDeviceFound(dev)
            }
        }
        foundList
    }
}
