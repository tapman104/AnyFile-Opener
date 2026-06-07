# 📂 AnyFile X - Source Files Manifest & Architectural Guide

Welcome to the **AnyFile X** Source Manifest. This document lists every file residing within the `src` directory, defining its location, architectural role, and primary features.

---

## 🗺️ Source Directory Structure

Here is a visual overview of the `app/src` directory hierarchy:

```
app/src
├── main/
│   ├── AndroidManifest.xml
│   ├── java/com/anyfile/x/
│   │   ├── utils/
│   │   │   └── ByteReader.kt
│   │   ├── ChooserResultReceiver.kt
│   │   ├── FileTypeAdapter.kt
│   │   ├── FolderWidgetProvider.kt
│   │   ├── InspectBottomSheet.kt
│   │   ├── IntentRouter.kt
│   │   ├── LauncherActivity.kt
│   │   ├── LauncherScreen.kt
│   │   ├── MainActivity.kt
│   │   ├── MimeDetector.kt
│   │   ├── OpenAsBottomSheet.kt
│   │   ├── OpenBridgeViewModel.kt
│   │   ├── PrefsManager.kt
│   │   ├── RecentFile.kt
│   │   ├── RecentFileStore.kt
│   │   ├── SettingsActivity.kt
│   │   ├── SettingsScreen.kt
│   │   ├── Theme.kt
│   │   └── ThemePreference.kt
│   └── res/
│       ├── drawable/
│       ├── layout/
│       ├── values/
│       └── xml/
└── test/
    └── java/com/anyfile/x/
        └── MimeDetectorTest.kt
```

---

## ⚡ Core Source Files (`app/src/main/java/com/anyfile/x/`)

| File Name | Architectural Role | Key Features & Implementation |
| :--- | :--- | :--- |
| **[`MimeDetector.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/MimeDetector.kt)** | **Core Detection Engine** | Inspects magic bytes (using a 33KB buffer) to identify true MIME types. Supports deep-peek ZIP analysis (APKs, Office files, EPUBs) and script shebang/syntax checks. |
| **[`IntentRouter.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/IntentRouter.kt)** | **Intent Forwarder & Router** | Fires standard view intents with the resolved MIME type. Solves Android permission issues by pre-emptively granting read URI permissions to target handlers. |
| **[`MainActivity.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/MainActivity.kt)** | **Transparent Entry-Point** | Dedicated gatekeeper activity that intercepts inbound file intents (`ACTION_VIEW`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE`) and forwards them to the UI without holding state. |
| **[`LauncherActivity.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/LauncherActivity.kt)** | **Dashboard Host Activity** | Entry point of the interactive UI. Manages life cycle events, configures settings, and launches system file dialog overrides. |
| **[`LauncherScreen.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/LauncherScreen.kt)** | **Main Dashboard Composable** | Rich Jetpack Compose UI containing the file picker, detected MIME results panel, action buttons (Open, Inspect, Open Folder), custom overrides, and Recent Files. |
| **[`OpenBridgeViewModel.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/OpenBridgeViewModel.kt)** | **UI State Controller** | Connects UI interactions with the detection logic. Scans files asynchronously on `Dispatchers.IO` using the safe, application-level `ContentResolver`. |
| **[`InspectBottomSheet.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/InspectBottomSheet.kt)** | **Hex/ASCII Binary Viewer** | An inspection sheet that reads blocks of bytes incrementally (256/512 byte chunks) to display formatted Hex Dumps and printable ASCII previews side-by-side. |
| **[`OpenAsBottomSheet.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/OpenAsBottomSheet.kt)** | **MIME Override Chooser** | Displays detected file properties and provides a grid of standard types (Video, Audio, Image, text, etc.) to force-open the file using a specific category. |
| **[`ChooserResultReceiver.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/ChooserResultReceiver.kt)** | **Broadcast Receiver** | Listens to system chooser actions to record which application package was picked by the user for a specific MIME type. |
| **[`FolderWidgetProvider.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/FolderWidgetProvider.kt)** | **App Widget Provider** | Powers the home screen shortcut widget to instantly trigger folder navigation to default storage directories. |
| **[`PrefsManager.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/PrefsManager.kt)** | **Preference Layer** | Handles simple flags in `SharedPreferences` and modern flow-based UI settings (like selected theme mode) via Jetpack `DataStore`. |
| **[`RecentFile.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/RecentFile.kt)** | **Data Model** | Serializable data class representing a recently opened file (URI, filename, custom MIME, and epoch timestamp). |
| **[`RecentFileStore.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/RecentFileStore.kt)** | **Recents Database** | A Jetpack DataStore wrapper that persists a serialized JSON list containing the 10 most recently accessed files, maintaining sorting order. |
| **[`SettingsActivity.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/SettingsActivity.kt)** | **Settings Activity Host** | A simple Composable wrapper that opens the settings UI matching the current user-defined theme. |
| **[`SettingsScreen.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/SettingsScreen.kt)** | **Settings Dashboard UI** | Composable page containing settings options (theme switching dialog, help guides, application details, and donation links). |
| **[`FileTypeAdapter.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/FileTypeAdapter.kt)** | **RecyclerView Adapter** | Binds file type options to their grid items in the traditional bottom sheet chooser layout. |
| **[`Theme.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/Theme.kt)** | **Style customizer** | Handles dynamic material color schemes (Light, Dark, AMOLED Pure Black, and Android 12+ Material You dynamic color adaptation). |
| **[`ThemePreference.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/ThemePreference.kt)** | **Theme Enum** | Enumeration defining style options: `SYSTEM_DEFAULT`, `AMOLED`, and `MATERIAL_YOU`. |
| **[`utils/ByteReader.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/java/com/anyfile/x/utils/ByteReader.kt)** | **Safe Byte Reader** | Helper that performs safe low-level stream offset skipping and reads from Uri objects. Safely catches errors and returns empty arrays. |

