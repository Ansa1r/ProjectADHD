package com.ansa1r.projectadhd.ui.settings

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.settings.BlockingOpacity
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BlockingSettingsState(
    val percent: Int = BlockingOpacity.DEFAULT_PERCENT,
    val savedPercent: Int = BlockingOpacity.DEFAULT_PERCENT,
    val loading: Boolean = true,
    val saving: Boolean = false
)

class BlockingSettingsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(BlockingSettingsState())
    val state = mutable.asStateFlow()
    private var requestVersion = 0L
    private var locallyEdited = false

    init {
        execute {
            container.preferences.blockingOverlayOpacity.collect { percent ->
                mutable.update { it.copy(savedPercent = percent,
                    percent = if (locallyEdited) it.percent else percent, loading = false) }
            }
        }
    }

    fun change(value: Float) {
        if (mutable.value.loading) return
        val percent = BlockingOpacity.fromSlider(value)
        if (percent == mutable.value.percent) return
        locallyEdited = true
        val request = ++requestVersion
        mutable.update { it.copy(percent = percent, saving = true) }
        // Application scope lets an already requested save finish after leaving this screen.
        container.applicationScope.launch {
            try {
                container.preferences.setBlockingOpacity(percent)
                if (request == requestVersion) mutable.update { it.copy(savedPercent = percent, saving = false) }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (request == requestVersion) {
                    val saved = try { container.preferences.blockingOverlayOpacity.first() }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { mutable.value.savedPercent }
                    mutable.update { it.copy(percent = saved, savedPercent = saved, saving = false) }
                }
                inform(R.string.opacity_save_error)
            }
        }
    }
}
