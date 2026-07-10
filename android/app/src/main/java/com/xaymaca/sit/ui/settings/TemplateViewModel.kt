package com.xaymaca.sit.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaymaca.sit.R
import com.xaymaca.sit.data.model.MessageTemplate
import com.xaymaca.sit.data.repository.MessageTemplateRepository
import com.xaymaca.sit.data.repository.MessageTemplateSeed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val repo: MessageTemplateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val templates: StateFlow<List<MessageTemplate>> = repo
        .getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // NonCancellable on save/update: TemplateEditScreen calls these then
    // onBack() → popBackStack() in the same tick, and this ViewModel is scoped
    // to that entry — the pop cancels viewModelScope mid-Room-write without
    // the guard. Same TIC-84 invariant as TickleViewModel.upsert; see the full
    // reasoning there. deleteTemplate/seedDefaultIfNeeded are only invoked
    // from TemplateListScreen (which stays resident), so they don't need it.

    fun saveTemplate(title: String, body: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repo.insertTemplate(MessageTemplate(title = title.trim(), body = body.trim()))
            }
        }
    }

    fun updateTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repo.updateTemplate(template)
            }
        }
    }

    fun deleteTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            repo.deleteTemplate(template)
        }
    }

    fun seedDefaultIfNeeded(prefs: SharedPreferences) {
        viewModelScope.launch {
            MessageTemplateSeed.seedDefaultIfNeeded(
                repo,
                prefs,
                context.getString(R.string.template_default_title),
                context.getString(R.string.template_default_body)
            )
        }
    }
}
