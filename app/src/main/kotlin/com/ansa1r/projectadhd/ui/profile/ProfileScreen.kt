package com.ansa1r.projectadhd.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.ui.mascot.MascotView

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, openProgress: () -> Unit, openAchievements: () -> Unit, editProfile: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(MascotMood.IDLE, Modifier.size(192.dp))
                Text(state.nickname ?: stringResource(R.string.nickname_default), style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.testTag("profile_nickname"))
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.profile_today), style = MaterialTheme.typography.titleLarge)
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                else {
                    Text(stringResource(R.string.today_progress, state.tasks.completed, state.tasks.total), modifier = Modifier.testTag("profile_tasks"))
                    Text(stringResource(R.string.profile_tracked_count, state.trackedCount), modifier = Modifier.testTag("profile_apps"))
                    Text(stringResource(R.string.profile_interventions, state.interventions), modifier = Modifier.testTag("profile_interventions"))
                }
            }
        }
        item { MenuCard(stringResource(R.string.my_progress), stringResource(R.string.my_progress_hint), R.drawable.ic_nav_stats, openProgress) }
        item { MenuCard(stringResource(R.string.achievements), stringResource(R.string.achievements_hint), R.drawable.ic_nav_habits, openAchievements) }
        item { MenuCard(stringResource(R.string.edit_profile), stringResource(R.string.edit_profile_hint), R.drawable.ic_nav_profile, editProfile) }
    }
}

@Composable
fun ProfilePlaceholderScreen(title: Int, body: Int) {
    ScreenList {
        item { SectionCard {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(body))
        } }
    }
}
