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
- [Feature: Home Screen Long-Press "Add" Menu (App, Widget, Shortcut, Folder, Page)]
  - Updated home screen long-press context menu (`menu_home_screen.xml`) to include `Add` (`@string/add`) at the top while retaining existing options (`Widgets`, `Wallpapers`, `Launcher settings`, `Set as default`).
  - Implemented cascading `Add` popup menu (`menu_add_to_home.xml`) with five options:
    - **App**: Opens `AddAppDialog` with real-time title search and application icon list; placing an app pins it to the long-pressed cell (or first vacant grid cell on the current page).
    - **Widget**: Directly expands the widgets drawer (`WidgetsFragment`) for widget selection and placement.
    - **Shortcut**: Opens `AddShortcutDialog` listing all installed apps advertising `ACTION_CREATE_SHORTCUT`; supports both modern `LauncherApps.PinItemRequest` and legacy `ACTION_CREATE_SHORTCUT` result intents with fallback icons.
    - **Folder**: Opens `CreateFolderDialog` with customizable name (defaults to "Folder"); creates an empty folder and handles empty folder rendering (`generateDrawable()` fallback and scalable open folder view).
    - **Page**: Invokes `HomeScreenGrid.addNewPage()`, increments max page tracking, updates page indicator dots, and navigates immediately to the new home page.
  - Added smart cell targeting (`HomeScreenGrid.getTargetCell()` and `findFirstEmptyCellOnCurrentPage()`) prioritizing the exact long-pressed vacant cell.
  - Verified clean compilation via `compile_applet` and Gradle test task; purged ephemeral artifacts in compliance with Security Scan Protocol.
- [Feature & Bugfix: Clock Widget Tap/Long-Press/Drag, System-Wide Widget Moving, Drawer Fast Icon Loading, and Proportional Icon Sizing]
  - **Clock Widget Tap vs. Menu Fix**: Resolved bug where tapping the clock widget opened the long-press menu upon returning from the clock app. Tapping cancels any pending long-press handlers immediately in `BuiltInClockWidgetView` and `MyAppWidgetHostView`, so tap solely launches the clock app.
  - **Widget Long-Press & Move**: Unified touch dispatching in `MyAppWidgetHostView` and `BuiltInClockWidgetView` to provide tactile long-press menus and drag-and-drop repositioning across pages and grid cells, matching modern launcher behavior. Replaced deprecated `drawingCache` with modern `View.draw(Canvas)` for widget drag previews.
  - **Drawer Fast Icon Loading & Async Cache**: Added in-memory `LruCache` to `IconCache` to eliminate duplicate icon loading. Bypassed redundant Glide pipeline for in-memory drawables; loads cached drawables instantly on main thread and fetches uncached package icons asynchronously. Optimized `getAllAppLaunchers` to reuse existing database thumbnail colors without full bitmap pixel scanning.
  - **Proportional Icon Sizing**: Updated drawer icon layout dimensions and minimized padding in `LaunchersAdapter` so drawer icons render at standard ~60dp launcher size. Tuned home screen grid cell `iconMargin` to 4dp-6dp to ensure icons are balanced and prominent on high-density grids.
- [Feature: Dock Folders Support]
  - Enabled dragging existing folders into the dock row (`yIndex == rowCount - 1`) with dock column boundary safety (`xIndex < dockColumnCount`).
  - Enabled creating new folders directly in the dock by dropping app icons onto docked apps (`potentialParent` logic in `HomeScreenGrid.kt`).
  - Enabled dropping items into existing docked folders and auto-opening docked folders upon drag hover.
  - Ensured items inside folders maintain `docked = false` and properly belong to their parent container.
- [Feature: Custom App & Folder Icon Customization (Option B + Style 1)]
  - Implemented safe bitmap downsampling in `CustomIconManager` (capping dimensions to 192x192) to strictly protect against SQLite `CursorWindow` 2MB OOM crashes.
  - Added global package-level custom icon persistence via internal storage (`filesDir/custom_icons/<pkg>.png`) and in-memory `IconCache` updates, reflecting custom icons across both the app drawer and home screen.
  - Added folder custom icon persistence directly in `HomeScreenGridItem.icon` column.
  - Upgraded rename dialog to `EditItemDialog` (Style 1), featuring instant icon preview, title editor, and a modal action sheet offering Gallery/Photo Picker (zero-permission), installed app icon selection, and reset to default.
