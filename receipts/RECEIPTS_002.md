# Receipts Log - Part 2

Permanent audit trail for Vian Launcher.
Continues from `RECEIPTS_001.md` after reaching the 500-line cap.

---

### Entry 014
- **Timestamp**: 2026-10-09T09:43:00-07:00
- **One-line summary of what was requested**: Fix Room main-thread crash on adding apps, fix SecurityException on unexported shortcuts, consolidate homescreen long-press menu, and fix LogKeeper status bar overlap.
- **Exact files touched**:
  - `/BLUEPRINT.md`
  - `/app/src/main/res/values/strings.xml`
  - `/app/src/main/res/menu/menu_home_screen.xml`
  - `/app/src/main/kotlin/org/fossify/home/activities/MainActivity.kt`
  - `/app/src/main/kotlin/org/fossify/home/dialogs/SelectActionDialog.kt`
  - `/app/src/main/kotlin/org/fossify/home/activities/LogViewerActivity.kt`
  - `/receipts/RECEIPTS_002.md`
- **What was actually done**:
  - Solved `IllegalStateException: Cannot access database on the main thread`: Wrapped `getAllAppLaunchers()` in `ensureBackgroundThread` in both `MainActivity.showAddAppDialog` and `SelectActionDialog.pickAppToLaunch`, posting `AddAppDialog` display back to `runOnUiThread` with activity lifecycle guards.
  - Solved `SecurityException: Permission Denial` on shortcut creation: Added filter `it.activityInfo != null && it.activityInfo.exported` to `MainActivity.showAddShortcutDialog` query results; wrapped `startActivityForResult(REQUEST_CREATE_SHORTCUT)` in defensive `try-catch` inside `handleShorcutCreation` with user toast (`cannot_create_shortcut`) and `LogCatcher` error logging.
  - Consolidated home screen long-press popup menu: Removed redundant `R.id.widgets` entry from `menu_home_screen.xml` and handler in `MainActivity.kt`, unifying element creation under `R.id.add_to_home` (`AddToHomeBottomSheet`).
  - Solved status bar overlap in LogKeeper UI: Added `padTopSystem = listOf(binding.logViewerTopBar)` to `LogViewerActivity.setupEdgeToEdge`, properly applying window top insets beneath camera cutouts and system status bar.
- **How it was verified**: Local build only (`compile_applet` passed, `gradle :app:testDebugUnitTest` passed).
- **Any deviation from what was requested, and why**: None. Built exactly to the agreed architecture.
- **Any known issue or follow-up needed**: Ready for on-device manual QA.

---

### Entry 015
- **Timestamp**: 2026-10-10T01:10:00-07:00
- **One-line summary of what was requested**: Implement App Drawer UI overhaul, squarish bubble folders on home screen, label-free folder popups with top-bar (+) app picker, and unified widget long-press & drag-and-drop.
- **Exact files touched**:
  - `/app/src/main/kotlin/org/fossify/home/views/BuiltInClockWidgetView.kt`
  - `/app/src/main/kotlin/org/fossify/home/views/HomeScreenGrid.kt`
  - `/app/src/main/kotlin/org/fossify/home/activities/MainActivity.kt`
  - `/BLUEPRINT.md`
  - `/receipts/RECEIPTS_002.md`
- **What was actually done**:
  - App Drawer Overhaul: Verified top-bar layout in `all_apps_fragment.xml` with bold "All apps" title (22sp) and right-aligned search action, along with semi-transparent background scrim in `View.setupDrawerBackground()` (~82% alpha) ensuring wallpaper visibility beneath the drawer.
  - Squarish Bubble Folders: Configured squircle geometry (`cornerRadius = iconSize * 0.26f`) with translucent fill (`backgroundColor.adjustAlpha(0.60f)`) and subtle bubble stroke outline (`1.5dp`) in `HomeScreenFolder.generateDrawable()`.
  - Folder Popup Enhancements: Verified label omission when `item.parentId != null` inside folder popups, centering app icons vertically; wired top-bar `(+)` button (`isClickingFolderAddButton`) in `HomeScreenGrid` and `MainActivity.homeScreenClicked` to trigger `showAddAppToFolderDialog`, loading launchers off the main thread and inserting `HomeScreenGridItem` into Room DB on a worker thread. Also wired top-bar title tap to open `RenameItemDialog`.
  - Unified Widget Long-Press & Drag: Fixed touch interception in `HomeScreenGrid.widgetLongPressed` and `placeAppWidget` so that long-pressing a widget activates its resize frame while touch moves flow smoothly to `handleWidgetDrag` and `draggedItemMoved`; multi-cell dimensions and grid snapping are maintained, updating Room DB on drop and re-displaying the resize frame around the updated coordinates.
  - Built-in Clock Widget Reliability: Added `dispatchDraw` implementation alongside `onDraw`, bypassed framework `updateAppWidgetSize` dummy ID reset, and guaranteed dynamic widget view restoration in `HomeScreenGrid.onDraw` even when `isFirstDraw` is false.
  - Security Sanitization: Keystore scanning performed; ephemeral `./debug.keystore` generated during build was deleted immediately.
- **How it was verified**: Local build only (`compile_applet` passed; `gradle :app:testDebugUnitTest` passed).
- **Any deviation from what was requested, and why**: None. Built strictly to the finalized implementation plan.
- **Any known issue or follow-up needed**: Ready for on-device manual QA.

