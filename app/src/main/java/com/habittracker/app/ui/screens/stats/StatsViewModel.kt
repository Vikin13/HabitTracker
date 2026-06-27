package com.habittracker.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.repository.HabitRepository
import com.habittracker.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HabitStat(
    val habit: HabitEntity,
    val totalDays: Long,
    val completedDays: Int,
    val currentStreak: Int,
    val completionRate: Float
)

data class HabitDetail(
    val habit: HabitEntity,
    val totalDays: Long,
    val completedDays: Int,
    val currentStreak: Int,
    val completionRate: Float,
    val createdDate: String,
    val endDate: String?,
    val completionDates: List<String>
)

data class StatsUiState(
    val stats: List<HabitStat> = emptyList(),
    val selectedHabitDetail: HabitDetail? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.allHabits.collect { habits ->
                val stats = habits.map { calculateStat(it) }
                _uiState.value = StatsUiState(stats = stats, isLoading = false)
            }
        }
    }

    fun selectHabit(habitId: Long) {
        viewModelScope.launch {
            val stat = _uiState.value.stats.find { it.habit.id == habitId } ?: return@launch
            val records = repository.getAllRecordsForHabit(habitId)
            val dates = records
                .map { DateUtils.toLocalDate(it.date) }
                .distinct()
                .sortedDescending()
                .take(10)
                .map { it.toString() }

            _uiState.value = _uiState.value.copy(
                selectedHabitDetail = HabitDetail(
                    habit = stat.habit,
                    totalDays = stat.totalDays,
                    completedDays = stat.completedDays,
                    currentStreak = stat.currentStreak,
                    completionRate = stat.completionRate,
                    createdDate = DateUtils.formatDate(stat.habit.createdAt),
                    endDate = stat.habit.endDate?.let { DateUtils.formatDate(it) },
                    completionDates = dates
                )
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedHabitDetail = null)
    }

    private suspend fun calculateStat(habit: HabitEntity): HabitStat {
        val createdDate = DateUtils.toLocalDate(habit.createdAt)
        val today = LocalDate.now()
        val totalDays = ChronoUnit.DAYS.between(createdDate, today) + 1

        val records = repository.getAllRecordsForHabit(habit.id)
        val completedDates = records
            .map { DateUtils.toLocalDate(it.date) }
            .distinct()
            .sorted()
        val completedDays = completedDates.size

        val streak = calculateStreak(completedDates, today)

        // Completion rate based on weekly target.
        // If weeklyTarget == 0, treat as 7 (every day).
        val effectiveWeeklyTarget = if (habit.weeklyTarget > 0) habit.weeklyTarget else 7
        // Expected completions since creation
        val expectedCompletions = (totalDays / 7.0) * effectiveWeeklyTarget
        val completionRate = if (expectedCompletions > 0) {
            (completedDays.toFloat() / expectedCompletions.toFloat()).coerceAtMost(1f)
        } else 0f

        return HabitStat(
            habit = habit,
            totalDays = totalDays,
            completedDays = completedDays,
            currentStreak = streak,
            completionRate = completionRate
        )
    }

    private fun calculateStreak(completedDates: List<LocalDate>, today: LocalDate): Int {
        val set = completedDates.toSet()
        var streak = 0
        var current = today
        while (set.contains(current)) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }
}
