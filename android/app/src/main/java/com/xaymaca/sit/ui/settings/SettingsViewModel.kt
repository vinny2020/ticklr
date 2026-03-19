package com.xaymaca.sit.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.BuildConfig
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.service.SeedDataService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val seedDataService: SeedDataService
) : ViewModel() {

    private val prefs = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)

    private val _sendDirectly = MutableStateFlow(
        prefs.getBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, false)
    )
    val sendDirectly: StateFlow<Boolean> = _sendDirectly

    private val _seedMessage = MutableStateFlow<String?>(null)
    val seedMessage: StateFlow<String?> = _seedMessage

    val isDebug: Boolean = BuildConfig.DEBUG

    fun toggleSendDirectly() {
        val newValue = !_sendDirectly.value
        _sendDirectly.value = newValue
        prefs.edit().putBoolean(SITApp.KEY_SEND_SMS_DIRECTLY, newValue).apply()
    }

    fun loadTestContacts() {
        viewModelScope.launch {
            try {
                val count = seedDataService.seedTestContacts()
                _seedMessage.value = "Loaded $count test contacts"
            } catch (e: Exception) {
                _seedMessage.value = "Seed failed: ${e.message}"
            }
        }
    }

    fun clearSeedMessage() {
        _seedMessage.value = null
    }
}
