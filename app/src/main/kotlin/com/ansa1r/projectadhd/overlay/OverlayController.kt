package com.ansa1r.projectadhd.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.domain.settings.BlockingOpacity
import com.ansa1r.projectadhd.MainActivity
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.monitoring.ExcludedApps
import com.ansa1r.projectadhd.monitoring.PermissionManager
import com.ansa1r.projectadhd.ui.mascot.BlockingContent
import com.ansa1r.projectadhd.ui.mascot.PraiseContent
import com.ansa1r.projectadhd.ui.theme.ProjectADHDTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface OverlayResult {
    data object Shown : OverlayResult
    data object Suppressed : OverlayResult
    data class Failed(val reason: String) : OverlayResult
}
data class OverlaySnapshot(
    val visible: Boolean = false, val packageName: String? = null,
    val mood: MascotMood? = null, val test: Boolean = false,
    val lastError: String? = null, val testArmedUntil: Long? = null,
    val blockingOpacityPercent: Int = BlockingOpacity.DEFAULT_PERCENT
)
private data class OverlayContent(val payload: InterventionPayload, val mood: MascotMood, val test: Boolean)

/** Main-thread owner of at most one non-focusable application overlay. No service or polling here. */
class OverlayController(
    private val context: Context, private val permissions: PermissionManager, private val excluded: ExcludedApps
) {
    companion object { const val PRAISE_DURATION_MS = 4_000L; const val DEBUG_DURATION_MS = 10_000L }
    private val mutable = MutableStateFlow(OverlaySnapshot())
    val state = mutable.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var manager: WindowManager? = null
    private var overlayContent by mutableStateOf<OverlayContent?>(null)
    private var blockingOpacityPercent by mutableStateOf(BlockingOpacity.DEFAULT_PERCENT)
    private var foreground: String? = null
    private var appVisible = false
    private var armedMood: MascotMood? = null
    private val dismiss = Runnable { hide() }

    fun setBlockingOpacity(percent: Int) {
        check(Looper.myLooper() == Looper.getMainLooper())
        blockingOpacityPercent = BlockingOpacity.normalize(percent)
        mutable.update { it.copy(blockingOpacityPercent = blockingOpacityPercent) }
    }

    fun appVisibility(visible: Boolean) {
        appVisible = visible
        if (visible) { foreground = null; hide() }
    }
    fun foregroundChanged(packageName: String?) {
        foreground = packageName
        if (appVisible || packageName == null || excluded.contains(packageName) ||
            (state.value.visible && state.value.packageName != packageName)) hide()
    }
    fun armTest(mood: MascotMood) {
        if (!BuildConfig.DEBUG) return
        armedMood = mood
        mutable.update { it.copy(testArmedUntil = System.currentTimeMillis() + 30_000) }
    }
    fun takeTest(packageName: String?, tracked: Boolean): MascotMood? {
        if (!BuildConfig.DEBUG) return null
        val expiry = state.value.testArmedUntil ?: return null
        if (System.currentTimeMillis() > expiry) { cancelTest(); return null }
        if (!tracked || packageName == null || excluded.contains(packageName) || appVisible) return null
        val mood = armedMood
        cancelTest()
        return mood
    }
    fun cancelTest() { armedMood = null; mutable.update { it.copy(testArmedUntil = null) } }

    fun showBlocking(payload: InterventionPayload, test: Boolean = false): OverlayResult = show(payload, MascotMood.BLOCKING, test)
    fun showPraise(payload: InterventionPayload, test: Boolean = false): OverlayResult = show(payload, MascotMood.PRAISE, test)

    fun hideProductionBlock() {
        if (state.value.mood == MascotMood.BLOCKING && !state.value.test) hide()
    }
    fun hide() {
        handler.removeCallbacks(dismiss)
        val old = view
        view = null
        if (old != null) {
            try { manager?.removeViewImmediate(old) }
            catch (error: RuntimeException) { recordError("REMOVE: " + error.javaClass.simpleName) }
            finally { old.disposeComposition() }
        }
        owner?.destroy(); owner = null; manager = null; overlayContent = null
        mutable.update { it.copy(visible = false, packageName = null, mood = null, test = false) }
    }
    fun recordError(reason: String) { mutable.update { it.copy(lastError = reason) } }

    private fun show(payload: InterventionPayload, mood: MascotMood, test: Boolean): OverlayResult {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (appVisible || foreground != payload.packageName || excluded.contains(payload.packageName)) {
            hide(); return OverlayResult.Suppressed
        }
        if (Build.VERSION.SDK_INT < 26) return fail("API_BELOW_26")
        if (!permissions.canDrawOverlays()) return fail("OVERLAY_PERMISSION_MISSING")
        if (view != null && state.value.packageName == payload.packageName && state.value.mood == mood && state.value.test == test) {
            overlayContent = OverlayContent(payload, mood, test)
            return OverlayResult.Shown
        }
        hide()
        return try {
            val windowContext = if (Build.VERSION.SDK_INT >= 30) {
                val displays = requireNotNull(context.getSystemService(DisplayManager::class.java))
                val display = requireNotNull(displays.getDisplay(Display.DEFAULT_DISPLAY))
                context.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
            } else context
            val wm = requireNotNull(windowContext.getSystemService(WindowManager::class.java))
            val host = OverlayOwner()
            owner = host
            val compose = ComposeView(windowContext).apply {
                setViewTreeLifecycleOwner(host)
                setViewTreeViewModelStoreOwner(host)
                setViewTreeSavedStateRegistryOwner(host)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                isClickable = mood == MascotMood.BLOCKING
                setContent {
                    ProjectADHDTheme {
                        overlayContent?.let { data ->
                            if (data.mood == MascotMood.BLOCKING) BlockingContent(data.payload, data.test, blockingOpacityPercent, ::openHabits)
                            else PraiseContent(data.test)
                        }
                    }
                }
            }
            overlayContent = OverlayContent(payload, mood, test)
            val blocking = mood == MascotMood.BLOCKING
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                if (blocking) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    if (blocking) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = if (blocking) Gravity.CENTER else Gravity.TOP or Gravity.CENTER_HORIZONTAL
                // Android 12+ rejects touch-through above maximum obscuring opacity.
                if (!blocking) alpha = if (Build.VERSION.SDK_INT >= 31) {
                    minOf(0.8f, windowContext.getSystemService(InputManager::class.java)?.maximumObscuringOpacityForTouch ?: 0.8f)
                } else 0.8f
                // BLOCK keeps window alpha=1: only its Compose scrim is translucent.
                if (blocking) alpha = 1f
                setTitle("ProjectADHD " + mood.name)
            }
            manager = wm; view = compose
            wm.addView(compose, params)
            mutable.update { it.copy(visible = true, packageName = payload.packageName, mood = mood, test = test) }
            if (!blocking || test) handler.postDelayed(dismiss, if (blocking) DEBUG_DURATION_MS else PRAISE_DURATION_MS)
            OverlayResult.Shown
        } catch (error: RuntimeException) {
            fail("WINDOW: " + error.javaClass.simpleName + ": " + (error.message ?: ""))
        }
    }
    private fun fail(reason: String): OverlayResult.Failed { hide(); recordError(reason); return OverlayResult.Failed(reason) }
    private fun openHabits() {
        try {
            context.startActivity(MainActivity.habitsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            hide()
        } catch (error: RuntimeException) {
            // Keep the block visible. Home/system navigation and the notification remain available.
            recordError("OPEN_HABITS: " + error.javaClass.simpleName)
        }
    }
}

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore = ViewModelStore()
    init {
        savedState.performAttach(); savedState.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun destroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