- [Feature: Decoupled Log Catcher & Log Keeper UI]
  - **Early Process Startup Hook**: Introduce `VianApp : FossifyApp()` in `AndroidManifest.xml` to initialize `LogCatcher` at the earliest point in the application lifecycle (`Application.onCreate()`). Captures unhandled exceptions, runtime errors, and early startup telemetry before any Activity starts.
  - **Lightweight LogCatcher Core**: Headless singleton that records sanitized, structured log entries (timestamp, component tag, message, sanitized stack trace) without UI overhead. Adheres to Mandate 17 (Log Keeper Standard: no PII, no user content, no credentials).
  - **2MB Auto-Dump & Scoped Storage**: Maintains a rolling buffer up to 2MB in app-private storage. When 2MB is reached or on application crash, automatically flushes logs directly to the public device `Download/` folder as `vian logs.txt` via `MediaStore.Downloads` (zero-permission on Android 10+; fallback on Android 8/9).
  - **Crash Handler Integration**: Installs `Thread.setDefaultUncaughtExceptionHandler` during `VianApp` init, writes crash dump with system context immediately to `Download/vian logs.txt` via synchronous I/O, and passes to the system default handler for clean termination.
  - **Dedicated Log Keeper UI (Screenshot Match)**: Overhaul `LogViewerActivity` into a dedicated on-demand page:
    - Top bar: Back arrow, "Log Keeper" title, Master On/Off Switch (`config.logKeeperEnabled`), Copy icon (copies active logs to clipboard), Download icon (manual dump to `Download/vian logs.txt`).
    - Time filter pills: `6h`, `12h`, `24h`, and `All` tabs with dynamic filtering.
    - Log cards: Rounded card layout displaying monospace timestamp (`HH:mm:ss.SSS`), component/tag, bold log message, and expandable stack trace.
  - **Backward-Compatible Migration**: Integrate with existing `settingsViewAppLogsHolder` in `SettingsActivity.kt` and preserve existing `LogKeeperHelper` / `logKeeper.log` call sites. Migrate legacy logs from `vian_app_log.txt`.
- [Feature: 5 Item Gestures & Curated System Actions (Phase 1)]
  - **Zero-Latency Touch Discrimination**: Preserves instant 0ms tap launch on `ACTION_UP` and long-press context menu/drag. Intercepts `MotionEvent.ACTION_DOWN` on `HomeScreenGrid` items to route item-specific gestures.
  - **5 Item Gestures**: Supports `Double Tap`, `Swipe Up`, `Swipe Down`, `Swipe Left`, and `Swipe Right` on home screen items.
  - **Horizontal Pass-Through**: Gated horizontal swipe detection — if an item does not have an explicit `Swipe Left` or `Swipe Right` action assigned, horizontal flings pass seamlessly through to `HomeScreenGridPager` for smooth home screen page sliding.
  - **Curated System & Launcher Actions (`LauncherActionHandler`)**:
    - `ACTION_LOCK_SCREEN`: Locks device via `DevicePolicyManager.lockNow()`.
    - `ACTION_MUTE_VOLUME`: Toggles media stream mute via `AudioManager.adjustStreamVolume()`.
    - `ACTION_MUTE_RINGTONE`: Toggles ringer mode between Normal and Vibrate (or Silent with safe DND access check).
    - `ACTION_NOTIFICATIONS`: Expands notification shade via `StatusBarManager` reflection.
    - `ACTION_QUICK_SETTINGS`: Expands quick settings panel via `StatusBarManager` reflection.
    - `ACTION_APP_DRAWER`: Opens all apps drawer fragment.
    - `ACTION_FOLDER_POPUP`: Opens animated floating folder popup overlay (`HomeScreenGrid.openFolder()`).
    - `ACTION_APP_SHORTCUTS`: Opens dynamic app shortcuts popup.
    - `ACTION_LAUNCH_APP`: Launches assigned target application.
  - **Isolated Memory-Cached Persistence (`ItemGestureManager`)**: Thread-safe in-memory cache backed by private JSON storage (`item_gestures.json`), eliminating Room schema migration risks while providing instant 0ms lookup.
  - **Folder Double-Tap Default**: Automatically maps folder double-tap to `ACTION_FOLDER_POPUP`.
  - **Universal LogCatcher Instrumentation**: Logs action executions and failures directly to `LogCatcher` without PII.
- [Feature: On-Demand Popup Widgets (Phase 2 - Option C: Dual-Mode)]
  - **Floating Modal Host (`PopupWidgetDialog`)**: Implemented floating Material 3 modal card container (`dialog_popup_widget.xml`) hosting `MyAppWidgetHostView` with clean backdrop, rounded corners (24dp), elevation, header bar with app/widget icon, title, configure action button, unlink/change action button, and close (`✕`) button.
  - **Shared Host & Lifecycle Safeguards**: Reuses master `MyAppWidgetHost` (`WIDGET_HOST_ID = 100`) via `baseContext` to prevent RemoteViews theming collisions. Detaches view hierarchy immediately upon dismissal (`dialog.setOnDismissListener` and `onDetachedFromWindow`), ensuring 0MB residual View overhead while preserving the allocated `appWidgetId` in `ItemGestureManager` so users never face repetitive configuration prompts.
  - **Widget Selector Dialog (`SelectPopupWidgetDialog`)**: Modal picker (`dialog_select_popup_widget.xml`, `item_select_popup_widget.xml`) featuring search filtering, app-specific widget grouping, cell dimension indicators (`4 × 2`), and preview rendering with asynchronous background loading.
  - **Binding & Configuration Pipeline**: Integrated full Android `AppWidgetManager.bindAppWidgetIdIfAllowed` and `startAppWidgetConfigureActivityForResult` with fallback allocation cancellation.
  - **Context Menu Integration**: Added "Popup Widget" button (`ic_widget_vector`) to both the quick actions bar and fallback actions menu in `AppShortcutsPopupWindow`.
  - **Item Removal Cleanup**: Wired `HomeScreenGrid.removeItemFromHomeScreen` to automatically release allocated widget IDs from `appWidgetHost` and clear associated gesture configurations when home screen items are deleted.
