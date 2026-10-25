package shura.home.helpers

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
import shura.home.R
import kotlin.math.max

object IconDecorator {

    /**
     * Decorates an application icon for archived apps according to standard Android 15 / Pixel launcher UX:
     * 1. Draws the base icon with dimmed alpha (~55%)
     * 2. Overlays a prominent cloud download glyph centered directly OVER the icon with a dark disc
     */
    fun decorateArchivedIcon(context: Context, original: Drawable): Drawable {
        val width = max(original.intrinsicWidth, 192)
        val height = max(original.intrinsicHeight, 192)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw base icon with ~55% alpha (140/255) to signify offloaded state
        original.setBounds(0, 0, width, height)
        val prevAlpha = original.alpha
        original.alpha = 140
        original.draw(canvas)
        original.alpha = prevAlpha

        // 2. Draw circular backdrop centered directly over the icon
        val centerX = width / 2f
        val centerY = height / 2f
        val badgeRadius = width * 0.28f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D91C1B1F") // 85% dark surface
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, badgeRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF") // Subtle 30% white stroke for boundary
            style = Paint.Style.STROKE
            strokeWidth = max(width * 0.016f, 2f)
        }
        canvas.drawCircle(centerX, centerY, badgeRadius, strokePaint)

        // 3. Draw cloud download vector centered inside the disc
        val downloadDrawable = ContextCompat.getDrawable(context, R.drawable.ic_cloud_download_vector)?.mutate()
        if (downloadDrawable != null) {
            val iconInnerHalf = badgeRadius * 0.68f
            downloadDrawable.setBounds(
                (centerX - iconInnerHalf).toInt(),
                (centerY - iconInnerHalf).toInt(),
                (centerX + iconInnerHalf).toInt(),
                (centerY + iconInnerHalf).toInt()
            )
            downloadDrawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            downloadDrawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * Decorates an application icon for locked apps according to standard launcher UX:
     * 1. Draws the base icon dimmed to ~55% alpha (140/255)
     * 2. Overlays a prominent padlock glyph centered directly OVER the icon with a dark disc
     */
    fun decorateLockedIcon(context: Context, original: Drawable): Drawable {
        val width = max(original.intrinsicWidth, 192)
        val height = max(original.intrinsicHeight, 192)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw base icon dimmed to ~55% alpha (140/255)
        original.setBounds(0, 0, width, height)
        val prevAlpha = original.alpha
        original.alpha = 140
        original.draw(canvas)
        original.alpha = prevAlpha

        // 2. Draw circular backdrop centered directly over the icon
        val centerX = width / 2f
        val centerY = height / 2f
        val badgeRadius = width * 0.28f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D91C1B1F") // 85% dark surface
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, badgeRadius, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF") // Subtle 30% white stroke for boundary
            style = Paint.Style.STROKE
            strokeWidth = max(width * 0.016f, 2f)
        }
        canvas.drawCircle(centerX, centerY, badgeRadius, strokePaint)

        // 3. Draw lock vector centered inside the disc
        val lockDrawable = ContextCompat.getDrawable(context, R.drawable.ic_lock_vector)?.mutate()
        if (lockDrawable != null) {
            val iconInnerHalf = badgeRadius * 0.68f
            lockDrawable.setBounds(
                (centerX - iconInnerHalf).toInt(),
                (centerY - iconInnerHalf).toInt(),
                (centerX + iconInnerHalf).toInt(),
                (centerY + iconInnerHalf).toInt()
            )
            lockDrawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            lockDrawable.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
