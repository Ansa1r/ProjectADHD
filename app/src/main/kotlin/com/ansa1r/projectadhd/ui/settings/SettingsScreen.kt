package com.ansa1r.projectadhd.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.ScreenList
import com.ansa1r.projectadhd.ui.components.SectionCard
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun SettingsScreen(
    openPermissions: () -> Unit,
    openBlocking: () -> Unit,
    openTheme: () -> Unit,
    openPrivacy: () -> Unit,
    openDeveloper: () -> Unit,
    stopMonitoring: () -> Unit
) {
    ScreenList {
        item { SettingsMenuItem(R.string.permissions_title, R.drawable.ic_permission, openPermissions) }
        item { SettingsMenuItem(R.string.blocking_settings, R.drawable.ic_lock, openBlocking) }
        item { SettingsMenuItem(R.string.interface_theme, R.drawable.ic_theme, openTheme) }
        item { SettingsMenuItem(R.string.privacy, R.drawable.ic_permission, openPrivacy) }
        if (BuildConfig.DEBUG) item { SettingsMenuItem(R.string.developer, R.drawable.ic_developer, openDeveloper) }
        item {
            OutlinedButton(onClick = stopMonitoring, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stop_monitoring))
            }
        }
    }
}

@Composable
fun SettingsMenuItem(title: Int, icon: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.Surface, contentColor = BrandColors.Text),
        border = BorderStroke(1.dp, BrandColors.PurpleOutline)) {
        Row(Modifier.padding(20.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = BrandColors.Primary, modifier = Modifier.size(24.dp))
            Text(stringResource(title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = BrandColors.Primary)
        }
    }
}

@Composable
fun SettingsPlaceholderScreen(title: Int, body: Int, showPrivacyInfo: Boolean = false) {
    ScreenList {
        item {
            SectionCard {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(body))
                if (showPrivacyInfo) Text(stringResource(R.string.local_data_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DeveloperScreen(openDebug: () -> Unit) {
    if (!BuildConfig.DEBUG) return
    ScreenList {
        item { Text(stringResource(R.string.debug_intro)) }
        item { SettingsMenuItem(R.string.debug, R.drawable.ic_developer, openDebug) }
    }
}
