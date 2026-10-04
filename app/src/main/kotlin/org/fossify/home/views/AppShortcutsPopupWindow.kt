package org.fossify.home.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.realScreenSize
import org.fossify.commons.extensions.showErrorToast
import org.fossify.home.R
import org.fossify.home.activities.MainActivity
import org.fossify.home.databinding.ItemPopupShortcutBinding
import org.fossify.home.databinding.PopupAppShortcutsBinding
import org.fossify.home.extensions.canAppBeUninstalled
import org.fossify.home.helpers.AppLockManager
import org.fossify.home.helpers.BUILT_IN_CLOCK_CLASS_NAME
import org.fossify.home.helpers.ITEM_TYPE_FOLDER
import org.fossify.home.helpers.ITEM_TYPE_ICON
import org.fossify.home.helpers.ITEM_TYPE_SHORTCUT
import org.fossify.home.helpers.ITEM_TYPE_WIDGET
import org.fossify.home.interfaces.ItemMenuListener
import org.fossify.home.models.HomeScreenGridItem

@SuppressLint("WrongConstant")
class AppShortcutsPopupWindow(
    private val activity: Activity,
    private val gridItem: HomeScreenGridItem,
    private val isOnAllAppsFragment: Boolean,
    private val listener: ItemMenuListener,
    private val iconRect: Rect? = null,
) : PopupWindow(activity) {

    private val binding = PopupAppShortcutsBinding.inflate(LayoutInflater.from(activity))

    init {
        contentView = binding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        elevation = 16f
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupUI()
    }

    private fun setupUI() {
        // App / Item Title
        binding.popupAppTitle.text = gridItem.title.ifEmpty {
            if (gridItem.type == ITEM_TYPE_WIDGET) {
                activity.getString(R.string.clock)
            } else {
                activity.getString(R.string.app_launcher_name)
            }
        }

        // Header Gear icon (App Info)
        val canShowAppInfo = gridItem.type == ITEM_TYPE_ICON && gridItem.packageName.isNotEmpty()
        binding.popupAppInfoBtn.beVisibleIf(canShowAppInfo)
        if (canShowAppInfo) {
            binding.popupAppInfoBtn.setOnClickListener {
                listener.onAnyClick()
                listener.appInfo(gridItem)
                dismiss()
            }
        }

        // Check shortcuts availability via LauncherApps
        val launcherApps = activity.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val hasHostPermission = launcherApps?.hasShortcutHostPermission() == true

        val shortcuts: List<ShortcutInfo>? = if (hasHostPermission && gridItem.type == ITEM_TYPE_ICON && gridItem.packageName.isNotEmpty()) {
            try {
                val query = LauncherApps.ShortcutQuery().setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                ).setPackage(gridItem.packageName)
                launcherApps?.getShortcuts(query, Process.myUserHandle())
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val hasShortcuts = !shortcuts.isNullOrEmpty()

        if (hasShortcuts && launcherApps != null) {
            setupShortcutsList(shortcuts!!, launcherApps)
            setupQuickActionsBar()
            binding.popupShortcutsContainer.beVisible()
            binding.popupActionsBar.beVisible()
            binding.popupFallbackActions.beGone()
            binding.popupDefaultLauncherPrompt.beGone()
        } else {
            binding.popupShortcutsContainer.beGone()
            binding.popupActionsBar.beGone()
            binding.popupFallbackActions.beVisible()

            setupFallbackActions()

            // If launcher does not have host permission and this is an app icon, inform the user
            if (!hasHostPermission && gridItem.type == ITEM_TYPE_ICON) {
                binding.popupDefaultLauncherPrompt.beVisible()
                binding.popupBtnSetDefault.setOnClickListener {
                    dismiss()
                    launchSetAsDefault()
                }
            } else {
                binding.popupDefaultLauncherPrompt.beGone()
            }
        }

        setOnDismissListener {
            listener.onDismiss()
        }
    }

    private fun setupShortcutsList(shortcuts: List<ShortcutInfo>, launcherApps: LauncherApps) {
        binding.popupShortcutsContainer.removeAllViews()
        val inflater = LayoutInflater.from(activity)
        val densityDpi = activity.resources.displayMetrics.densityDpi

        // Display up to 5 shortcuts
        shortcuts.take(5).forEach { shortcutInfo ->
            val itemBinding = ItemPopupShortcutBinding.inflate(inflater, binding.popupShortcutsContainer, false)

            // Label (prefer shortLabel for clean appearance like in launcher popups)
            val label = shortcutInfo.shortLabel?.toString()?.takeIf { it.isNotBlank() }
                ?: shortcutInfo.longLabel?.toString()?.takeIf { it.isNotBlank() }
                ?: shortcutInfo.id
            itemBinding.shortcutItemLabel.text = label

            // Icon
            val iconDrawable = try {
                launcherApps.getShortcutIconDrawable(shortcutInfo, densityDpi)
            } catch (e: Exception) {
                null
            }

            if (iconDrawable != null) {
                itemBinding.shortcutItemIcon.setImageDrawable(iconDrawable)
                itemBinding.shortcutItemIcon.beVisible()
            } else {
                itemBinding.shortcutItemIcon.beGone()
            }

            // Click action to launch shortcut
            itemBinding.root.setOnClickListener {
                listener.onAnyClick()
                val pkgName = shortcutInfo.`package`
                val shortcutId = shortcutInfo.id
                val bounds = Rect()
                itemBinding.root.getGlobalVisibleRect(bounds)
                val userHandle = Process.myUserHandle()
                dismiss()

                if (activity is FragmentActivity && !AppLockManager.canLaunchWithoutAuth(activity, pkgName)) {
                    AppLockManager.authenticateAndLaunch(activity, pkgName, label) {
                        try {
                            launcherApps.startShortcut(pkgName, shortcutId, bounds, null, userHandle)
                        } catch (e: Exception) {
                            activity.showErrorToast(e)
                        }
                    }
                } else {
                    try {
                        launcherApps.startShortcut(pkgName, shortcutId, bounds, null, userHandle)
                    } catch (e: Exception) {
                        activity.showErrorToast(e)
                    }
                }
            }

            // Pin action on button or long click
            val pinAction = {
                listener.onAnyClick()
                if (activity is MainActivity) {
                    activity.pinShortcutToHome(shortcutInfo)
                }
                dismiss()
            }

            itemBinding.shortcutItemPin.setOnClickListener { pinAction() }
            itemBinding.root.setOnLongClickListener {
                pinAction()
                true
            }

            binding.popupShortcutsContainer.addView(itemBinding.root)
        }
    }

    private fun setupQuickActionsBar() {
        val canRename = (gridItem.type == ITEM_TYPE_ICON || gridItem.type == ITEM_TYPE_FOLDER)
        val canHide = gridItem.type == ITEM_TYPE_ICON && isOnAllAppsFragment
        val canResize = gridItem.type == ITEM_TYPE_WIDGET && gridItem.className != BUILT_IN_CLOCK_CLASS_NAME
        val canUninstall = gridItem.type == ITEM_TYPE_ICON &&
            activity.canAppBeUninstalled(gridItem.packageName) &&
            gridItem.packageName != activity.packageName
        val canRemove = !isOnAllAppsFragment
        val canLock = gridItem.type == ITEM_TYPE_ICON

        binding.popupActionRemoveBtn.beVisibleIf(canRemove)
        binding.popupActionRenameBtn.beVisibleIf(canRename)
        binding.popupActionHideBtn.beVisibleIf(canHide)
        binding.popupActionUninstallBtn.beVisibleIf(canUninstall)
        binding.popupActionResizeBtn.beVisibleIf(canResize)
        binding.popupActionLockBtn.beVisibleIf(canLock)

        if (canLock) {
            val isLocked = AppLockManager.isAppLocked(gridItem.packageName)
            if (isLocked) {
                binding.popupActionLockBtn.setImageResource(R.drawable.ic_lock_open_vector)
                binding.popupActionLockBtn.contentDescription = activity.getString(R.string.unlock_app)
            } else {
                binding.popupActionLockBtn.setImageResource(R.drawable.ic_lock_vector)
                binding.popupActionLockBtn.contentDescription = activity.getString(R.string.lock_app)
            }

            binding.popupActionLockBtn.setOnClickListener {
                listener.onAnyClick()
                listener.toggleLock(gridItem)
                dismiss()
            }
        }

        binding.popupActionRemoveBtn.setOnClickListener {
            listener.onAnyClick()
            listener.remove(gridItem)
            dismiss()
        }

        binding.popupActionRenameBtn.setOnClickListener {
            listener.onAnyClick()
            listener.rename(gridItem)
            dismiss()
        }

        binding.popupActionHideBtn.setOnClickListener {
            listener.onAnyClick()
            listener.hide(gridItem)
            dismiss()
        }

        binding.popupActionUninstallBtn.setOnClickListener {
            listener.onAnyClick()
            listener.uninstall(gridItem)
            dismiss()
        }

        binding.popupActionResizeBtn.setOnClickListener {
            listener.onAnyClick()
            listener.resize(gridItem)
            dismiss()
        }
    }

    private fun setupFallbackActions() {
        val canRename = (gridItem.type == ITEM_TYPE_ICON || gridItem.type == ITEM_TYPE_FOLDER)
        val canHide = gridItem.type == ITEM_TYPE_ICON && isOnAllAppsFragment
        val canResize = gridItem.type == ITEM_TYPE_WIDGET && gridItem.className != BUILT_IN_CLOCK_CLASS_NAME
        val canUninstall = gridItem.type == ITEM_TYPE_ICON &&
            activity.canAppBeUninstalled(gridItem.packageName) &&
            gridItem.packageName != activity.packageName
        val canRemove = !isOnAllAppsFragment
        val canLock = gridItem.type == ITEM_TYPE_ICON

        binding.fallbackActionRemove.beVisibleIf(canRemove)
        binding.fallbackActionRename.beVisibleIf(canRename)
        binding.fallbackActionHide.beVisibleIf(canHide)
        binding.fallbackActionUninstall.beVisibleIf(canUninstall)
        binding.fallbackActionResize.beVisibleIf(canResize)
        binding.fallbackActionLock.beVisibleIf(canLock)

        if (canLock) {
            val isLocked = AppLockManager.isAppLocked(gridItem.packageName)
            if (isLocked) {
                binding.fallbackLockIcon.setImageResource(R.drawable.ic_lock_open_vector)
                binding.fallbackLockText.text = activity.getString(R.string.unlock_app)
            } else {
                binding.fallbackLockIcon.setImageResource(R.drawable.ic_lock_vector)
                binding.fallbackLockText.text = activity.getString(R.string.lock_app)
            }

            binding.fallbackActionLock.setOnClickListener {
                listener.onAnyClick()
                listener.toggleLock(gridItem)
                dismiss()
            }
        }

        binding.fallbackActionRemove.setOnClickListener {
            listener.onAnyClick()
            listener.remove(gridItem)
            dismiss()
        }

        binding.fallbackActionRename.setOnClickListener {
            listener.onAnyClick()
            listener.rename(gridItem)
            dismiss()
        }

        binding.fallbackActionHide.setOnClickListener {
            listener.onAnyClick()
            listener.hide(gridItem)
            dismiss()
        }

        binding.fallbackActionUninstall.setOnClickListener {
            listener.onAnyClick()
            listener.uninstall(gridItem)
            dismiss()
        }

        binding.fallbackActionResize.setOnClickListener {
            listener.onAnyClick()
            listener.resize(gridItem)
            dismiss()
        }
    }

    private fun launchSetAsDefault() {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        val intent = intents.firstOrNull { it.resolveActivity(activity.packageManager) != null }
        if (intent != null) {
            activity.startActivity(intent)
        }
    }

    fun show(anchorView: View) {
        val displayMetrics = activity.resources.displayMetrics
        val density = displayMetrics.density
        val screenWidth = activity.realScreenSize.x
        val screenHeight = activity.realScreenSize.y

        val horizontalMargin = (12 * density).toInt()
        val maxAllowedWidth = (screenWidth - 2 * horizontalMargin).coerceAtLeast(100)
        val targetMeasureWidth = minOf(maxAllowedWidth, (280 * density).toInt())

        // Measure content view with capped width
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(targetMeasureWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        )
        val popupWidth = binding.root.measuredWidth.coerceAtMost(maxAllowedWidth)
        val popupHeight = binding.root.measuredHeight

        // Determine icon center and vertical bounds
        val iconCenterX: Float
        val iconTop: Float
        val iconBottom: Float
        if (iconRect != null && iconRect.width() > 0) {
            iconCenterX = iconRect.exactCenterX()
            iconTop = iconRect.top.toFloat()
            iconBottom = iconRect.bottom.toFloat()
        } else {
            iconCenterX = anchorView.x
            iconTop = anchorView.y
            iconBottom = anchorView.y + (48 * density)
        }

        // Horizontal positioning: centered above icon, clamped inside screen padding
        val idealX = (iconCenterX - popupWidth / 2f).toInt()
        val minX = horizontalMargin
        val maxX = (screenWidth - popupWidth - horizontalMargin).coerceAtLeast(minX)
        val popupX = if (maxX >= minX) idealX.coerceIn(minX, maxX) else minX

        // Vertical positioning: placed above icon if space permits, else below
        val verticalMargin = (6 * density).toInt()
        val statusBarHeight = (36 * density).toInt()
        val navBarHeight = (56 * density).toInt()

        val spaceAbove = iconTop - statusBarHeight
        val placeAbove = spaceAbove >= popupHeight + verticalMargin

        val minY = statusBarHeight
        val maxY = (screenHeight - popupHeight - navBarHeight).coerceAtLeast(minY)
        val popupY = if (placeAbove) {
            (iconTop - popupHeight - verticalMargin).toInt().coerceAtLeast(minY)
        } else {
            (iconBottom + verticalMargin).toInt().coerceIn(minY, maxY)
        }

        // Arrow positioning: align with icon center
        val arrowWidth = (16 * density).toInt()
        val cornerRadius = (22 * density).toInt()
        val relativeCenter = iconCenterX - popupX
        val minArrow = cornerRadius.toFloat()
        val maxArrow = (popupWidth - cornerRadius - arrowWidth).toFloat().coerceAtLeast(minArrow)
        val idealArrow = relativeCenter - arrowWidth / 2f
        val arrowLeft = if (maxArrow >= minArrow) idealArrow.coerceIn(minArrow, maxArrow) else minArrow

        if (placeAbove) {
            binding.popupArrowBottom.beVisible()
            binding.popupArrowTop.beGone()
            binding.popupArrowBottom.translationX = arrowLeft
        } else {
            binding.popupArrowTop.beVisible()
            binding.popupArrowBottom.beGone()
            binding.popupArrowTop.translationX = arrowLeft
        }

        showAtLocation(anchorView, Gravity.NO_GRAVITY, popupX, popupY)
    }
}
