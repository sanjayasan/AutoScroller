# Goal: Build "AutoScroller" Native Android App (Kotlin) with GitHub Actions CI

Create a complete, production-ready Android application in Kotlin using modern Jetpack libraries, an AccessibilityService for automated gestures, a floating window overlay service, and a complete GitHub Actions CI workflow that compiles and produces a downloadable APK artifact.

---

## 1. Project Specifications

* **Language**: Kotlin
* **Min SDK**: 26 (Android 8.0) | **Target SDK**: 34+
* **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with version catalog (`libs.versions.toml`)
* **Architecture**: Clean MVVM with foreground/background services

---

## 2. Core Architecture & Components

### A. Permissions & Fixed UI (`MainActivity`)
* **Required Permissions**:
  * `android.permission.SYSTEM_ALERT_WINDOW` (Overlay)
  * `android.permission.BIND_ACCESSIBILITY_SERVICE` (Accessibility gesture injection)
  * `android.permission.FOREGROUND_SERVICE`
  * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Battery whitelist)
* **UI Features**:
  1. Live permission indicator chips/cards (Green = Granted, Red = Missing).
  2. Direct navigation intents for each permission (e.g., `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`, `Settings.ACTION_ACCESSIBILITY_SETTINGS`, battery optimization dialog).
  3. "Launch Floating Controller" button (enabled only when required permissions are granted).
  4. "Exit App" button (`finishAffinity()`).

### B. Floating Window Service (`FloatingOverlayService`)
* Runs as a foreground service with a persistent notification.
* Mounts a custom overlay layout using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
* Draggable floating panel with two visual states:
  * **Expanded State**:
    * Countdown timer delay input (seconds/minutes before loop starts).
    * Active countdown display.
    * "Set Point A" and "Set Point B" triggers: Spawns two draggable visual marker pins on screen to capture `(x1, y1)` and `(x2, y2)` coordinates.
    * Loop count input (integer limit to prevent infinite loops).
    * Scroll duration & pause interval inputs.
    * "Start" button & "Stop / Abort" kill switch.
    * "Minimize" button.
    * "Close Floating UI" button.
  * **Minimized / Compact State**:
    * Clean pill HUD showing only the active countdown or current loop count (`Loop X / Total`), tap to re-expand.

### C. Gesture Execution Engine (`ScrollAccessibilityService`)
* Inherits from `AccessibilityService`.
* Exposes an interface/coroutine channel to receive gesture commands.
* Uses `dispatchGesture()` with a `Path` moving linearly from Point A to Point B over the configured swipe duration.
* Repeats sequentially across the designated loop count, respecting delay intervals, with cancellation checks on each iteration.

---

## 3. GitHub Actions CI & Repository Setup

1. Initialize a Git repository with an appropriate `.gitignore` for Android/Gradle.
2. Create `.github/workflows/build-apk.yml`:
   * Set up JDK 17.
   * Run Gradle wrapper validation.
   * Build the debug APK (`./gradlew assembleDebug`).
   * Upload the resulting `.apk` file using `actions/upload-artifact@v4` so it can be directly downloaded from the GitHub run summary.
3. Configure Git remotes and prepare instructions to push to GitHub.

---

## Deliverables Required
1. Complete source code files (Manifest, services, layout XMLs or Compose, helper classes, Gradle configuration).
2. Proper permission helper classes to verify overlay and accessibility status.
3. GitHub Actions workflow file.
4. Step-by-step CLI commands executed to verify the build runs cleanly (`./gradlew build` or `./gradlew assembleDebug`).

## Git & GitHub Automation Step
When the codebase and Gradle configurations are complete and verified:
1. Initialize git with `git init -b main`.
2. Configure a standard Android `.gitignore` (ignore build/, .gradle/, *.keystore, local.properties).
3. Commit all generated files with a clean commit message.
4. Using the GitHub CLI (`gh`), run:
   gh repo create AutoScroller --public --source=. --remote=origin --push
5. Verify the push was successful and print the URL to the GitHub Actions tab so I can watch the APK build.
