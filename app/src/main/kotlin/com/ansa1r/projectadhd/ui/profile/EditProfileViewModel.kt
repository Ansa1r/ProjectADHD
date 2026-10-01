package com.ansa1r.projectadhd.ui.profile

import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.profile.Nickname
import com.ansa1r.projectadhd.ui.components.AppViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EditProfileState(val avatar: String? = null, val importingAvatar: Boolean = false, val input: String = "", val loading: Boolean = true, val saving: Boolean = false, val saved: Boolean = false) {
    val valid: Boolean get() = Nickname.valid(input)
}
class EditProfileViewModel(private val container: AppContainer) : AppViewModel() {
    private val mutable = MutableStateFlow(EditProfileState())
    val state = mutable.asStateFlow()
    private var edited = false
    init {
        execute { container.avatars.file.collect { path -> mutable.update { it.copy(avatar = path) } } }
        execute {
        container.preferences.nickname.collect { nickname ->
            mutable.update { it.copy(input = if (edited) it.input else nickname.orEmpty(), loading = false) }
        }
    } }
    fun changeAvatar(uri: android.net.Uri) {
        if (mutable.value.importingAvatar) return
        mutable.update { it.copy(importingAvatar = true) }
        container.applicationScope.launch {
            try { container.avatars.saveFromPicker(uri) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { inform(R.string.avatar_error) }
            finally { mutable.update { it.copy(importingAvatar = false) } }
        }
    }
    fun change(value: String) {
        if (!mutable.value.loading && !mutable.value.saving) {
            edited = true
            mutable.update { it.copy(input = value, saved = false) }
        }
    }
    fun save() {
        val snapshot = mutable.value
        if (snapshot.loading || snapshot.saving || snapshot.saved || !snapshot.valid) return
        mutable.update { it.copy(saving = true) }
        container.applicationScope.launch {
            try {
                container.preferences.setNickname(snapshot.input)
                mutable.update { it.copy(input = Nickname.normalize(snapshot.input), saving = false, saved = true) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { mutable.update { it.copy(saving = false) }; inform(R.string.nickname_save_error) }
        }
    }
}
