package com.ansa1r.projectadhd.ui.profile

import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
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
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.changeAvatar(uri)
    }
    LaunchedEffect(state.saved) { if (state.saved) saved() }
    Box(Modifier.imePadding()) {
        ScreenList {
            item { MessageBanner(viewModel) }
            item { SectionCard {
                UserAvatar(state.avatar, Modifier.size(112.dp))
                BrandButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !state.importingAvatar, modifier = Modifier.fillMaxWidth().testTag("change_avatar")) {
                    Text(if (state.importingAvatar) "Сохранение…" else "Изменить аватар")
                }
                Text("Изображение сохраняется только на этом устройстве.")
            } }
            item { SectionCard {
                OutlinedTextField(state.input, viewModel::change,
                    label = { Text(stringResource(R.string.nickname)) },
                    placeholder = { Text(stringResource(R.string.nickname_default)) }, singleLine = true,
                    supportingText = { Text(stringResource(R.string.nickname_hint)) },
                    enabled = !state.loading && !state.saving, isError = state.input.isNotEmpty() && !state.valid,
                    modifier = Modifier.fillMaxWidth().testTag("nickname_input"), colors = brandFieldColors())
                BrandButton(viewModel::save, enabled = state.valid && !state.loading && !state.saving && !state.saved,
                    modifier = Modifier.fillMaxWidth().testTag("save_nickname")) {
                    Text(stringResource(if (state.saving) R.string.saving else R.string.save))
                }
            } }
        }
    }
}
