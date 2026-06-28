package com.habittracker.app.ui.screens.habits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.app.data.local.entity.isCurrentlyPaused
import com.habittracker.app.data.repository.HabitRepository
import com.habittracker.app.reminder.ReminderScheduler
import com.habittracker.app.ui.widget.WidgetUpdateHelper
import com.habittracker.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditHabitUiState(
    val name: String = "",
    val selectedEmoji: String = "✅",
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val endDateMillis: Long? = null,
    val endDateError: String? = null,
    val weeklyTarget: Int = 7,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isCurrentlyPaused: Boolean = false,
    val existingReminderTime: String? = null
)

val EmojiOptions = listOf(
    "✅", "📝", "🏃", "💪", "📖", "🧘", "🎯",
    "💧", "🥗", "😴", "☀️", "🎨", "🎵", "🧹",
    "💰", "📞", "✍️", "🧠", "🌱", "🔥", "⭐",
    "💡", "📊", "🏠", "🚀", "💚", "🎉", "🔄",
    "🏋️", "🚴", "🏊", "🧗", "🤸", "🧎", "⛰️",
    "🥦", "🥑", "🍎", "🥬", "🍵", "🩺", "💊",
    "📚", "🎓", "💼", "📋", "📈", "📅", "⏰",
    "🏆", "🥇", "🎖️", "💎", "🏁", "📉", "⏱️",
    "🔑", "🛡️", "⚙️", "🧰", "🗂️", "⌛", "📌"
)

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    private val repository: HabitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditHabitUiState())
    val uiState: StateFlow<AddEditHabitUiState> = _uiState.asStateFlow()

    private var editingHabitId: Long? = null

    fun loadHabit(habitId: Long) {
        editingHabitId = habitId
        viewModelScope.launch {
            repository.getHabitById(habitId).first()?.let { habit ->
                val (hour, minute) = habit.reminderTime?.split(":")?.let {
                    it[0].toInt() to it[1].toInt()
                } ?: (null to null)

                _uiState.value = AddEditHabitUiState(
                    name = habit.name,
                    selectedEmoji = habit.emoji,
                    reminderHour = hour,
                    reminderMinute = minute,
                    endDateMillis = habit.endDate,
                    weeklyTarget = habit.weeklyTarget.coerceIn(1, 7),
                    isEditMode = true,
                    isCurrentlyPaused = habit.isCurrentlyPaused,
                    existingReminderTime = habit.reminderTime
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onEmojiSelected(emoji: String) {
        _uiState.value = _uiState.value.copy(selectedEmoji = emoji)
    }

    fun onReminderSet(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun onReminderCleared() {
        _uiState.value = _uiState.value.copy(reminderHour = null, reminderMinute = null)
    }

    fun onEndDateSelected(millis: Long) {
        _uiState.value = _uiState.value.copy(
            endDateMillis = millis,
            endDateError = if (millis < DateUtils.todayMillis()) "End date must be today or later" else null
        )
    }

    fun onEndDateCleared() {
        _uiState.value = _uiState.value.copy(endDateMillis = null, endDateError = null)
    }

    fun onWeeklyTargetChanged(value: Int) {
        _uiState.value = _uiState.value.copy(weeklyTarget = value)
    }

    fun deleteHabit(onDeleted: () -> Unit) {
        val id = editingHabitId ?: return
        viewModelScope.launch {
            repository.getHabitById(id).first()?.let { habit ->
                repository.deleteHabit(habit)
                ReminderScheduler.cancel(context, id)
            }
            WidgetUpdateHelper.notifyDataChanged(context)
            onDeleted()
        }
    }

    fun pauseHabit(onDone: () -> Unit) {
        val id = editingHabitId ?: return
        viewModelScope.launch {
            repository.pauseHabit(id)
            ReminderScheduler.cancel(context, id)
            _uiState.value = _uiState.value.copy(isCurrentlyPaused = true)
            WidgetUpdateHelper.notifyDataChanged(context)
            onDone()
        }
    }

    fun resumeHabit(onDone: () -> Unit) {
        val id = editingHabitId ?: return
        viewModelScope.launch {
            repository.resumeHabit(id)
            val currentReminder = _uiState.value.reminderHour?.let { hour ->
                _uiState.value.reminderMinute?.let { minute ->
                    String.format("%02d:%02d", hour, minute)
                }
            }
            if (currentReminder != null) {
                ReminderScheduler.schedule(context, id, currentReminder)
            }
            _uiState.value = _uiState.value.copy(isCurrentlyPaused = false)
            WidgetUpdateHelper.notifyDataChanged(context)
            onDone()
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) return
        if (state.endDateError != null) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            val reminderTime = if (state.reminderHour != null && state.reminderMinute != null) {
                String.format("%02d:%02d", state.reminderHour, state.reminderMinute)
            } else null

            val weeklyTarget = state.weeklyTarget

            val habitId = editingHabitId
            if (habitId != null) {
                val habit = repository.getHabitById(habitId).first()
                if (habit != null) {
                    repository.updateHabit(
                        habit.copy(
                            name = state.name,
                            emoji = state.selectedEmoji,
                            reminderTime = reminderTime,
                            endDate = state.endDateMillis,
                            weeklyTarget = weeklyTarget
                        )
                    )
                }
            } else {
                val newId = repository.addHabit(
                    name = state.name,
                    emoji = state.selectedEmoji,
                    reminderTime = reminderTime,
                    endDate = state.endDateMillis,
                    weeklyTarget = weeklyTarget,
                    sortOrder = 0
                )
                editingHabitId = newId
            }

            // Schedule or update reminder
            val savedId = editingHabitId ?: return@launch
            if (reminderTime != null) {
                ReminderScheduler.schedule(context, savedId, reminderTime)
            } else if (habitId != null) {
                // Reminder was cleared → cancel old schedule
                ReminderScheduler.cancel(context, habitId)
            }

            WidgetUpdateHelper.notifyDataChanged(context)
            onSaved()
        }
    }
}
