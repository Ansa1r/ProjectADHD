package com.ansa1r.projectadhd.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.ui.apps.InstalledAppIcon
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun CreateHabitScreen(vm: CreateHabitViewModel, saved: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) saved() }
    Box(Modifier.imePadding()) { ScreenList {
        item { MessageBanner(vm) }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { SectionCard {
            OutlinedTextField(state.title, vm::title, label = { Text("Название привычки") }, singleLine = true,
                enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("habit_title"), colors = brandFieldColors())
            Text("Время в день · HH:MM")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(state.hours, vm::hours, label = { Text("HH") }, singleLine = true, enabled = vm.id == null && !state.saving,
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = brandFieldColors())
                Text(" : ")
                OutlinedTextField(state.minutes, vm::minutes, label = { Text("MM") }, singleLine = true, enabled = vm.id == null && !state.saving,
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = brandFieldColors())
            }
            Text("От 00:01 до 23:59. Время накапливается в течение дня.")
            BrandButton({ picking = !picking }, Modifier.fillMaxWidth(), enabled = vm.id == null && !state.saving) {
                Text(state.linked?.displayName ?: "Без приложения")
            }
            if (vm.id != null) Text("Можно изменить название. Для другой длительности или приложения создайте новую привычку — текущий прогресс сохранится.")
        } }
        if (picking && vm.id == null) {
            item { BrandOutlinedButton({ vm.link(null); picking = false }, Modifier.fillMaxWidth()) { Text("Без приложения") } }
            item { OutlinedTextField(state.query, vm::search, label = { Text("Поиск приложений") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = brandFieldColors()) }
            items(state.installed.filter { it.displayName.contains(state.query, true) }, key = { it.packageName }) { app ->
                val limited = app.packageName in state.limited
                SectionCard {
                    Row(Modifier.fillMaxWidth().clickable(enabled = !limited && !state.saving) { vm.link(app); picking = false },
                        verticalAlignment = Alignment.CenterVertically) {
                        InstalledAppIcon(app.packageName, true, 0, vm.icons)
                        Column(Modifier.weight(1f)) { Text(app.displayName); if (limited) Text("Ограничено") }
                        RadioButton(selected = state.linked?.packageName == app.packageName, enabled = !limited,
                            onClick = if (limited) null else { { vm.link(app); picking = false } })
                    }
                    if (limited) Text("Уберите приложение из ограничений, чтобы использовать его в привычке.")
                }
            }
        }
        item { BrandButton(vm::save, Modifier.fillMaxWidth().testTag("save_habit"), enabled = state.valid && !state.loading && !state.saving) {
            Text(if (state.saving) "Сохранение…" else "Сохранить")
        } }
    } }
}