---

## 🎨 Layout & Resource Files (`app/src/main/res/`)

AnyFile X leverages a hybrid configuration: XML layouts for complex bottom sheet designs and RecyclerView binds, and Jetpack Compose for the main screens.

### 📐 1. Layouts (`res/layout/`)
* **[`activity_settings.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/layout/activity_settings.xml)**: Legacy shell for settings wrapper (though mostly composed).
* **[`bottom_sheet_open_as.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/layout/bottom_sheet_open_as.xml)**: Renders the Open As selection grid, display name headers, and the Quick Open chip.
* **[`bottom_sheet_inspect.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/layout/bottom_sheet_inspect.xml)**: Hosts the binary hex viewer scrolls, headers, and "Load More" controls.
* **[`item_file_type.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/layout/item_file_type.xml)**: Represents individual cells in the FileType chooser (emoji text + labels).
* **[`widget_folder_shortcut.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/layout/widget_folder_shortcut.xml)**: Layout for the home screen system folder widget.

### 🧭 2. App XML metadata (`res/xml/`)
* **[`folder_widget_info.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/xml/folder_widget_info.xml)**: Declares Android widget parameters (dimensions, initial layout, update frequencies).
* **[`paths.xml`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/res/xml/paths.xml)**: Declares directory rules for the internal `FileProvider` so other apps can read exported files.

### 🎨 3. Design Assets & Configs (`res/values/` & `res/drawable/`)
* **`values/colors.xml` & `values/styles.xml`**: Defines XML application themes (including `Theme.OpenBridge.Transparent` for backgroundless main activity redirects).
* **`drawable/` styles**: Configures clean, modern gradients (`bg_primary_gradient.xml`), card styles (`bg_card_sleek.xml`), and bottom sheet shapes.

---

## 🛡️ Manifest Configuration (`app/src/main/AndroidManifest.xml`)

The [**`AndroidManifest.xml`**](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/main/AndroidManifest.xml) acts as the routing blueprint:
- **Scoped Storage & Permissions**: Requests `READ_EXTERNAL_STORAGE` and `MANAGE_EXTERNAL_STORAGE` to parse internal file data.
- **Intent Gateway (`MainActivity`)**: Intercepts generic file intents by binding to `ACTION_VIEW`, `ACTION_SEND`, and `ACTION_SEND_MULTIPLE` for `*/*` MIME types, allowing AnyFile X to act as a system-wide "Open With..." option.
- **Package Visibility Queries**: Specifically queries package visibility for `com.android.documentsui` to enable direct folder jumping.

---

## 🧪 Test Suite (`app/src/test/java/com/anyfile/x/`)

* **[`MimeDetectorTest.kt`](file:///c:/Users/tapman/Desktop/open%20with%20apk/app/src/test/java/com/anyfile/x/MimeDetectorTest.kt)**:
  - **Unit Test Coverage**: Targets the `MimeDetector` logic.
  - **Tested Capabilities**: Validates standard signatures for Video, Audio, Images, Documents, Archives, and complex Zip-based formats (APKs, EPUB, DOCX offsets) using custom-built mock headers.

---

## 🔒 Architectural Stability Anchors

AnyFile X features strict safeguarding to prevent common Android lifecycle/permission bugs:

1. **Decoupled Lifecycle I/O** (Implemented in `OpenBridgeViewModel.kt` & `InspectBottomSheet.kt`):
   All resource reading operations are bound to the `viewModelScope` or `lifecycleScope` executing on `Dispatchers.IO`, utilizing `applicationContext` to prevent memory leaks and `IllegalStateException`s if the UI sheet is dismissed mid-read.
2. **Intent Integrity** (Implemented in `MainActivity.kt`):
   The gatekeeper `MainActivity` holds no Compose or UI state. It handles the incoming intent immediately, showing the bottom sheet dialog on top of a transparent window, avoiding crashes or intent data drops during configuration changes.
3. **Permission Bridging** (Implemented in `IntentRouter.kt`):
   Pre-emptively queries matching packages and invokes `grantUriPermission` on target packages prior to starting the chooser, eliminating permission errors when third-party apps read shared files.
4. **Signature Locking** (Implemented in `MimeDetector.kt`):
   Performs deep recursive inspection of ZIP headers up to 30 files, specifically verifying manifests (`AndroidManifest.xml` for APK, etc.) to guarantee that generic ZIP archives are never misidentified.
