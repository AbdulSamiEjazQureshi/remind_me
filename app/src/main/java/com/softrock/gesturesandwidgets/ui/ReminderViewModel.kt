package com.softrock.gesturesandwidgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


enum class RecordingState { IDLE, RECORDING, RECORDED, ERROR }


data class ReminderViewModelState(
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val previousReminders: List<ReminderEntity> = emptyList(),
    val title: String = "",

    val timeInMillis: Long = 0L,
    val audioUri: String? = null,
    val error: String? = null,
    val isAddingTitle: Boolean = false,
    val isAddingReminder: Boolean = false,
    val isSelectedDate: Boolean = true,
    val recordingState: RecordingState = RecordingState.IDLE,
)

sealed interface ReminderVMEvent {
    object SaveReminder : ReminderVMEvent
    data class SetTitle(val title: String) : ReminderVMEvent
    data class SetTimeInMillis(val timeInMillis: Long) : ReminderVMEvent
    data class SetAudioUri(val audioUri: String) : ReminderVMEvent
    data class SetIsAddingTitle(val newState: Boolean) : ReminderVMEvent
    data class SetIsAddingReminder(val newState: Boolean) : ReminderVMEvent
    data class SetIsSelectedDate(val newState: Boolean) : ReminderVMEvent
    data class SetRecordingState(val recordingState: RecordingState) : ReminderVMEvent
    object ResetFormValues : ReminderVMEvent
}

class ReminderViewModel(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(ReminderViewModelState())

    val state = combine(
        _state,
        reminderRepository.getAllRemindersByStatus(true),
        reminderRepository.getAllRemindersByStatus(false)
    ) { state, previousReminders, upcomingReminders ->
        state.copy(previousReminders = previousReminders, upcomingReminders = upcomingReminders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderViewModelState())

    fun onEvent(event: ReminderVMEvent) {
        when (event) {
            ReminderVMEvent.SaveReminder -> {
                try {

                    if (_state.value.title.isEmpty() || _state.value.title.isBlank()) {
                        _state.update {
                            it.copy(error = "Please enter a title")
                        }
                        return
                    }

                    if (_state.value.timeInMillis == 0L) {
                        _state.update {
                            it.copy(error = "Please choose date and time")
                        }
                        return
                    }

                    if (_state.value.audioUri == null) {
                        _state.update {
                            it.copy(error = "Please record a audio note")
                        }
                        return
                    }

                    val entity = ReminderEntity(
                        title = _state.value.title,
                        timeInMillis = _state.value.timeInMillis,
                        audioUri = _state.value.audioUri!!
                    )

                    viewModelScope.launch {
                        val insertedId = reminderRepository.insert(entity)
                        val isReminderScheduled =
                            reminderScheduler.schedule(insertedId, entity.timeInMillis)

                        resetFormValues()

                        if (!isReminderScheduled) {
                            _state.update { it.copy(error = "Schedule Failed") }
                        }
                    }

                } catch (e: Exception) {
                    _state.update {
                        it.copy(error = e.message.toString())
                    }
                }
            }

            is ReminderVMEvent.SetAudioUri -> {
                _state.update {
                    it.copy(audioUri = event.audioUri)
                }

            }

            is ReminderVMEvent.SetTitle -> {
                _state.update {
                    it.copy(title = event.title)
                }
            }

            is ReminderVMEvent.ResetFormValues -> resetFormValues()

            is ReminderVMEvent.SetIsAddingReminder -> {
                _state.update {
                    it.copy(
                        isAddingReminder = event.newState
                    )
                }
            }

            is ReminderVMEvent.SetIsAddingTitle -> {
                _state.update {
                    it.copy(
                        isAddingTitle = event.newState
                    )
                }
            }

            is ReminderVMEvent.SetTimeInMillis -> {
                _state.update {
                    it.copy(
                        timeInMillis = event.timeInMillis
                    )
                }
            }

            is ReminderVMEvent.SetIsSelectedDate -> {
                _state.update {
                    it.copy(
                        isSelectedDate = event.newState
                    )
                }
            }

            is ReminderVMEvent.SetRecordingState -> {
                _state.update { it.copy(recordingState = event.recordingState) }
            }
        }
    }

    private fun resetFormValues() {
        _state.update {
            it.copy(
                title = "",
                timeInMillis = 0L,
                audioUri = null,
                isAddingTitle = false,
                isAddingReminder = false,
                error = null,
                isSelectedDate = true
            )
        }
    }
}