- [Feature: Add to Home M3 Bottom Sheet & Element Bridge (Phase 3)]
  - **Sidebar App Coordinator Bridge (`AddElementBridge`)**: Decoupled the launcher home screen placement engine from the UI presentation layer via a clean `Delegate` interface. Allows seamless future delegation to the target Sidebar App's external multi-page Add Element Activity with a single delegate line, while providing a first-class native standalone implementation today.
  - **M3 Add to Home Bottom Sheet (`AddToHomeBottomSheet`)**: Replaced legacy nested `PopupMenu` with a Material 3 rounded bottom sheet (`dialog_add_to_home_bottom_sheet.xml`) featuring drag handle, title, subtitle, and 5 interactive category cards with ripple indications:
    1. 📱 **Applications**: Directly triggers `AddAppDialog`, placing the chosen app into the target cell `(x, y)` without manual dragging.
    2. 🧩 **Desktop Widgets**: Directly slides up the native `WidgetsFragment` drawer for multi-cell desktop widget placement.
    3. 🪟 **Popup Widgets**: Launches `SelectPopupWidgetDialog` and configures a standalone 1-slot popup widget launcher icon on the target grid cell.
    4. ⚡ **Shortcuts**: Directly opens `AddShortcutDialog` for pinning deep-link actions.
    5. 📁 **Folders**: Prompts `CreateFolderDialog` to place a newly named folder ready to receive apps.
    *(Explicitly removed legacy "Add Page" menu item)*.
  - **Home Screen & Gesture Action Routing**: Added `ACTION_ADD_TO_HOME`, `ACTION_ADD_APP`, and `ACTION_ADD_WIDGET` to `LauncherActionHandler` for home gesture assignment. Main long-press menu routes empty space directly to `AddElementBridge.open(x, y)` while preserving direct top-level access to Desktop Widgets.
  - **Standalone Popup Widget Click Support**: Enhanced `MainActivity.performItemClick` to automatically summon `openPopupWidget` when tapping a standalone popup widget icon on the home screen grid.
- [Feature: Full Gestures & Actions Settings Page & Per-Item UI (Phase 4)]
  - **Full Gestures Settings Screen (`GesturesActivity` & `activity_gestures.xml`)**:
    - Created dedicated "Gestures & Actions" settings page accessible from Settings (`SettingsActivity`).
    - Configured global empty home screen gesture handlers: Double Tap, Swipe Down, Swipe Up, and Pinch In (via `ScaleGestureDetector`).
    - Configured Folder Behavior section featuring Folder Cover Mode toggle (`folderCoverMode`) with real-time preference persistence.
    - Configured Feedback & Permissions section featuring Haptic Feedback toggle (`gestureHaptics`) and Device Admin permission status badge with direct shortcut to grant Device Admin for instant screen lock.
    - Standardized Material 3 typography, dynamic primary color styling via `getProperPrimaryColor()`, and `MyAppBarLayout` integration.
  - **Per-Item Gestures & Widget Customization (`EditItemDialog` & `dialog_edit_item.xml`)**:
    - Integrated "Linked Popup Widget" card displaying linked widget status, title, link widget picker, and unlink action.
    - Integrated "Item Gestures" card for granular per-item overrides across all 5 gestures: Double Tap, Swipe Up, Swipe Down, Swipe Left, and Swipe Right.
    - Upgraded `ItemGestureConfig` to support per-gesture target package assignments (`getTargetPackage`, `setTargetPackage`), allowing multiple launch app gestures per item.
    - Real-time badge display showing action name and target app title (`LauncherActionHandler.getActionLabel`).
  - **Folder Cover Mode & Touch Interaction Polish**:
    - When Folder Cover Mode is active: single tap launches first child application; swipe up or double tap opens folder popup overlay.
    - Universal haptic feedback feedback triggers on gesture activations when enabled.
  - **Security Sanitization**:
    - Purged ephemeral keystores and APK binaries post-compilation in accordance with Security Scan Protocol.





