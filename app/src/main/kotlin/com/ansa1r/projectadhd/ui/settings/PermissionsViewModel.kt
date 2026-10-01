package com.ansa1r.projectadhd.ui.settings

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionsViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(container.permissions.state())
    val state = mutable.asStateFlow()
    fun refresh() { mutable.value = container.permissions.state() }
    fun usageSettings() { if (!container.permissions.openUsageSettings()) inform(R.string.error_settings) }
    fun overlaySettings() { if (!container.permissions.openOverlaySettings()) inform(R.string.error_settings) }
    fun notificationSettings() { if (!container.permissions.openNotificationSettings()) inform(R.string.error_settings) }
}
