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
        private const val OPEN_STOP = "com.ansa1r.projectadhd.CONFIRM_STOP"
        fun stopConfirmationIntent(context: Context) = Intent(context, MainActivity::class.java)
            .setAction(OPEN_STOP).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        private const val OPEN_HABITS = "com.ansa1r.projectadhd.OPEN_HABITS"
        fun habitsIntent(context: Context) = Intent(context, MainActivity::class.java)
            .setAction(OPEN_HABITS).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    private var startupEntry by mutableStateOf(0L)
    private var habitsRequest by mutableStateOf(0)
    private var stopRequest by mutableStateOf(0)
    private val container get() = (application as ProjectADHDApplication).container
    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        systemSplash.setOnExitAnimationListener { provider ->
            // No artificial system-splash hold; reveal the matching Compose scene.
            provider.view.animate().alpha(0f).setDuration(80L)
                .withEndAction { provider.remove() }.start()
        }
        updateSystemBars(true)
        habitsRequest = savedInstanceState?.getInt("habits_request") ?: 0
        stopRequest = savedInstanceState?.getInt("stop_request") ?: 0
        if (savedInstanceState == null && intent.action == OPEN_HABITS) habitsRequest++
        if (savedInstanceState == null && intent.action == OPEN_STOP) stopRequest++
        setContent { ProjectADHDTheme {
            StartupHost(startupEntry, ::updateSystemBars) { AppNavigation(container, habitsRequest, stopRequest) }
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
        if (intent.action == OPEN_STOP) stopRequest++
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("habits_request", habitsRequest)
        outState.putInt("stop_request", stopRequest)
        super.onSaveInstanceState(outState)
    }
    override fun onStart() {
        super.onStart()
        container.uiVisibility(true)
        container.overlays.appVisibility(true)
        container.uiEntries.onStart(android.os.SystemClock.elapsedRealtime())?.let { startupEntry = it }
    }
    override fun onDestroy() {
        container.uiEntries.onUiDestroyed(isChangingConfigurations)
        super.onDestroy()
    }
    override fun onResume() {
        super.onResume()
        container.onUiResumed()
    }
    override fun onStop() {
        startupEntry = 0L
        container.uiEntries.onStop(isChangingConfigurations, android.os.SystemClock.elapsedRealtime(), System.currentTimeMillis(), isFinishing)
        if (!isChangingConfigurations) container.saveUiBackground()
        container.uiVisibility(false)
        container.overlays.appVisibility(false)
        super.onStop()
    }
}
