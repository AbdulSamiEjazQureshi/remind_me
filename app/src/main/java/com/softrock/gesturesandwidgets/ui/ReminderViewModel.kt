package com.softrock.gesturesandwidgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderViewModelState(
    val reminders: List<ReminderEntity> = emptyList()
)

class ReminderViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReminderViewModelState())

    val state = combine(_state, reminderRepository.allReminders) { state, reminders ->
        state.copy(reminders = reminders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderViewModelState())

    fun insert(entity: ReminderEntity) = viewModelScope.launch {
        reminderRepository.insert(entity)
    }
}
