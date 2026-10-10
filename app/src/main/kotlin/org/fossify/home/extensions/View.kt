package org.fossify.home.extensions

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.home.R

fun View.animateScale(
    from: Float,
    to: Float,
    duration: Long,
) = animate()
    .scaleX(to)
    .scaleY(to)
    .setDuration(duration)
    .setInterpolator(AccelerateDecelerateInterpolator())
    .withStartAction {
        scaleX = from
        scaleY = from
    }

fun View.setupDrawerBackground() {
    val backgroundColor = context.getProperBackgroundColor()
    // Semi-transparent background scrim (~82% alpha) to match modern drawer style
    val translucentBg = ColorUtils.setAlphaComponent(backgroundColor, 210)
    val bgDrawable = ResourcesCompat.getDrawable(
        context.resources, R.drawable.bottom_sheet_bg, context.theme
    )?.mutate()

    if (bgDrawable is GradientDrawable) {
        bgDrawable.setColor(translucentBg)
        background = bgDrawable
    } else {
        background = translucentBg.toDrawable()
    }
}