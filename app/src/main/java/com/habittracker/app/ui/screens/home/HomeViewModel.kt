package com.habittracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.repository.HabitRepository
import com.habittracker.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val showArchived: Boolean = false,
    val archivedCount: Int = 0,

    val isLoading: Boolean = true
)



@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Incremented on every toggle to trigger weekly grid refresh
    private val _weeklyRefreshTrigger = MutableStateFlow(0L)
    private val _showArchived = MutableStateFlow(false)

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

        // Reactive chain: all habits + today records + archive toggle + trigger
        viewModelScope.launch {
            combine(
                repository.allHabits,
                repository.getTodayRecords(today),
                _showArchived,
                _weeklyRefreshTrigger
            ) { allHabits, todayRecords, showArchived, _ ->
                val habits = if (showArchived) allHabits.filter { it.isArchived }
                             else allHabits.filter { it.isActive && !it.isArchived }
                val archivedCount = allHabits.count { it.isArchived }
                val completedSet = todayRecords.map { it.habitId }.toSet()
                val weeklyMap = mutableMapOf<Long, Set<Long>>()
                habits.forEach { habit ->
                    val habitRecords = repository.getRecordsInRange(habit.id, weekStart, weekEnd).first()
                    weeklyMap[habit.id] = habitRecords.map { it.date }.toSet()
                }
                _uiState.value = _uiState.value.copy(
                    habits = habits,
                    showArchived = showArchived,
                    archivedCount = archivedCount,
                    completedToday = completedSet,
                    weeklyCompletions = weeklyMap,
                    isLoading = false
                )
            }.collect { }
        }
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            repository.toggleRecord(habitId, _uiState.value.todayDate)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun toggleHabitOnDate(habitId: Long, dateMillis: Long) {
        viewModelScope.launch {
            repository.toggleRecord(habitId, dateMillis)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun setTitle(title: String) {
        _uiState.value = _uiState.value.copy(titleText = title)
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun unarchiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.unarchiveHabit(habitId)
            _weeklyRefreshTrigger.value = System.currentTimeMillis()
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
