package org.fossify.home.helpers

import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import org.fossify.home.models.AppLauncher

object IconCache {
    private val drawableCache = LruCache<String, Drawable>(256)

    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    var launchers: List<AppLauncher>
        get() = cachedLaunchers
        set(value) {
            synchronized(this) {
                cachedLaunchers = value
            }
        }

    fun getDrawable(key: String): Drawable? = synchronized(drawableCache) {
        drawableCache.get(key)
    }

    fun putDrawable(key: String, drawable: Drawable) = synchronized(drawableCache) {
        drawableCache.put(key, drawable)
    }

    fun removeDrawable(key: String) = synchronized(drawableCache) {
        drawableCache.remove(key)
    }

    fun clear() {
        launchers = emptyList()
        synchronized(drawableCache) {
            drawableCache.evictAll()
        }
    }
}
