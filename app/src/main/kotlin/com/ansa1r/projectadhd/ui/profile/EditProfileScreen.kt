package com.ansa1r.projectadhd.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel, saved: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) saved() }
    Box(Modifier.imePadding()) {
        ScreenList {
            item { MessageBanner(viewModel) }
            item { SectionCard {
                OutlinedTextField(state.input, viewModel::change,
                    label = { Text(stringResource(R.string.nickname)) },
                    placeholder = { Text(stringResource(R.string.nickname_default)) }, singleLine = true,
                    supportingText = { Text(stringResource(R.string.nickname_hint)) },
                    enabled = !state.loading && !state.saving, isError = state.input.isNotEmpty() && !state.valid,
                    modifier = Modifier.fillMaxWidth().testTag("nickname_input"))
                BrandButton(viewModel::save, enabled = state.valid && !state.loading && !state.saving && !state.saved,
                    modifier = Modifier.fillMaxWidth().testTag("save_nickname")) {
                    Text(stringResource(if (state.saving) R.string.saving else R.string.save))
                }
            } }
        }
    }
}
