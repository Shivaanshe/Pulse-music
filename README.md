<div align="center">

  # Pulse Music 🎵

  <img src="app/src/main/ic_launcher-playstore.png" width="140" alt="Pulse Music Logo" />

  <p><b>A resilient, high-performance Android music streaming engine & player built on modern Android standards.</b></p>

  <p>
    <a href="https://github.com/Shivaanshe/PulsePlayer-Android/releases/latest">
      <img src="https://img.shields.io/github/v/release/Shivaanshe/PulsePlayer-Android?style=for-the-badge&color=4CAF50&label=Download%20APK" alt="Latest Release" />
    </a>
    <a href="https://github.com/Shivaanshe/PulsePlayer-Android/stargazers">
      <img src="https://img.shields.io/github/stars/Shivaanshe/PulsePlayer-Android?style=for-the-badge&color=00E676" alt="Stars" />
    </a>
    <a href="https://github.com/Shivaanshe/PulsePlayer-Android/network/members">
      <img src="https://img.shields.io/github/forks/Shivaanshe/PulsePlayer-Android?style=for-the-badge&color=2196F3" alt="Forks" />
    </a>
  </p>

</div>

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

## 📖 How to Use & Features Guide

### 1. Discover Screen (Smart Cloud Streaming)
- **Paste & Save Links:** You can paste any YouTube or Spotify song or playlist link directly into the Discover screen.
- **What Happens When Saved:** Pulse extracts and saves only lightweight metadata (title, artist, cover art, and source link) to your local database. No bulky audio files are downloaded, preserving your device storage.
- **On-Demand JIT Streaming:** When you tap play, Pulse's Just-In-Time (JIT) Resolution Engine dynamically extracts the direct high-quality audio stream in milliseconds, buffering it temporarily in memory. Temporary stream buffers recycle automatically so your phone stays light and fast.

### 2. My Library Screen (Offline Downloads & Local Storage)
- **Permanent Offline Hub:** Tracks you explicitly choose to download from Discover, as well as local audio files stored on your device, appear in My Library.
- **Zero-Data Playback:** Local songs bypass network extraction entirely and start playing instantly with zero latency and zero mobile data usage.
- **Unified Catalog:** Pulse automatically scans and indexes device storage, merging local MP3s and downloaded tracks into one seamless library.

### 3. Playlists & Collections Management
- **Immersive Playlist Header:** View playlist collections with hero artwork, dynamic ambient glows, and instant $O(1)$ track count & runtime calculations.
- **Control Bar:** Use the glowing **Play All** button for instant playback, **Shuffle** for randomized listening, or the **`+` (Add Tracks)** button to quickly add Recommended tracks into your collection.
- **Drag-and-Drop Reordering:** Enable Arrange Mode to reorder tracks dynamically using smooth spring physics animations.

### 4. System Settings, Debugger & In-App OTA Updates
- **Pulse Debugger:** Built-in telemetry lets you monitor LRU memory cache, active extraction threads, and toggle background visualizer orbs for hardware efficiency on low-end devices.
- **1-Click OTA Updates:** Check for updates directly inside the app. Pulse connects to GitHub Releases, notifies you when a new version is available, and lets you download and install updates in one tap.

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
