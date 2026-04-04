package com.xaymaca.sit.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repo: MessageTemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<MessageTemplate>> = repo
        .getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTemplate(title: String, body: String) {
        viewModelScope.launch {
            repo.insertTemplate(MessageTemplate(title = title.trim(), body = body.trim()))
        }
    }

    fun updateTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            repo.updateTemplate(template)
        }
    }

    fun deleteTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            repo.deleteTemplate(template)
        }
    }

    fun seedDefaultIfNeeded(prefs: SharedPreferences) {
        val key = "hasSeededDefaultTemplates"
        if (!prefs.getBoolean(key, false)) {
            saveTemplate(
                title = "Checking in",
                body = "Hey! Just checking in — hope you're doing well. Let's catch up soon!"
            )
            prefs.edit().putBoolean(key, true).apply()
        }
    }
}
