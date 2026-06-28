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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HabitStat(
    val habit: HabitEntity,
    val totalDays: Long,
    val completedDays: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate: Float
)

data class HabitDetail(
    val habit: HabitEntity,
    val totalDays: Long,
    val completedDays: Int,
    val missedDays: Long,
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate: Float,
    val createdDate: String,
    val endDate: String?,
    val completionDates: List<String>,
    val weeklyTrend: List<Pair<String, Int>>
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
            repository.allVisibleHabits.collect { habits ->
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

            // Build weekly trend: last 5 full weeks
            val today = LocalDate.now()
            val weekStart = today.with(DayOfWeek.MONDAY).minusWeeks(4)
            val weekEnd = today.with(DayOfWeek.SUNDAY)
            val completedSet = records.map { DateUtils.toLocalDate(it.date) }.toSet()
            val trend = mutableListOf<Pair<String, Int>>()
            var w = weekStart
            while (!w.isAfter(weekEnd)) {
                val label = "W${w.monthValue}/${w.year}"
                var count = 0
                val wEnd = w.plusDays(6)
                var d = w
                while (!d.isAfter(wEnd) && !d.isAfter(today)) {
                    if (completedSet.contains(d)) count++
                    d = d.plusDays(1)
                }
                trend.add(label to count)
                w = w.plusDays(7)
            }

            _uiState.value = _uiState.value.copy(
                selectedHabitDetail = HabitDetail(
                    habit = stat.habit,
                    totalDays = stat.totalDays,
                    completedDays = stat.completedDays,
                    missedDays = stat.totalDays - stat.completedDays,
                    currentStreak = stat.currentStreak,
                    bestStreak = stat.bestStreak,
                    completionRate = stat.completionRate,
                    createdDate = DateUtils.formatDate(stat.habit.createdAt),
                    endDate = stat.habit.endDate?.let { DateUtils.formatDate(it) },
                    completionDates = dates,
                    weeklyTrend = trend
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
        // Account for endDate: only count days the habit was actually active
        val effectiveEndDate = if (habit.endDate != null) {
            minOf(DateUtils.toLocalDate(habit.endDate), today)
        } else {
            today
        }
        val totalDays = ChronoUnit.DAYS.between(createdDate, effectiveEndDate) + 1

        val records = repository.getAllRecordsForHabit(habit.id)
        val completedDates = records
            .map { DateUtils.toLocalDate(it.date) }
            .distinct()
            .sorted()
        val completedDays = completedDates.size

        val currentStreak = calculateStreak(completedDates, today)
        val bestStreak = calculateBestStreak(completedDates)

        // Completion rate based on weekly target.
        // If weeklyTarget == 0, treat as 7 (every day).
        val effectiveWeeklyTarget = if (habit.weeklyTarget > 0) habit.weeklyTarget else 7
        val expectedCompletions = (totalDays / 7.0) * effectiveWeeklyTarget
        val completionRate = if (expectedCompletions > 0) {
            (completedDays.toFloat() / expectedCompletions.toFloat()).coerceAtMost(1f)
        } else 0f

        return HabitStat(
            habit = habit,
            totalDays = totalDays,
            completedDays = completedDays,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
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

    private fun calculateBestStreak(completedDates: List<LocalDate>): Int {
        if (completedDates.isEmpty()) return 0
        val set = completedDates.toSet()
        var best = 1
        var currentStreak = 1
        var prev = completedDates.first()
        for (date in completedDates.drop(1)) {
            if (ChronoUnit.DAYS.between(prev, date) == 1L) {
                currentStreak++
                best = maxOf(best, currentStreak)
            } else {
                currentStreak = 1
            }
            prev = date
        }
        return best
    }
}
