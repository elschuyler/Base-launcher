package org.fossify.home.dialogs

import android.app.Activity
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
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
                                    // Option B: Global package level custom icon
                                    CustomIconManager.saveCustomIcon(activity, item.packageName, downsampled)
                                    activity.homeScreenGridItemsDB.updateAppIcon(downsampled, item.packageName)
                                }
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
                        // Pick from Gallery / Photo Picker
                        (activity as? MainActivity)?.launchIconPicker { bitmap ->
                            if (bitmap != null) {
                                pendingCustomIcon = bitmap
                                isIconReset = false
                                binding.editItemIcon.setImageBitmap(bitmap)
                            }
                        }
                    }
                    1 -> {
                        // Pick from installed apps
                        PickAppIconDialog(activity) { bitmap ->
                            pendingCustomIcon = bitmap
                            isIconReset = false
                            binding.editItemIcon.setImageBitmap(bitmap)
                        }
                    }
                    2 -> {
                        // Reset to default
                        pendingCustomIcon = null
                        isIconReset = true
                        if (item.type == ITEM_TYPE_FOLDER) {
                            if (item.drawable != null && item.icon == null) {
                                binding.editItemIcon.setImageDrawable(item.drawable)
                            } else {
                                binding.editItemIcon.setImageResource(org.fossify.commons.R.drawable.ic_folder_vector)
                            }
                        } else if (item.packageName.isNotEmpty()) {
                            CustomIconManager.removeCustomIcon(activity, item.packageName)
                            binding.editItemIcon.setImageDrawable(activity.getDrawableForPackageName(item.packageName))
                        }
                    }
                }
            }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .show()
    }
}
