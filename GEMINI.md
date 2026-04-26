# AnyFile Opener (OpenBridge) - Context & Instructions

AnyFile Opener (internally named **OpenBridge**) is a specialized Android utility designed to act as a universal `ACTION_VIEW` handler. Its primary purpose is to allow users to open files with forced or accurately detected MIME types, specifically solving URI permission issues that occur when forwarding files from restricted storage (like Telegram's cache or `Android/data`) to third-party apps.

## Project Overview

- **Core Function:** Intercepts file-opening intents, identifies the file type via magic bytes, and re-dispatches the file to a target app with robust URI permission grants.
- **Key Technologies:** Kotlin, Android SDK (API 26-34), ViewBinding, Gradle.
- **Package Name:** `com.openbridge` (Note: `com.anyfileopener` is legacy/deprecated).

## Architecture & Key Files

### Core Logic
- **Entry Point:** `app/src/main/java/com/openbridge/MainActivity.kt` - Transparent activity that handles incoming `ACTION_VIEW` intents and launches the "Open As" bottom sheet.
- **MIME Detection:** `app/src/main/java/com/openbridge/MimeDetector.kt` - Logic for magic-byte signature matching and extension fallback.
- **Intent Routing:** `app/src/main/java/com/openbridge/IntentRouter.kt` - Critical component that uses `ClipData` and explicit `grantUriPermission` calls to ensure target apps can read the shared URI.
- **Launcher:** `app/src/main/java/com/openbridge/LauncherActivity.kt` - Main UI for file picking, manual MIME overrides, and binary inspection.

### UI & Resources
- **Selection UI:** `app/src/main/java/com/openbridge/OpenAsBottomSheet.kt` - Grid of file type options shown to the user.
- **Binary Inspector:** `app/src/main/java/com/openbridge/InspectBottomSheet.kt` - Hex/ASCII preview of file headers.
- **Layouts:** Found in `app/src/main/res/layout/`, notably `bottom_sheet_open_as.xml`.

## Building and Running

### Prerequisites
- JDK 11 or higher.
- Android SDK (API 34).

### Key Commands
- **Assemble Debug APK:**
  ```powershell
  ./gradlew assembleDebug
  ```
- **Install to Device:**
  ```powershell
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- **Clean Project:**
  ```powershell
  ./gradlew clean
  ```

## Development Conventions

### 1. Kotlin Lexer Workaround
**CRITICAL:** Avoid using the literal string `*/*` (slash-star) directly in Kotlin source code. Some environments/compilers (Kotlin 1.9.22) may misinterpret this token as a comment start/end, breaking the build.
- **Standard Practice:** Use runtime string construction:
  ```kotlin
  val slash = "/"
  val wildcardMime = "video${slash}*"
  ```

### 2. URI Permission Forwarding
Always use `IntentRouter.open()` when launching files. This ensures:
- `Intent.FLAG_GRANT_READ_URI_PERMISSION` is set.
- `ClipData` is populated (required for Android 10+).
- Explicit permissions are granted to candidate packages to prevent `SecurityException`.

### 3. Extending File Detection
To add support for a new file type:
1. Update `MimeDetector.FileType` enum if a new category is needed.
2. Add a signature match in `MimeDetector.detectFromHeader()`.
3. Update `MimeDetector.mimeOf()` to map the category to a MIME string.

### 4. Code Style
- Use **ViewBinding** for all UI interactions.
- Prefer `object` for stateless utility engines (like `MimeDetector`).
- Ensure all new Activities are declared in `AndroidManifest.xml` with appropriate themes.

## Status & Troubleshooting
- **Build Success:** The project is currently in a buildable and functional state.
- **Permission Denied:** If a target app cannot open a file, verify that `IntentRouter` is successfully iterating through and granting permissions to the recipient package.
- **Package Conflicts:** Ensure you are working within the `com.openbridge` package; ignore or remove any leftovers in `com.anyfileopener`.
