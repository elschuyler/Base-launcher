package org.fossify.home.dialogs

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.home.R
import org.fossify.home.activities.MainActivity
import org.fossify.home.databinding.DialogEditItemBinding
import org.fossify.home.extensions.getDrawableForPackageName
import org.fossify.home.extensions.homeScreenGridItemsDB
import org.fossify.home.extensions.launchersDB
import org.fossify.home.helpers.CustomIconManager
import org.fossify.home.helpers.ITEM_TYPE_FOLDER
import org.fossify.home.helpers.ITEM_TYPE_ICON
import org.fossify.home.helpers.ItemGestureConfig
import org.fossify.home.helpers.ItemGestureManager
import org.fossify.home.helpers.LauncherActionHandler
import org.fossify.home.helpers.LogCatcher
import org.fossify.home.models.HomeScreenGridItem

class EditItemDialog(
    val activity: Activity,
    val item: HomeScreenGridItem,
    val callback: () -> Unit
) {
    private var pendingCustomIcon: Bitmap? = null
    private var isIconReset = false

    init {
        val binding = DialogEditItemBinding.inflate(activity.layoutInflater)
        binding.editItemEdittext.setText(item.title)

        // Set initial icon preview
        if (item.icon != null) {
            binding.editItemIcon.setImageBitmap(item.icon)
        } else if (item.drawable != null) {
            binding.editItemIcon.setImageDrawable(item.drawable)
        } else if (item.packageName.isNotEmpty()) {
            binding.editItemIcon.setImageDrawable(activity.getDrawableForPackageName(item.packageName))
        }

        binding.editItemIconContainer.setOnClickListener {
            showIconPickerOptions(binding)
        }

        // Setup Gestures & Popup Widget configuration
        val gestureConfig = if (item.id != null) {
            ItemGestureManager.getOrCreateConfig(activity, item.id!!)
        } else {
            ItemGestureConfig()
        }

        setupWidgetSection(binding, gestureConfig)
        setupGesturesSection(binding, gestureConfig)

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.edit_item) { alertDialog ->
                    alertDialog.showKeyboard(binding.editItemEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.editItemEdittext.value
                        if (newTitle.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.value_cannot_be_empty)
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            val titleChanged = newTitle != item.title
                            if (titleChanged) {
                                item.title = newTitle
                                if (item.id != null) {
                                    activity.homeScreenGridItemsDB.updateItemTitle(newTitle, item.id!!)
                                }
                                if (item.type == ITEM_TYPE_ICON && item.packageName.isNotEmpty()) {
                                    activity.homeScreenGridItemsDB.updateAppTitle(newTitle, item.packageName)
                                    activity.launchersDB.updateAppTitle(newTitle, item.packageName)
                                }
                            }

                            if (isIconReset) {
                                item.icon = null
                                if (item.id != null) {
                                    activity.homeScreenGridItemsDB.updateItemIcon(null, item.id!!)
                                }
                                if (item.type == ITEM_TYPE_ICON && item.packageName.isNotEmpty()) {
                                    CustomIconManager.removeCustomIcon(activity, item.packageName)
                                    activity.homeScreenGridItemsDB.updateAppIcon(null, item.packageName)
                                }
                            } else if (pendingCustomIcon != null) {
                                val downsampled = CustomIconManager.downsampleBitmap(pendingCustomIcon!!)
                                item.icon = downsampled
                                if (item.id != null) {
                                    activity.homeScreenGridItemsDB.updateItemIcon(downsampled, item.id!!)
                                }
                                if (item.type == ITEM_TYPE_ICON && item.packageName.isNotEmpty()) {
                                    CustomIconManager.saveCustomIcon(activity, item.packageName, downsampled)
                                    activity.homeScreenGridItemsDB.updateAppIcon(downsampled, item.packageName)
                                }
                            }

                            // Save gesture actions
                            if (item.id != null) {
                                ItemGestureManager.setGestureAction(
                                    activity,
                                    item.id!!,
                                    ItemGestureManager.GESTURE_DOUBLE_TAP,
                                    gestureConfig.doubleTapAction,
                                    gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_DOUBLE_TAP)
                                )
                                ItemGestureManager.setGestureAction(
                                    activity,
                                    item.id!!,
                                    ItemGestureManager.GESTURE_SWIPE_UP,
                                    gestureConfig.swipeUpAction,
                                    gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_UP)
                                )
                                ItemGestureManager.setGestureAction(
                                    activity,
                                    item.id!!,
                                    ItemGestureManager.GESTURE_SWIPE_DOWN,
                                    gestureConfig.swipeDownAction,
                                    gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_DOWN)
                                )
                                ItemGestureManager.setGestureAction(
                                    activity,
                                    item.id!!,
                                    ItemGestureManager.GESTURE_SWIPE_LEFT,
                                    gestureConfig.swipeLeftAction,
                                    gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_LEFT)
                                )
                                ItemGestureManager.setGestureAction(
                                    activity,
                                    item.id!!,
                                    ItemGestureManager.GESTURE_SWIPE_RIGHT,
                                    gestureConfig.swipeRightAction,
                                    gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_RIGHT)
                                )
                            }

                            activity.runOnUiThread {
                                callback()
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }

    private fun setupWidgetSection(binding: DialogEditItemBinding, gestureConfig: ItemGestureConfig) {
        fun refreshWidgetUI() {
            val linkedId = gestureConfig.linkedWidgetId
            if (linkedId != -1) {
                val providerInfo = AppWidgetManager.getInstance(activity).getAppWidgetInfo(linkedId)
                val widgetTitle = providerInfo?.loadLabel(activity.packageManager) ?: "Widget #$linkedId"
                binding.itemEditWidgetTitle.text = widgetTitle
                binding.itemEditWidgetStatus.text = "Linked • Ready on gesture/popup"
                binding.itemEditBtnLinkWidget.text = activity.getString(R.string.change_widget)
                binding.itemEditBtnUnlinkWidget.beVisible()
            } else {
                binding.itemEditWidgetTitle.text = activity.getString(R.string.no_popup_widget_linked)
                binding.itemEditWidgetStatus.text = "Tap to link on-demand shutter"
                binding.itemEditBtnLinkWidget.text = activity.getString(R.string.link_widget)
                binding.itemEditBtnUnlinkWidget.beGone()
            }
        }

        refreshWidgetUI()

        binding.itemEditBtnLinkWidget.setOnClickListener {
            SelectPopupWidgetDialog(activity, item) { selectedInfo ->
                if (activity is MainActivity && item.id != null) {
                    activity.bindAndLinkPopupWidget(item, selectedInfo)
                    gestureConfig.linkedWidgetId = ItemGestureManager.getLinkedWidget(activity, item.id!!)?.first ?: -1
                    gestureConfig.linkedWidgetProvider = selectedInfo.provider.className
                    refreshWidgetUI()
                }
            }.show()
        }

        binding.itemEditBtnUnlinkWidget.setOnClickListener {
            if (item.id != null) {
                if (activity is MainActivity) {
                    activity.unlinkPopupWidget(item)
                } else {
                    ItemGestureManager.unlinkWidget(activity, item.id!!)
                }
                gestureConfig.linkedWidgetId = -1
                gestureConfig.linkedWidgetProvider = ""
                refreshWidgetUI()
            }
        }
    }

    private fun setupGesturesSection(binding: DialogEditItemBinding, gestureConfig: ItemGestureConfig) {
        val isFolder = item.type == ITEM_TYPE_FOLDER

        fun refreshGestureBadges() {
            binding.itemGestureDoubleTapBadge.text = LauncherActionHandler.getActionLabel(activity, gestureConfig.doubleTapAction, gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_DOUBLE_TAP))
            binding.itemGestureSwipeUpBadge.text = LauncherActionHandler.getActionLabel(activity, gestureConfig.swipeUpAction, gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_UP))
            binding.itemGestureSwipeDownBadge.text = LauncherActionHandler.getActionLabel(activity, gestureConfig.swipeDownAction, gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_DOWN))
            binding.itemGestureSwipeLeftBadge.text = LauncherActionHandler.getActionLabel(activity, gestureConfig.swipeLeftAction, gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_LEFT))
            binding.itemGestureSwipeRightBadge.text = LauncherActionHandler.getActionLabel(activity, gestureConfig.swipeRightAction, gestureConfig.getTargetPackage(ItemGestureManager.GESTURE_SWIPE_RIGHT))
        }

        refreshGestureBadges()

        binding.itemGestureDoubleTapHolder.setOnClickListener {
            SelectActionDialog(
                activity = activity,
                gestureLabel = activity.getString(R.string.gesture_double_tap),
                currentAction = gestureConfig.doubleTapAction,
                isFolderItem = isFolder
            ) { action, pkg, _ ->
                gestureConfig.doubleTapAction = action
                if (pkg.isNotEmpty()) gestureConfig.setTargetPackage(ItemGestureManager.GESTURE_DOUBLE_TAP, pkg)
                refreshGestureBadges()
            }.show()
        }

        binding.itemGestureSwipeUpHolder.setOnClickListener {
            SelectActionDialog(
                activity = activity,
                gestureLabel = activity.getString(R.string.gesture_swipe_up),
                currentAction = gestureConfig.swipeUpAction,
                isFolderItem = isFolder
            ) { action, pkg, _ ->
                gestureConfig.swipeUpAction = action
                if (pkg.isNotEmpty()) gestureConfig.setTargetPackage(ItemGestureManager.GESTURE_SWIPE_UP, pkg)
                refreshGestureBadges()
            }.show()
        }

        binding.itemGestureSwipeDownHolder.setOnClickListener {
            SelectActionDialog(
                activity = activity,
                gestureLabel = activity.getString(R.string.gesture_swipe_down),
                currentAction = gestureConfig.swipeDownAction,
                isFolderItem = isFolder
            ) { action, pkg, _ ->
                gestureConfig.swipeDownAction = action
                if (pkg.isNotEmpty()) gestureConfig.setTargetPackage(ItemGestureManager.GESTURE_SWIPE_DOWN, pkg)
                refreshGestureBadges()
            }.show()
        }

        binding.itemGestureSwipeLeftHolder.setOnClickListener {
            SelectActionDialog(
                activity = activity,
                gestureLabel = activity.getString(R.string.gesture_swipe_left),
                currentAction = gestureConfig.swipeLeftAction,
                isFolderItem = isFolder
            ) { action, pkg, _ ->
                gestureConfig.swipeLeftAction = action
                if (pkg.isNotEmpty()) gestureConfig.setTargetPackage(ItemGestureManager.GESTURE_SWIPE_LEFT, pkg)
                refreshGestureBadges()
            }.show()
        }

        binding.itemGestureSwipeRightHolder.setOnClickListener {
            SelectActionDialog(
                activity = activity,
                gestureLabel = activity.getString(R.string.gesture_swipe_right),
                currentAction = gestureConfig.swipeRightAction,
                isFolderItem = isFolder
            ) { action, pkg, _ ->
                gestureConfig.swipeRightAction = action
                if (pkg.isNotEmpty()) gestureConfig.setTargetPackage(ItemGestureManager.GESTURE_SWIPE_RIGHT, pkg)
                refreshGestureBadges()
            }.show()
        }
    }

    private fun showIconPickerOptions(binding: DialogEditItemBinding) {
        val options = arrayOf(
            activity.getString(R.string.choose_from_gallery),
            activity.getString(R.string.choose_from_apps),
            activity.getString(R.string.reset_to_default_icon)
        )

        activity.getAlertDialogBuilder()
            .setTitle(R.string.change_icon)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (activity is MainActivity) {
                            activity.launchIconPicker { bitmap ->
                                if (bitmap != null) {
                                    pendingCustomIcon = bitmap
                                    isIconReset = false
                                    binding.editItemIcon.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                    1 -> {
                        PickAppIconDialog(activity) { bitmap ->
                            pendingCustomIcon = bitmap
                            isIconReset = false
                            binding.editItemIcon.setImageBitmap(bitmap)
                        }
                    }
                    2 -> {
                        isIconReset = true
                        pendingCustomIcon = null
                        if (item.drawable != null) {
                            binding.editItemIcon.setImageDrawable(item.drawable)
                        } else if (item.packageName.isNotEmpty()) {
                            binding.editItemIcon.setImageDrawable(activity.getDrawableForPackageName(item.packageName))
                        }
                    }
                }
            }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .show()
    }
}
