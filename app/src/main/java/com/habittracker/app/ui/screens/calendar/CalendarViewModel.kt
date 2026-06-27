package com.habittracker.app.ui.screens.calendar

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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasCompletion: Boolean = false
)

data class DayDetail(
    val habitId: Long,
    val habitName: String,
    val emoji: String,
    val isCompleted: Boolean,
    /** true = △ back-filled (completedAt date > record date) */
    val isBackfill: Boolean = false,
    /** true = ★ same-day check-in (completedAt date == record date) */
    val isSameDay: Boolean = false
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedDayCompletions: List<DayDetail> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _refreshTrigger = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            combine(
                repository.allActiveHabits,
                _currentMonth,
                _refreshTrigger
            ) { habits, yearMonth, _ ->
                val startDate = DateUtils.startOfDay(yearMonth.atDay(1))
                val endDate = DateUtils.startOfNextDay(yearMonth.atEndOfMonth())

                val allRecords = repository.getRecordsInDateRange(startDate, endDate)
                val completedDates = allRecords.map { it.date }.toSet()

                val days = generateCalendarDays(yearMonth).map { day ->
                    if (day.isCurrentMonth) {
                        day.copy(hasCompletion = completedDates.contains(DateUtils.startOfDay(day.date)))
                    } else day
                }

                CalendarUiState(
                    currentMonth = yearMonth,
                    days = days,
                    habits = habits,  // all habits, no global filter
                    isLoading = false,
                    selectedDate = _uiState.value.selectedDate,
                    selectedDayCompletions = _uiState.value.selectedDayCompletions
                )
            }.collect { state ->
                _uiState.value = state
                state.selectedDate?.let { loadDayDetail(it) }
            }
        }
    }

    fun loadMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    fun onDayClicked(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDayDetail(date)
    }

    private fun loadDayDetail(date: LocalDate) {
        viewModelScope.launch {
            val dateMillis = DateUtils.startOfDay(date)

            // Only habits active on this date (endDate == null or endDate >= this date)
            val validHabits = _uiState.value.habits.filter { h ->
                h.endDate == null || h.endDate >= dateMillis
            }

            val records = repository.getRecordsByDate(dateMillis)
            val recordMap = records.associateBy { it.habitId }

            val details = validHabits.map { habit ->
                val record = recordMap[habit.id]
                val isCompleted = record != null
                var isBackfill = false
                var isSameDay = false
                if (isCompleted) {
                    val completedDate = DateUtils.toLocalDate(record!!.completedAt)
                    isBackfill = completedDate > date
                    isSameDay = completedDate == date
                }
                DayDetail(
                    habitId = habit.id,
                    habitName = habit.name,
                    emoji = habit.emoji,
                    isCompleted = isCompleted,
                    isBackfill = isBackfill,
                    isSameDay = isSameDay
                )
            }
            _uiState.value = _uiState.value.copy(selectedDayCompletions = details)
        }
    }

    fun toggleDayHabit(habitId: Long) {
        val date = _uiState.value.selectedDate ?: return
        if (date > LocalDate.now()) return // future dates cannot be toggled
        viewModelScope.launch {
            val dateMillis = DateUtils.startOfDay(date)
            repository.toggleRecord(habitId, dateMillis)
            _refreshTrigger.value = System.currentTimeMillis()
            loadDayDetail(date)
        }
    }

    fun previousMonth() {
        loadMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun nextMonth() {
        loadMonth(_uiState.value.currentMonth.plusMonths(1))
    }

    private fun generateCalendarDays(yearMonth: YearMonth): List<CalendarDay> {
        val today = LocalDate.now()
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val days = mutableListOf<CalendarDay>()

        val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 7) % 7 // Mon=0..Sun=6
        val prevMonth = yearMonth.minusMonths(1)
        val prevMonthLastDay = prevMonth.lengthOfMonth()

        for (i in (prevMonthLastDay - startDayOfWeek + 1)..prevMonthLastDay) {
            days.add(CalendarDay(prevMonth.atDay(i), false, false))
        }
        for (i in 1..lastDayOfMonth.dayOfMonth) {
            val date = yearMonth.atDay(i)
            days.add(CalendarDay(date, true, date == today))
        }
        val remaining = (7 - (days.size % 7)) % 7
        val nextMonth = yearMonth.plusMonths(1)
        for (i in 1..remaining) {
            days.add(CalendarDay(nextMonth.atDay(i), false, false))
        }
        return days
    }
}
