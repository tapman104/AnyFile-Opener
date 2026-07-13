# AnyFile X - Project Gemini Modernization

![Project Banner](https://img.shields.io/badge/Project-AnyFile%20X-blueviolet?style=for-the-badge&logo=android)
![Version](https://img.shields.io/badge/Version-1.0.0--Stable-green?style=for-the-badge)
![UI](https://img.shields.io/badge/UI-Compose%20%2B%20Material%203-orange?style=for-the-badge&logo=jetpackcompose)

> **AnyFile X** (formerly Open Bridge / AnyFile Opener) is a high-performance, intent-driven Android file routing utility. It serves as the "Universal Translator" for the Android filesystem, ensuring that every file has a home and every intent finds its destination.

---

## 🚀 The Modernization Journey

Project Gemini represents a complete architectural overhaul and rebranding of the application.

### 💎 Rebranding & Identity

- **Namespace Migration**: Fully transitioned to the streamlined `com.anyfile.x` package namespace across all components, activities, and providers.
- **Visual Refresh**: Implementation of a modern Material 3 design system (`AnyFileOpenerTheme`) with full support for Dynamic Color (Material You) and pure black AMOLED Dark Mode.
- **Identity Unification**: Consolidated all launcher and settings flows under a unified Compose-driven experience, while preserving `MainActivity` as a high-speed, transparent intent gatekeeper.
- **Official Repository**: Synchronized and hosted at [AnyFile-Opener](https://github.com/tapman104/AnyFile-Opener.git).

### 🛠 Architecture & Tech Stack

- **Core Engine**: Binary-level MIME detection (`MimeDetector`) capable of deep-peeking ZIP structures (APK, EPUB, DOCX) and script heuristics (Shebangs) using deterministic memory buffering.
- **UI Framework**: Modern hybrid architecture combining **Jetpack Compose** (`LauncherScreen`, `SettingsScreen`) with high-performance **ViewBinding** bottom sheets (`InspectBottomSheet`, `OpenAsBottomSheet`) and lifecycle-aware coroutines (`lifecycleScope.launch(Dispatchers.IO)`).
- **Intent Routing**: Advanced URI permission bridging (`IntentRouter`) that solves the Android "Access Denied" bottleneck for cross-app file sharing from restricted cache folders.
- **Build Infrastructure**: Production-hardened Gradle configuration (`app/build.gradle.kts`) with externalized signing (`keystore.properties`) and graceful fallback for development builds.

---

## ✨ Key Features

### 🔍 Smart Detection (`MimeDetector`)
Unlike standard file managers, AnyFile X inspects the **Magic Bytes** of every file to identify its true nature, even when misnamed or lacking an extension.

### 🛡 Permission Bridging (`IntentRouter` & `MainActivity`)
Automatically handles the complex `FLAG_GRANT_READ_URI_PERMISSION` propagation, ensuring that target applications (media players, document viewers, package installers) have explicit permission to read the file URI.

### 📊 Advanced Binary Inspector (`InspectBottomSheet`)
A deep-dive tool into file metadata and structure:
- **Hex & ASCII Dump**: Real-time paginated viewing (`buildHexDump` & `buildAsciiPreview`) of file headers and raw byte contents.
- **Header Verification**: Compares magic-byte detection against filesystem metadata.
- **Quick Access**: One-tap button to jump directly to the containing folder of any inspected file.

### 🗂 Recent Files & Storage Shortcuts
- **Recent File History**: DataStore-backed storage (`RecentFileStore`) tracking recently analyzed files for quick re-opening.
- **Home Screen Widget**: A custom Android app widget (`FolderWidgetProvider`) offering one-tap access to system storage roots.

---

## 🛡️ Technical Safeguards & Stability

To ensure long-term reliability and prevent the "messed up" states typical of legacy intent-handling apps, AnyFile X implements several architectural "locks":

### 🧵 Lifecycle-Safe I/O
- **De-coupled Execution**: All heavy file analysis in `OpenAsBottomSheet` and `InspectBottomSheet` is performed using `lifecycleScope.launch(Dispatchers.IO)`.
- **Reference Safety**: To prevent `IllegalStateException` when a user closes the UI while a file is being read, background tasks use `applicationContext` and local copies of the `ContentResolver`. This "locks" the task to the application lifecycle rather than the transient UI lifecycle.

### 🧩 Intent Integrity
- **Transparent Bridge**: The `MainActivity` acts as a dedicated intent gatekeeper. It does not hold UI state, which ensures that complex URI data is passed cleanly to the Compose layer without being dropped during configuration changes (like screen rotation).
- **URI Permission Forwarding**: Implements a robust "Pre-emptive Granting" logic that manually propagates read permissions to target apps before the system chooser appears, eliminating common "Permission Denied" crashes.

### 🏗️ Build Hardening
- **Signature Security**: The signing configuration in `build.gradle.kts` defensively checks for the existence of `keystore.properties`. If missing, it gracefully skips signing rather than crashing the build process.

### 🔒 Signature Locking (Detection Engine)
- **Deep-Peek Verification**: To prevent "MIME-hijacking" or incorrect routing, the engine performs recursive inspection of ZIP structures. It "locks" the identification only after verifying internal manifests (e.g., `AndroidManifest.xml` for APKs), ensuring that a generic ZIP is never mistakenly opened as a specialized format.
- **Buffer Integrity**: Uses a fixed-size 33KB buffer for all detection tasks, ensuring deterministic memory usage and preventing OOM (Out Of Memory) errors during the analysis of massive multi-gigabyte files.

---

## 📈 Current Status

- [x] **Namespace Migration** (`com.anyfile.x`)
- [x] **Material 3 Implementation** (`AnyFileOpenerTheme`, Dynamic Color, AMOLED Dark Mode)
- [x] **Hybrid UI Architecture** (Jetpack Compose + ViewBinding BottomSheets)
- [x] **Lifecycle Stability Fixes** (Resolved `IllegalStateException` crashes via ApplicationContext decoupling)
- [x] **Binary Inspector UI** (Hex & ASCII dumps, pagination, metadata inspection)
- [x] **Quick Access Folder Routing** (One-tap containing folder navigation)
- [x] **Production Signing Configuration** (Graceful `keystore.properties` support)
- [x] **Repository Relocation** (Synchronized at `AnyFile-Opener`)

---

## 🗺 Roadmap (Gemini Next)

- [ ] **Batch Processing**: Simultaneous inspection and redirection of multiple files.
- [ ] **Custom MIME Rules**: User-definable magic byte rules and custom extension overrides.
- [x] **Quick Access**: Jump directly to the containing folder of any analyzed file with one tap.
- [x] **Hex & ASCII Inspector**: Native lightweight hex/ASCII preview inside the inspection sheet.

---

*Generated by Antigravity | Project Gemini 2026*
