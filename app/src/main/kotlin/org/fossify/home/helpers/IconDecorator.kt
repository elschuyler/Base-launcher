package org.fossify.home.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import org.fossify.home.R
import kotlin.math.max

object IconDecorator {

    /**
     * Decorates an application icon for archived apps according to standard Android launcher UX:
     * 1. Draws the base icon with dimmed alpha (~65%)
     * 2. Overlays a crisp circular badge in the bottom-right corner with a download arrow symbol
     */
    fun decorateArchivedIcon(context: Context, original: Drawable): Drawable {
        val width = max(original.intrinsicWidth, 192)
        val height = max(original.intrinsicHeight, 192)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw base icon with ~65% alpha (165/255)
        original.setBounds(0, 0, width, height)
        val prevAlpha = original.alpha
        original.alpha = 165
        original.draw(canvas)
        original.alpha = prevAlpha

        // 2. Draw circular download badge in bottom-right corner
        val badgeRadius = width * 0.19f
        val badgeCenterX = width - badgeRadius - (width * 0.04f)
        val badgeCenterY = height - badgeRadius - (height * 0.04f)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E61C1B1F") // Dark surface matching M3 dark elevation
            style = Paint.Style.FILL
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF") // Subtle 30% white stroke for crisp boundary
            style = Paint.Style.STROKE
            strokeWidth = max(width * 0.015f, 2f)
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, strokePaint)

        // 3. Draw cloud download vector inside the badge
        val downloadDrawable = ContextCompat.getDrawable(context, R.drawable.ic_cloud_download_vector)?.mutate()
        if (downloadDrawable != null) {
            val iconInnerHalf = badgeRadius * 0.62f
            downloadDrawable.setBounds(
                (badgeCenterX - iconInnerHalf).toInt(),
                (badgeCenterY - iconInnerHalf).toInt(),
                (badgeCenterX + iconInnerHalf).toInt(),
                (badgeCenterY + iconInnerHalf).toInt()
            )
            downloadDrawable.colorFilter = PorterDuffColorFilter(Color.parseColor("#E2E2E6"), PorterDuff.Mode.SRC_IN)
            downloadDrawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * Decorates an application icon for locked apps according to standard Android launcher UX:
     * 1. Draws the base icon at 100% full opacity (no dimming)
     * 2. Overlays a crisp circular badge in the bottom-right corner with a lock symbol
     */
    fun decorateLockedIcon(context: Context, original: Drawable): Drawable {
        val width = max(original.intrinsicWidth, 192)
        val height = max(original.intrinsicHeight, 192)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw base icon at full 100% opacity
        original.setBounds(0, 0, width, height)
        original.draw(canvas)

        // 2. Draw circular badge in bottom-right corner
        val badgeRadius = width * 0.19f
        val badgeCenterX = width - badgeRadius - (width * 0.04f)
        val badgeCenterY = height - badgeRadius - (height * 0.04f)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E61C1B1F") // Dark surface matching M3 dark elevation
            style = Paint.Style.FILL
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF") // Subtle 30% white stroke for crisp boundary
            style = Paint.Style.STROKE
            strokeWidth = max(width * 0.015f, 2f)
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, strokePaint)

        // 3. Draw lock vector inside the badge
        val lockDrawable = ContextCompat.getDrawable(context, R.drawable.ic_lock_vector)?.mutate()
        if (lockDrawable != null) {
            val iconInnerHalf = badgeRadius * 0.58f
            lockDrawable.setBounds(
                (badgeCenterX - iconInnerHalf).toInt(),
                (badgeCenterY - iconInnerHalf).toInt(),
                (badgeCenterX + iconInnerHalf).toInt(),
                (badgeCenterY + iconInnerHalf).toInt()
            )
            lockDrawable.colorFilter = PorterDuffColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
            lockDrawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
