package org.fossify.home

import org.fossify.commons.FossifyApp
import org.fossify.home.helpers.LogCatcher

class VianApp : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        // Initialize LogCatcher at the absolute earliest point in the application lifecycle
        LogCatcher.init(this)
    }
}
