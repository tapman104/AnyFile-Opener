# PROJECTBUILD.md - Architecture, Features, and Roadmap

This document provides a technical deep-dive into the "Open Bridge" (AnyFile Opener) project, detailing its internal structure, capabilities, and current development status.

---

## 🏗 Architecture & Design

The application follows a modular, logic-first architecture designed for speed and reliability in handling system-wide file intents.

### Core Modules
- **Transparent Routing (`MainActivity`)**: A zero-UI entry point that intercepts `ACTION_VIEW` intents. It instantly analyzes the incoming URI and presents the `OpenAsBottomSheet` for rapid redirection.
- **Detection Engine (`MimeDetector`)**: A singleton utility that performs binary signature analysis. 
    - **Buffer**: Reads up to 33KB (optimized to support ISO-9660 detection at offset 32769 and TAR at 257).
    - **Logic Hierarchy**: Magic-bytes → Script detection (Shebangs) → ZIP Deep peeking → Extension fallback → Structured text heuristics (JSON/XML/CSV).

- **Security & Bridging (`IntentRouter`)**: The critical layer that solves Android's URI permission "walled garden."
    - **Permission Forwarding**: Uses `ClipData` for modern intent standards.
    - **Pre-emptive Granting**: Manually grants `FLAG_GRANT_READ_URI_PERMISSION` to all resolved candidate applications before the chooser is shown to ensure zero "Access Denied" errors.
- **Primary Interface (`LauncherActivity`)**: A dashboard for manual operations, featuring recent files, system file picker shortcuts, and advanced inspection tools.
- **Data Persistence**: `RecentFileStore` manages a history of accessed files using JSON-based storage in internal preferences.

---

## ✨ Features

### 🔍 Intelligence & Detection
- **Multi-Format Magic Bytes**: Native detection for MKV, MP4, AVI, MP3, FLAC, JPG, PNG, PDF, ZIP, 7Z, and more.
- **ZIP Deep Peeking**: Can distinguish between generic ZIPs, APKs, EPUBs, and Microsoft Office documents (`.docx`, `.xlsx`, `.pptx`) by inspecting internal ZIP entry headers.
- **Script & Shebang Detection**: Identifies Python, Bash, Node.js, and Kotlin scripts by reading the `#!` shebang or code structure.
- **Structured Text Heuristics**: Automatically identifies JSON, XML, HTML, YAML, and CSV files by analyzing content patterns and character entropy.


### 🛡 Security & Interop
- **Universal ACTION_VIEW**: Registers for all MIME types (`*/*`) to act as a fallback when the system doesn't know what to do.
- **MIME Force-Override**: Allows users to manually specify a MIME type (e.g., forcing a `.bin` file to open as an `image/jpeg`).
- **Binary Inspector**: Real-time Hexadecimal and ASCII preview of file headers.

### 🚀 UX & Integration
- **Recent Files**: Quick access to previously opened files with timestamp tracking.
- **Folder Shortcut Widget**: A home-screen widget providing a 1-tap shortcut to the system's root internal storage.
- **Quick-Action Chips**: Rapid-select buttons for common formats like APK, JSON, PDF, and ZIP.
- **System Shortcut**: Direct link to the `DocumentsUI` root, bypassing simplified file pickers.
- **Theming Engine**: Supports System Default, Material You (Dynamic), and AMOLED Black themes.


---

## 📖 How it's Used

1. **Passive Flow**: 
   - Tap a file in your favorite File Manager.
   - Choose **AnyFile Opener** from the "Open with" list.
   - Select the target category or use the detected suggestion.

2. **Active Flow**:
   - Open **AnyFile Opener** from the app drawer.
   - Tap **PICK FILE** to browse storage.
   - Use **INSPECT BINARY** to verify the file's contents before opening.
   - Use **OPEN WITH FORCE** to bypass system associations.

3. **Widget**:
   - Long-press home screen → Widgets → AnyFile Opener.
   - Place the "Storage Root" widget for instant access to internal files.

---

## ✅ What it CAN do
- **Fix "No app can open this file"**: By providing a generic handler.
- **Fix Permission Denied**: By correctly forwarding URI grants from restricted locations (e.g., Telegram/Discord caches).
- **Identify Extensionless Files**: By reading magic bytes.
- **Recover Mislabelled Files**: Open a `.txt` that is actually a `.jpg` by forcing the image MIME.

## ❌ What it CANNOT do
- **Edit Files**: The app is strictly read-only and does not support `ACTION_EDIT`.
- **Modify File System**: It cannot rename, move, or delete files.
- **Handle Non-Seekable Streams**: Some rare network streams that don't allow partial reads may fail detection.
- **Access Private Data**: It cannot reach into other apps' private `/data/data/` folders (only standard `ContentProvider` URIs).

---

## 📈 Overall Progress

| Component | Status | Note |
| :--- | :--- | :--- |
| **MIME Detection** | 🟢 Stable | High accuracy; supports Scripts, ISO, and Office. |
| **Permission Bridging** | 🟢 Stable | Successfully handles restricted URI grants. |
| **UI/UX** | 🟢 Stable | Full Compose UI with Material 3. |
| **Recent Files** | 🟢 Stable | History tracking implemented. |
| **Widgets** | 🟢 Stable | Folder shortcut widget functional. |
| **Binary Inspector** | 🟢 Stable | Hex/ASCII view with offset support. |
| **Settings & Themes** | 🟢 Stable | Material You and AMOLED support. |


**Current Version**: Active Development (Pre-release)
**Build Status**: Passing (API 26-34)
