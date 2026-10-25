package shura.home

import org.fossify.commons.FossifyApp
import shura.home.helpers.LogCatcher

class VianApp : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        // Initialize LogCatcher at the absolute earliest point in the application lifecycle
        LogCatcher.init(this)
    }
}
