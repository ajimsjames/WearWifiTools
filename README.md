# 📶 Wear Wi-Fi Tools (v1.5.0)

**Real-Time Wi-Fi Signal Radar, Multi-AP Locator & LAN Scanner for Wear OS (Samsung Galaxy Watch 6)**

Developed by **Aju George**.

---

## ✨ Features

- ⚡ **High-Speed 400ms Signal Polling**: Samples Wi-Fi RSSI at 2.5 Hz (400ms interval) on Signal and Radar screens for instantaneous direction locking as you turn your wrist.
- 🧭 **360° Compass Wi-Fi Signal Radar**: Displays a green directional compass arrow tracking signal strength peaks around your body.
- 🌐 **Multi-AP Target Router Locator (`🧭 Locate AP`)**: Select any nearby Wi-Fi network or mesh node (via BSSID/MAC) to track its physical direction, even if unconnected!
- ⚡ **Latency & Packet Loss Ping Monitor**: Real-time ICMP ping tool measuring round-trip latency to gateway and DNS servers.
- 🔍 **Subnet LAN IP Device Scanner**: Multithreaded IP scanner discovering all active devices on your local Wi-Fi network.
- ⭕ **Samsung One UI Watch Design & Bezel Navigation**: Curved top navigation bar (`CurvedLayout`) with About App screen and One UI squircle launcher icon.

---

## 🛠️ Architecture & Tech Stack

- **Framework**: Android Wear OS (Min SDK 30 / Target SDK 33)
- **UI Engine**: Wear Compose + Jetpack Compose + CurvedLayout
- **Networking & Sensors**: Android WifiManager + LocationManager + Hardware Compass Azimuth Sensor.

---

## 📦 Installation

```bash
# Connect to Galaxy Watch 6 via Wireless ADB
adb connect <WATCH_IP>:<PORT>

# Build and Install Release APK
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License & Credits

Created and maintained by **Aju George**. Distributed for Wear OS devices.
