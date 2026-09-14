# Vian Launcher (Fossify Home) Blueprint

## 1. Architecture & App Structure
- **Application Type**: Android Launcher / Home Screen (`org.fossify.home` / Vian Launcher)
- **Upstream / Base**: Fossify Home
- **Tech Stack**: Kotlin, AndroidX, Jetpack Lifecycle, ViewBinding, Room (persistence), Coroutines, Detekt, Gradle Kotlin DSL
- **Platform**: Modern Android SDK 36, Java 17/21 target, AGP 9.1.1 / 9.3.0
- **Package Structure**:
  - `org.fossify.home`: Core Application, launcher activity, adapters, databases, receivers, dialogs, helpers, settings
  - Local database: Room DB for hidden apps, custom order, favorites, app drawer shortcuts

## 2. Planned Migration & Development Phases
- **Phase 1: Security Sanitization & Audit Compliance**
  - Remove exposed base64 keystore artifacts (`debug.keystore.base64`) from the workspace
  - Remove hardcoded signing credentials from build scripts, switching to environment variable injection (`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`, or system fallbacks)
- **Phase 2: Workspace Restructuring & Subfolder Promotion**
  - Promote nested `Vian-launcher-b99ee3196d6802f6f4e5703ede6b0611c4b9aa5f` assets and structure to the project root
  - Replace default template placeholder module with the full launcher app module
  - Synchronize resources, Fastlane bundles, detekt rules, and licensing
- **Phase 3: Build & Platform Alignment**
  - Reconcile `settings.gradle.kts` and root `build.gradle.kts`
  - Reconcile `app/build.gradle.kts` to remove flavor dimensions (enabling standard single APK generation at `app/build/outputs/apk/debug/app-debug.apk`)
  - Migrate `foss` flavor boolean resources (`bools.xml`) into `main/res/values/bools.xml`
  - Update `metadata.json` and strings (`app_launcher_name` / `app_name`) for consistent platform identity
- **Phase 4: Compilation Verification & Stability Check**
  - Execute `compile_applet` to verify zero compile or packaging regressions
  - Validate Room KSP code generation and resource resolution
- **Phase 5: Post-Phase QA & Verification Delivery**
  - Provide on-device verification suite

## 3. Running Change Ledger
- [Init] Created `BLUEPRINT.md` and initial audit ledger
- [Security] Removed `debug.keystore.base64`, updated `.gitignore` with keystore and credentials exclusions, removed hardcoded passwords from `signingConfigs`.
- [Migration] Promoted `Vian-launcher-...` to project root, replacing placeholder template app.
- [Build Alignment] Unified build configuration, added JitPack repository for `fossify.commons`, resolved duplicate `app_name` string resource, resolved duplicate legacy `com.android.support` classes via configuration exclusion, and synchronized platform `metadata.json` with `strings.xml`.
- [Verification] Verified end-to-end compilation with `compile_applet` (Build succeeded). Post-build keystore cleanup executed.
- [Feature: Native Clock Widget & Grid 5x9]
  - Configured default home screen grid to 5 columns by 9 rows beside dock (10 rows total, dock at bottom row).
  - Implemented `BuiltInClockWidgetView` (5x2 native header widget) featuring high-contrast white text with black outline (stroked font) for time and date.
  - Implemented single-tap launch targeting `com.android.deskclock.go` (Version 1.2.8go) with resilient system clock fallbacks.
  - Wired into `HomeScreenGrid` drag-and-drop lifecycle: fully movable, removable via context menu (with resize suppressed), and automatically placed on default page 0.
  - Integrated into `WidgetsFragment` under Clock category with vector preview (`ic_clock_widget_preview.xml`), allowing users to re-add the clock widget anytime.
  - Verified local build and unit test compilation cleanly. Cleared temporary build artifacts.
