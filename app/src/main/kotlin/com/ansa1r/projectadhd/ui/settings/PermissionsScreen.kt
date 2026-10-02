package com.ansa1r.projectadhd.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun PermissionsScreen(viewModel: PermissionsViewModel, onReady: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    RefreshOnResume(viewModel::refresh)
    val ready by rememberUpdatedState(onReady)
    LaunchedEffect(state) { if (state.onboardingReady) ready() }
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            PermissionCard(stringResource(R.string.usage_access), stringResource(R.string.usage_access_explanation),
                state.usageAccess, viewModel::usageSettings)
        }
        item {
            SectionCard {
                Text(stringResource(R.string.overlay_permission), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.overlay_explanation))
                Text(permissionText(state.overlay && state.overlaySupported))
                if (!state.overlaySupported) Text(stringResource(R.string.overlay_fallback_hint))
                else if (!state.overlay) BrandButton(onClick = viewModel::overlaySettings) { Text(stringResource(R.string.grant_overlay)) }
            }
        }
        item {
            PermissionCard(stringResource(R.string.notification_permission), stringResource(R.string.notification_explanation),
                state.notifications, onGrant = {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else viewModel.notificationSettings()
                })
        }
        item {
            SectionCard {
                Text(stringResource(R.string.channels_status), style = MaterialTheme.typography.titleMedium)
                Text(permissionText(state.monitoringChannel && state.interventionChannel))
                Text(stringResource(R.string.channels_explanation))
                BrandOutlinedButton(onClick = viewModel::notificationSettings) { Text(stringResource(R.string.notification_settings)) }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, explanation: String, allowed: Boolean, onGrant: () -> Unit) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(explanation)
        Text(permissionText(allowed))
        if (!allowed) BrandButton(onClick = onGrant) { Text(stringResource(R.string.allow_permission)) }
    }
}
