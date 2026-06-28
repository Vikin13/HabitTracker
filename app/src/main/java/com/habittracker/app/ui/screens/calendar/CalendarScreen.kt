package com.habittracker.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale
import com.habittracker.app.ui.BackgroundManager
import com.habittracker.app.ui.BackgroundType
import com.habittracker.app.ui.backgroundPresets
import com.habittracker.app.ui.rememberUriPainter
import com.habittracker.app.ui.scrimAlpha
import androidx.compose.foundation.Image as ComposeImage

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val bg by BackgroundManager.settings.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        if (bg.type == BackgroundType.IMAGE) {
            val context = LocalContext.current
            val painter = rememberUriPainter(bg.imageUri ?: "", context)
            if (painter != null) {
                ComposeImage(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(bg.imageScale)
                        .offset(x = bg.imageOffsetXDp.dp, y = bg.imageOffsetYDp.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.25f
                )
            }
        }
        // Background color
        if (bg.type == BackgroundType.COLOR && bg.colorIndex > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(backgroundPresets[bg.colorIndex].first))
            )
        }

        // Scrim overlay for dark backgrounds
        val scrimAlpha = bg.scrimAlpha()
        if (scrimAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Compact title bar
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            // Calendar section with swipe gesture for month switching
            var dragTotal by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragTotal += dragAmount
                            },
                            onDragEnd = {
                                if (dragTotal < -120f) viewModel.nextMonth()
                                else if (dragTotal > 120f) viewModel.previousMonth()
                            },
                            onDragCancel = { }
                        )
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Month navigation (compact)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.previousMonth() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous")
                        }
                        Text(
                            text = "${uiState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${uiState.currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next")
                        }
                    }

                    // Day-of-week headers
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Calendar grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        userScrollEnabled = false
                    ) {
                        items(uiState.days) { day ->
                            CalendarDayCell(
                                day = day,
                                isSelected = day.date == uiState.selectedDate,
                                onClick = { viewModel.onDayClicked(day.date) }
                            )
                        }
                    }
                }
            }

            // Selected day detail
            if (uiState.selectedDate != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                SelectedDayDetail(
                    date = uiState.selectedDate!!,
                    completions = uiState.selectedDayCompletions,
                    onToggle = { viewModel.toggleDayHabit(it) }
                )
            }
        } // Column
    } // Box
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isFuture = day.date > LocalDate.now()
    val bgColor = when {
        isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primary
        day.hasCompletion -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        isSelected -> MaterialTheme.colorScheme.onPrimary
        day.isToday -> MaterialTheme.colorScheme.onPrimary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor, CircleShape)
            .then(if (isFuture) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SelectedDayDetail(
    date: LocalDate,
    completions: List<DayDetail>,
    onToggle: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM dd")),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (completions.isEmpty()) {
            Text(
                text = "No habits yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            completions.forEach { detail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = detail.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = detail.habitName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    val (checkLabel, checkTint) = when {
                        detail.isBackfill -> "\u25B3" to Color.White          // △
                        detail.isSameDay  -> "\u2605" to Color.White          // ★
                        detail.isCompleted -> "\u2713" to Color.White         // ✓ fallback
                        else -> "" to Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (detail.isCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onToggle(detail.habitId) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (detail.isCompleted) {
                            Text(
                                text = checkLabel,
                                color = checkTint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
