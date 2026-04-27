# AnyFile Opener (OpenBridge) - Project Information

AnyFile Opener (internally named **OpenBridge**) is a specialized Android utility designed to solve the "Open with" problem. It acts as a universal `ACTION_VIEW` handler that accurately identifies file types using binary signature analysis and ensures target applications can access restricted files through robust URI permission forwarding.

---

## 🏗 Architecture & Design

The application is built with a modular, logic-first approach using Kotlin and Android SDK (API 26-34).

### Core Components
- **Transparent Router (`MainActivity`)**: A non-UI entry point that intercepts `ACTION_VIEW` intents. It immediately delegates the URI to the `OpenAsBottomSheet` to minimize user friction.
- **Detection Engine (`MimeDetector`)**: A powerful stateless utility that inspects file headers. It reads up to 33KB of a file to handle deep offsets (e.g., ISO-9660 at 32KB). It uses a hierarchy of:

    1. Magic-byte signature matching.
    2. Extension-based fallback.
    3. Heuristic text detection (printable character density check).
- **Intent Dispatcher (`IntentRouter`)**: The critical security/bridge layer. It handles the re-dispatching of files with `Intent.FLAG_GRANT_READ_URI_PERMISSION` and manual `grantUriPermission` calls for all candidate apps to prevent `SecurityException` on Android 10+.
- **Launcher Activity (`LauncherActivity`)**: Provides a full-featured UI for manual file selection, MIME type overrides, and quick access to common categories via chips.
- **Settings & Personalization (`SettingsActivity`)**: A dedicated screen for app configuration, including theme selection (Material You, AMOLED) and user guides.


### Data Flow
1. **Incoming**: `ACTION_VIEW` (from File Manager/Gallery) → `MainActivity`.
2. **Analysis**: `ByteReader` reads 32 bytes → `MimeDetector` returns `DetectionResult`.
3. **Selection**: User chooses a category in `OpenAsBottomSheet` (pre-selected based on detection).
4. **Outgoing**: `IntentRouter` grants permissions to target apps → `Intent.createChooser()` triggers the final app selection.

---

## ✨ Features

### 🔍 Advanced File Detection
- **Magic-Byte Analysis**: Supports video (MKV, MP4, AVI, FLV), audio (MP3, FLAC, OGG, WAV), images (JPEG, PNG, GIF, WEBP), and documents/archives.
- **MP4/3GP Parsing**: Inspects `ftyp` boxes to distinguish between MP4 video and M4A audio.
- **Text Heuristics**: If signatures fail, it calculates the ratio of printable ASCII characters to determine if a file should be opened as `text/plain`.

### 🛡 Robust Permission Forwarding
- **ClipData Propagation**: Uses `ClipData` (introduced in Android 4.1, mandatory for URI grants in modern Android) to ensure permissions persist across activity transitions.
- **Pre-emptive Granting**: Manually iterates through all apps capable of handling the target MIME and grants them URI read access via `PackageManager`, solving issues with apps like VLC or MX Player being unable to "see" files in private caches.

### 🛠 Utility Tools
- **Binary Inspector**: A low-level view of the file header showing Hexadecimal and ASCII representations.
- **Advanced MIME Override**: Allows users to type in any valid MIME string (e.g., `application/x-yaml`) for niche file types.
- **System File Manager Shortcut**: Force-launches the system `documentsui` (Files app) directly to the internal storage root, bypassing restricted "simplified" file pickers.
- **Quick Chips**: One-tap buttons for common formats like APK, JSON, PDF, and ZIP.
- **Modern UI & Themes**: Built with Jetpack Compose, supporting Material You dynamic coloring and a pure AMOLED black mode for power saving.


### 🍱 Widgets
- **Folder Shortcut Widget**: A home screen widget that provides a direct link to the primary storage root of the Android Documents provider.

---

## 🚀 What it Can Do
- **Open Hidden Files**: Open files without extensions by detecting their content.
- **Fix "Access Denied"**: Fix errors where an app says "File not found" or "Permission denied" when opened from Telegram or Discord.
- **Forced Installation**: Open `.bin` or `.cache` files as APKs to force-install them if they were renamed by a downloader.
- **Preview Binary**: Quickly check if a file is actually a PDF or just a renamed text file without opening it in a full editor.

## ⚠️ What it Can't Do (Limitations)
- **Modify Files**: OpenBridge is read-only. It does not support `ACTION_EDIT` or writing back to the URI.
- **Partial Headers**: If a file's magic bytes are further than the first 32 bytes (rare), detection may fall back to extension.
- **Streaming Restrictions**: Some HTTP/HTTPS streams cannot be read for magic bytes if the source doesn't support seeking or partial content.
- **Scoped Storage**: Cannot access files in another app's private `/data/data/` folder unless that app explicitly shares it via a ContentProvider.

---

## 🛠 Technical Implementation Details

### Permissions
- `READ_EXTERNAL_STORAGE`: Used for legacy file access.
- `MANAGE_DOCUMENTS`: Declared to interact with system file providers.

### The Kotlin Lexer Workaround
Due to a known issue in some Kotlin compilers (like 1.9.22), the literal token `/*` (slash-star) inside strings can be misinterpreted as a comment block. OpenBridge uses runtime string templates to avoid this:
```kotlin
val s = "/"
val mime = "video${s}*" // Prevents compiler error for */* 
```

### Persistence
Preferences are managed via `PrefsManager` using `SharedPreferences`, storing settings for:
- **Default MIME**: Fallback for unknown files.
- **Auto-Open**: Automatically launch the detected app if confidence is high.

---

## 📁 Project Structure Summary
com.openbridge
├── MainActivity.kt        # Transparent entry point for ACTION_VIEW
├── LauncherActivity.kt    # Main dashboard UI (Jetpack Compose)
├── SettingsActivity.kt    # App configuration & Themes
├── MimeDetector.kt        # Core detection engine (Magic bytes + Heuristics)
├── IntentRouter.kt        # Security, URI forwarding & Chooser logic
├── OpenAsBottomSheet.kt   # Category selection UI
├── InspectBottomSheet.kt  # Hex/ASCII binary viewer
├── PrefsManager.kt        # DataStore/SharedPreferences settings
├── RecentFileStore.kt     # JSON-based history management
├── FolderWidgetProvider.kt # Home screen storage shortcut
└── utils/
    └── ByteReader.kt      # Stream reading logic with offset support

