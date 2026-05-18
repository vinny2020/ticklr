package com.xaymaca.sit.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.xaymaca.sit.BuildConfig
import com.xaymaca.sit.SITApp
import com.xaymaca.sit.data.repository.ContactRepository
import com.xaymaca.sit.data.repository.TickleRepository
import com.xaymaca.sit.service.ContactPhotoService
import com.xaymaca.sit.service.SeedDataService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val seedDataService: SeedDataService,
    private val contactRepository: ContactRepository,
    private val tickleRepository: TickleRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences(SITApp.PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        prefs.getInt(SITApp.KEY_THEME_MODE, 0)
    )
    val themeMode: StateFlow<Int> = _themeMode

    private val _seedMessage = MutableStateFlow<String?>(null)
    val seedMessage: StateFlow<String?> = _seedMessage

    val isDebug: Boolean = BuildConfig.DEBUG

    fun setThemeMode(mode: Int) {
        _themeMode.value = mode
        prefs.edit().putInt(SITApp.KEY_THEME_MODE, mode).apply()
    }

    fun loadTestContacts() {
        viewModelScope.launch {
            try {
                val result = seedDataService.seedTestContacts()
                _seedMessage.value = when {
                    result.skipped == 0 -> "Loaded ${result.inserted} test contacts ✓"
                    result.inserted == 0 -> "All ${result.skipped} contacts already exist"
                    else -> "Loaded ${result.inserted} new, skipped ${result.skipped} duplicates"
                }
            } catch (e: Exception) {
                _seedMessage.value = "Seed failed: ${e.message}"
            }
        }
    }

    fun clearAllContacts() {
        viewModelScope.launch {
            try {
                contactRepository.deleteAllContacts()
                contactRepository.deleteAllGroups()
                tickleRepository.deleteAllReminders()
                WorkManager.getInstance(context).cancelAllWork()

                // Bulk equivalent of LocalPhotoStore.delete() — single-contact
                // deletion already wipes the per-row .jpg in ContactDetailScreen.
                withContext(Dispatchers.IO) {
                    File(context.filesDir, "photos").deleteRecursively()
                }
                // Drop the system-photo bitmap cache so a reused contact ID
                // can't surface a stale image.
                ContactPhotoService.clearCache()

                _seedMessage.value = "All data cleared ✓"
            } catch (e: Exception) {
                _seedMessage.value = "Clear failed: ${e.message}"
            }
        }
    }

    fun clearSeedMessage() {
        _seedMessage.value = null
    }
}
