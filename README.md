# Pulse Music 🎵

![Pulse Music Logo](./app/src/main/ic_launcher-playstore.png)

**A resilient, high-performance Android music streaming engine & player built on modern Android standards.**

[![Latest Release](https://img.shields.io/github/v/release/Shivaanshe/PulsePlayer-Android?style=for-the-badge&color=4CAF50&label=Download%20APK)](https://github.com/Shivaanshe/PulsePlayer-Android/releases/latest)
[![Stars](https://img.shields.io/github/stars/Shivaanshe/PulsePlayer-Android?style=for-the-badge&color=00E676)](https://github.com/Shivaanshe/PulsePlayer-Android/stargazers)
[![Forks](https://img.shields.io/github/forks/Shivaanshe/PulsePlayer-Android?style=for-the-badge&color=2196F3)](https://github.com/Shivaanshe/PulsePlayer-Android/network/members)

---

## 🌟 What Pulse Serves

Pulse Music delivers a unified, high-fidelity music experience combining cloud streaming, local storage playback, and a modern glassmorphic interface.

### 📱 Key Features & Capabilities

- **Smart Discover & Cloud Streaming:** Instant YouTube & Spotify link extraction. Paste any track or playlist link to resolve and stream music on-demand without storage bloat.
- **Immersive Glassmorphic Playlist Experience:** Dedicated hero artwork headers with soft ambient glowing backdrops, instant $O(1)$ track count and total runtime calculation, and intuitive "Play All", "Shuffle", and "Add Tracks" controls.
- **My Library & Offline Media Hub:** Native Android storage indexer that merges downloaded tracks and local device audio files into one cohesive, zero-data offline catalog.
- **Glassmorphic Aesthetic & Modern UX:** Translucent smoked-glass containers, signature Pulse green accents (`#4CAF50`), Compose spring physics, interactive drag-and-drop playlist reordering, and a two-pane widescreen landscape layout with Navigation Rail.
- **Pulse Debugger & System Telemetry:** Real-time diagnostics monitor bandwidth, active extraction threads, LRU cache allocation, and background visualizer toggles for hardware efficiency.

---

## ⚡ Architecture Highlights

- **Just-In-Time (JIT) Resolution Pipeline (`ResolvingDataSource`):** Natively pauses network requests at the last millisecond to negotiate the source stream and authenticated headers, providing seamless zero-gap playback.
- **"Burner Thread" Watchdog Resilience:** Supervised coroutine extraction pipeline with automatic detached watchers and hard-kill protocols to prevent JNI/native extraction deadlocks.
- **Zero-Lag Metadata & Dynamic Theming:** Raw byte album art injection with instant lock screen and notification tint matching.

---

## 🛠️ Tech Stack

- **UI Framework:** Jetpack Compose (Material 3)
- **Audio Engine:** Media3 (ExoPlayer) / MediaSession
- **Database:** Room Persistence Library
- **Networking:** OkHttp3 / Retrofit2
- **Stream Extraction:** `youtubedl-android` (`yt-dlp`) / FFmpeg
- **Image Pipeline:** Coil
- **Concurrency:** Kotlin Coroutines & StateFlow / Flow Pipelines

---

## 📥 Download & Automated Updates

### 🚀 Download Latest Build
Get the latest stable APK directly from GitHub Releases:
👉 **[Download Pulse Music APK (Latest Release)](https://github.com/Shivaanshe/PulsePlayer-Android/releases/latest)**

### 🔄 Connected In-App OTA Updates
Pulse Music includes a built-in **Over-The-Air (OTA) Update System** (`OtaUpdateManager`) connected directly to the `Shivaanshe/PulsePlayer-Android` GitHub Releases repository:
- **Automated Update Checks:** Periodically checks GitHub Releases for new updates and version tags.
- **In-App Changelog & Progress:** Displays update notifications, version comparison, and release notes directly in the app.
- **One-Click Installation:** Automatically downloads the latest APK via Android DownloadManager and prompts for seamless in-app installation.

---

*Developed with a focus on stability, performance, and a premium Android music experience.*
