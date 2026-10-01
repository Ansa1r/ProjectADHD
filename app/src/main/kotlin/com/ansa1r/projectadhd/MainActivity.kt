package com.ansa1r.projectadhd

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ansa1r.projectadhd.ui.startup.StartupHost
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ansa1r.projectadhd.navigation.AppNavigation
import com.ansa1r.projectadhd.ui.theme.ProjectADHDTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val OPEN_HABITS = "com.ansa1r.projectadhd.OPEN_HABITS"
        fun habitsIntent(context: Context) = Intent(context, MainActivity::class.java)
            .setAction(OPEN_HABITS).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    private var habitsRequest by mutableStateOf(0)
    private val container get() = (application as ProjectADHDApplication).container
    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val startup = !container.startupShown && savedInstanceState == null && intent.action == Intent.ACTION_MAIN
        container.startupShown = true
        systemSplash.setOnExitAnimationListener { provider ->
            // No artificial system-splash hold; reveal the matching Compose scene.
            provider.view.animate().alpha(0f).setDuration(if (startup) 120L else 0L)
                .withEndAction { provider.remove() }.start()
        }
        updateSystemBars(startup)
        if (savedInstanceState == null && intent.action == OPEN_HABITS) habitsRequest++
        setContent { ProjectADHDTheme {
            StartupHost(startup, habitsRequest, ::updateSystemBars) { AppNavigation(container, habitsRequest) }
        } }
    }
    private fun updateSystemBars(startup: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (startup) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == OPEN_HABITS) habitsRequest++
    }
    override fun onStart() {
        super.onStart()
        container.overlays.appVisibility(true)
    }
    override fun onStop() {
        container.overlays.appVisibility(false)
        super.onStop()
    }
}
