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
- [Feature: Modern App Shortcuts & Floating Card Context Menu]
  - Integrated Android native `LauncherApps` shortcuts query in `AppShortcutsPopupWindow.kt` for dynamic and pinned shortcuts with application icon badges.
  - Implemented direct "Pin to home" action to place shortcuts on the home screen grid.
  - Modernized long-press context menu into a rounded floating card UI with quick Lock/Unlock actions.
- [Feature: Biometric App Lock & Protected Hidden Apps]
  - Upgraded Room Database to v6 with `locked_apps` table and `LockedAppsDao`.
  - Created `AppLockManager` with thread-safe in-memory package cache and session timeout policies (`Immediately`, `1 min`, `5 min`, `Screen off`).
  - Implemented launch interception across home screen grid, dock, app drawer, and shortcut popup window using AndroidX `BiometricPrompt` and Fossify `SecurityDialog`.
  - Gated `HiddenIconsActivity` behind biometric authentication when protected hidden apps setting is active.
  - Added dedicated App Lock settings section in `SettingsActivity`.
- [Security Sanitization & Audit Compliance]
  - Purged `/debug.keystore.base64` and `/.build-outputs/` binary artifacts from repo.
  - Strengthened `/.gitignore` with rules for `debug.keystore.base64`, `*.base64`, `.build-outputs/`, `*.apk`, and `*.aab`.
  - Verified clean build via `compile_applet` and local unit test suite.
- [CI Pipeline Standalone APK Builder Implementation]
  - Removed upstream FossifyOrg-dependent workflow files (`no-response.yml`, `prepare-release-pr.yml`, `pr-labeler.yml`, `update-commons.yml`, `release.yml`, `testing-build.yml`, `validate-fastlane-metadata.yml`, `image-minimizer.yml`, `update-lint-baselines.yml`, `pr.yml`) that crashed on missing GitHub App / Org credentials.
  - Implemented standalone GitHub Actions workflow `.github/workflows/build-debug-apk.yml` configured with JDK 17, `temurin` distribution, Gradle caching, unit testing (`testDebugUnitTest`), debug packaging (`assembleDebug`), and artifact upload (`actions/upload-artifact@v4`).
  - Verified local Gradle execution (`testDebugUnitTest`, `assembleDebug`, and `assembleRelease`) all succeed cleanly.
  - Enforced security sanitization: deleted ephemeral keystore files and build output APKs; confirmed zero credentials committed.
- [Bugfix: AppShortcutsPopupWindow Empty Range Coerce Crash]
  - Resolved `IllegalArgumentException: Cannot coerce value to an empty range: maximum -24 is less than minimum 24` in `AppShortcutsPopupWindow.kt`.
  - Enforced capped measure pass width (`targetMeasureWidth = minOf(maxAllowedWidth, (280 * density).toInt())`), preventing match_parent children from expanding the card to full screen width.
  - Implemented defensive boundary clamping on `popupX`, `popupY`, and `arrowLeft` (`maxX.coerceAtLeast(minX)`, `maxArrow.coerceAtLeast(minArrow)`), mathematically eliminating empty-range exceptions.
  - Verified stability on widget placement/long-press and app drawer launcher long-press.
- [Feature: Drawer Top Bar Search Button & 3-Dots Sort Toggle]
  - Configured top bar action button ordering: Search button positioned directly before Google Play Store button and 3-dots dropdown menu.
  - Implemented search unfolding into full search bar on button tap, and graceful return to button on search exit or back navigation.
  - Added lightweight "Sort by name" and "Sort by time" directly into 3-dots popup menu (`menu_drawer.xml`).
  - Implemented "once for up and switch down" bidirectional toggles:
    - Sort by name: A-Z (up) $\leftrightarrow$ Z-A (down) with dynamic title indicators.
    - Sort by time: Newest (down) $\leftrightarrow$ Oldest (up) with dynamic title indicators.
  - Added in-memory `@Ignore var installTime` on `AppLauncher` with lazy `PackageInfo` caching — 0 Room migrations and zero DB schema risks.
  - Persisted user sort preferences in `Config` (SharedPreferences).
  - Executed compilation and unit test verification; confirmed clean security scan.

