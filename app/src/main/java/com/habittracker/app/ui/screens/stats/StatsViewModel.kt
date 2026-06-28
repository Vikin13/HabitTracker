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
import kotlinx.coroutines.flow.combine
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
    val completionRate: Float,
    val currentWeekCount: Int = 0
)

data class HabitDetail(
    val habit: HabitEntity,
    val totalDays: Long,
    val completedDays: Int,
    val expectedTotal: Int,
    val currentWeekCount: Int,
    val currentStreak: Int,
    val bestStreak: Int,
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
            combine(repository.allVisibleHabits, repository.anyRecordChange) { habits, _ -> habits }
                .collect { habits ->
                    val stats = habits.map { calculateStat(it) }
                    _uiState.value = StatsUiState(stats = stats, isLoading = false)
                }
        }
    }

    fun selectHabit(habitId: Long) {
        viewModelScope.launch {
            val stat = _uiState.value.stats.find { it.habit.id == habitId } ?: return@launch
            val records = repository.getAllRecordsForHabit(habitId)
            val completedSet = records.map { DateUtils.toLocalDate(it.date) }.toSet()

            // Weekly trend: from creation week up to (but not including) current week
            val today = LocalDate.now()
            val createdDate = DateUtils.toLocalDate(stat.habit.createdAt)
            val currentWeekStart = today.with(DayOfWeek.MONDAY)
            val trend = mutableListOf<Pair<String, Int>>()

            // Start from the Monday of the creation week
            val firstWeekStart = createdDate.with(DayOfWeek.MONDAY)
            // Limit to most recent 10 weeks
            val earliestWeek = currentWeekStart.minusWeeks(10)
            var w = maxOf(firstWeekStart, earliestWeek)

            while (w.isBefore(currentWeekStart)) {
                val wEnd = w.plusDays(6)
                var count = 0
                var d = w
                while (!d.isAfter(wEnd)) {
                    if (completedSet.contains(d)) count++
                    d = d.plusDays(1)
                }
                // Label: "Jun 22 - 28" or "May 25 - 31"
                val startLabel = "${w.monthValue}/${w.dayOfMonth}"
                val endLabel = if (w.monthValue == wEnd.monthValue)
                    "${wEnd.dayOfMonth}" else "${wEnd.monthValue}/${wEnd.dayOfMonth}"
                trend.add("$startLabel - $endLabel" to count)
                w = w.plusDays(7)
            }

            // Historical expected total: sum of weekly targets for non-paused weeks
            val weeklyTarget = stat.habit.weeklyTarget.coerceAtLeast(1)
            val pauseStart = if (stat.habit.pausedAt != null) DateUtils.toLocalDate(stat.habit.pausedAt) else null
            val pauseEnd = if (stat.habit.resumedAt != null) DateUtils.toLocalDate(stat.habit.resumedAt)
                else if (pauseStart != null) today.plusDays(1) else null
            var expectedTotal = 0
            var ew = firstWeekStart
            while (ew.isBefore(currentWeekStart)) {
                val weekSun = ew.plusDays(6)
                // Only skip weeks entirely within a pause period
                val isPaused = pauseStart != null && pauseEnd != null &&
                    !ew.isBefore(pauseStart) && weekSun.isBefore(pauseEnd)
                if (!isPaused) expectedTotal += weeklyTarget
                ew = ew.plusDays(7)
            }

            // Current week completions (Mon → today)
            val currentWeekCount = records
                .map { DateUtils.toLocalDate(it.date) }
                .count { it >= currentWeekStart && it <= today }

            _uiState.value = _uiState.value.copy(
                selectedHabitDetail = HabitDetail(
                    habit = stat.habit,
                    totalDays = stat.totalDays,
                    completedDays = stat.completedDays,
                    expectedTotal = expectedTotal,
                    currentWeekCount = currentWeekCount,
                    currentStreak = stat.currentStreak,
                    bestStreak = stat.bestStreak,
                    createdDate = DateUtils.formatDate(stat.habit.createdAt),
                    endDate = stat.habit.endDate?.let { DateUtils.formatDate(it) },
                    completionDates = emptyList(),
                    weeklyTrend = trend.reversed()
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
        var totalDays = ChronoUnit.DAYS.between(createdDate, effectiveEndDate) + 1

        // Subtract pause days (if any pause falls within the active range)
        if (habit.pausedAt != null) {
            val pauseStart = DateUtils.toLocalDate(habit.pausedAt)
            val pauseEnd = if (habit.resumedAt != null) DateUtils.toLocalDate(habit.resumedAt) else today.plusDays(1)
            // Only count overlap between [createdDate, effectiveEndDate] and [pauseStart, pauseEnd)
            val overlapStart = maxOf(pauseStart, createdDate)
            val overlapEnd = minOf(pauseEnd, effectiveEndDate.plusDays(1))
            if (overlapStart.isBefore(overlapEnd)) {
                totalDays -= ChronoUnit.DAYS.between(overlapStart, overlapEnd)
            }
        }

        val records = repository.getAllRecordsForHabit(habit.id)
        val completedDates = records
            .map { DateUtils.toLocalDate(it.date) }
            .filter { !it.isAfter(effectiveEndDate) }
            .distinct()
            .sorted()
        val completedDays = completedDates.size

        val currentStreak = calculateStreak(completedDates, today)
        val bestStreak = calculateBestStreak(completedDates)

        // Simple daily completion rate: completed days / total active days
        val completionRate = if (totalDays > 0) {
            (completedDays.toFloat() / totalDays.toFloat()).coerceAtMost(1f)
        } else 0f

        // Current week completions (Mon → today)
        val currentWeekStart = today.with(DayOfWeek.MONDAY)
        val currentWeekCount = completedDates.count { it >= currentWeekStart }

        return HabitStat(
            habit = habit,
            totalDays = totalDays,
            completedDays = completedDays,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionRate = completionRate,
            currentWeekCount = currentWeekCount
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
