package com.habittracker.app.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.local.entity.isActiveOn
import com.habittracker.app.data.repository.HabitRepository
import com.habittracker.app.reminder.ReminderScheduler
import com.habittracker.app.ui.widget.WidgetUpdateHelper
import com.habittracker.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val habits: List<HabitEntity> = emptyList(),
    val completedToday: Set<Long> = emptySet(),
    val weekDates: List<Long> = emptyList(),
    val weekDayLabels: List<String> = emptyList(),
    val weeklyCompletions: Map<Long, Set<Long>> = emptyMap(),
    val todayDate: Long = DateUtils.todayMillis(),
    val titleText: String = "Today",

    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HabitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState(
        titleText = prefs.getString("title", "Today") ?: "Today"
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Incremented on every toggle to trigger weekly grid refresh
    private val _weeklyRefreshTrigger = MutableStateFlow(0L)

    private val weekDates: List<LocalDate>
    private val weekStart: Long
    private val weekEnd: Long

    init {
        val today = DateUtils.todayMillis()
        weekDates = getCurrentWeekDates()
        weekStart = DateUtils.startOfDay(weekDates.first())
        weekEnd = DateUtils.startOfDay(weekDates.last())

        val weekMillis = weekDates.map { DateUtils.startOfDay(it) }
        val labels = weekDates.map { it.dayOfWeek.name.take(3) }
        _uiState.value = _uiState.value.copy(
            weekDates = weekMillis,
            weekDayLabels = labels,
            todayDate = today
        )

        // Reactive chain: active habits (active first, paused last) + today records + trigger
        viewModelScope.launch {
            combine(
                repository.allActiveHabits,
                repository.getTodayRecords(today),
                _weeklyRefreshTrigger
            ) { habits, todayRecords, _ ->
                val todayDate = LocalDate.now()
                val activeHabits = habits.filter { it.isActiveOn(todayDate) }
                val completedSet = todayRecords.map { it.habitId }.toSet()
                val weeklyMap = mutableMapOf<Long, Set<Long>>()
                activeHabits.forEach { habit ->
                    val habitRecords = repository.getRecordsInRange(habit.id, weekStart, weekEnd).first()
                    weeklyMap[habit.id] = habitRecords.map { it.date }.toSet()
                }
                _uiState.value = _uiState.value.copy(
                    habits = activeHabits,
                    completedToday = completedSet,
                    weeklyCompletions = weeklyMap,
                    isLoading = false
                )
            }.collect { }
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            repository.toggleRecord(habitId, _uiState.value.todayDate)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
            WidgetUpdateHelper.notifyDataChanged(context)
        }
    }

    fun toggleHabitOnDate(habitId: Long, dateMillis: Long) {
        viewModelScope.launch {
            repository.toggleRecord(habitId, dateMillis)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
            WidgetUpdateHelper.notifyDataChanged(context)
        }
    }

    fun setTitle(title: String) {
        _uiState.value = _uiState.value.copy(titleText = title)
        prefs.edit().putString("title", title).apply()
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            ReminderScheduler.cancel(context, habit.id)
            WidgetUpdateHelper.notifyDataChanged(context)
        }
    }

    fun pauseHabit(habitId: Long) {
        viewModelScope.launch {
            repository.pauseHabit(habitId)
            ReminderScheduler.cancel(context, habitId)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
            WidgetUpdateHelper.notifyDataChanged(context)
        }
    }

    fun resumeHabit(habitId: Long) {
        viewModelScope.launch {
            repository.resumeHabit(habitId)
            // Re-fetch the habit to get its reminder time
            repository.getHabitById(habitId).first()?.let { habit ->
                if (habit.reminderTime != null) {
                    ReminderScheduler.schedule(context, habitId, habit.reminderTime)
                }
            }
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
            WidgetUpdateHelper.notifyDataChanged(context)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
            WidgetUpdateHelper.notifyDataChanged(context)
        }
    }

    companion object {
        fun getCurrentWeekDates(): List<LocalDate> {
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek.value
            val monday = today.minusDays((dayOfWeek - 1).toLong())
            return (0..6).map { monday.plusDays(it.toLong()) }
        }
    }
}
