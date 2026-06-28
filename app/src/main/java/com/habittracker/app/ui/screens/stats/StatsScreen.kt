package com.habittracker.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.habittracker.app.ui.BackgroundManager
import com.habittracker.app.ui.BackgroundType
import com.habittracker.app.ui.backgroundPresets
import com.habittracker.app.ui.rememberUriPainter
import com.habittracker.app.ui.scrimAlpha
import androidx.compose.foundation.Image as ComposeImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onEditHabit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bg by BackgroundManager.settings.collectAsState()
    var revealedHabitId by remember { mutableStateOf<Long?>(null) }

    // ── Detail dialog ──
    uiState.selectedHabitDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSelection() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = detail.habit.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = detail.habit.name, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column {
                    DetailRow("Created", detail.createdDate)
                    detail.endDate?.let { DetailRow("End date", it) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("Duration", "${detail.totalDays} days")
                    DetailRow("Completed", "${detail.completedDays} days")
                    DetailRow("Missed", "${detail.missedDays} days")
                    DetailRow("Streak", "${detail.currentStreak} days (best ${detail.bestStreak})")
                    DetailRow("Rate", "${(detail.completionRate * 100).toInt()}%")
                    if (detail.weeklyTrend.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Weekly completions:", style = MaterialTheme.typography.labelLarge)
                        detail.weeklyTrend.forEach { (week, count) ->
                            DetailRow(week, "$count / 7")
                        }
                    }
                    if (detail.completionDates.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Recent completions:", style = MaterialTheme.typography.labelLarge)
                        detail.completionDates.forEach { date ->
                            Text("  $date", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSelection() }) { Text("Close") }
            }
        )
    }

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
            TopAppBar(
                title = { Text("Statistics") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )

            if (uiState.stats.isEmpty() && !uiState.isLoading) {
                Text(
                    text = "No habits to show",
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(revealedHabitId) {
                            if (revealedHabitId != null) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    revealedHabitId = null
                                }
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                items(uiState.stats, key = { it.habit.id }) { stat ->
                    val scope = rememberCoroutineScope()
                    val density = LocalDensity.current
                    val maxOffsetPx = density.density * 80f
                    val isRevealed = revealedHabitId == stat.habit.id
                    val offsetAnimatable = remember { Animatable(0f) }

                    // Animate back when revealedHabitId changes away from this habit
                    LaunchedEffect(revealedHabitId) {
                        if (!isRevealed && offsetAnimatable.value != 0f) {
                            offsetAnimatable.animateTo(0f, tween(250))
                        }
                    }

                    // Reset when user scrolls
                    LaunchedEffect(Unit) {
                        snapshotFlow { listState.isScrollInProgress }
                            .collect { scrolling ->
                                if (scrolling && revealedHabitId != null) {
                                    revealedHabitId = null
                                }
                            }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Layer 0 — dismiss overlay (intercepts taps on revealed card)
                        if (isRevealed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { revealedHabitId = null }
                            )
                        }

                        // Layer 1 — habit card (offset driven by Animatable)
                        HabitStatCard(
                            stat = stat,
                            onClick = {
                                if (isRevealed) {
                                    revealedHabitId = null
                                } else {
                                    if (revealedHabitId != null) revealedHabitId = null
                                    viewModel.selectHabit(stat.habit.id)
                                }
                            },
                            modifier = Modifier
                                .offset { IntOffset(offsetAnimatable.value.roundToInt(), 0) }
                                .pointerInput(isRevealed) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            scope.launch {
                                                if (isRevealed) {
                                                    revealedHabitId = null
                                                } else {
                                                    val reveal = offsetAnimatable.value < -maxOffsetPx * 0.3f
                                                    revealedHabitId = if (reveal) stat.habit.id else null
                                                    offsetAnimatable.animateTo(
                                                        if (reveal) -maxOffsetPx else 0f,
                                                        tween(250)
                                                    )
                                                }
                                            }
                                        },
                                        onHorizontalDrag = { _, dragAmount ->
                                            scope.launch {
                                                offsetAnimatable.snapTo(
                                                    (offsetAnimatable.value + dragAmount)
                                                        .coerceIn(-maxOffsetPx, 0f)
                                                )
                                            }
                                        }
                                    )
                                }
                        )

                        // Layer 2 — Edit icon (aligned to right end of outer Box)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isRevealed,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            enter = fadeIn(tween(200, delayMillis = 250)),
                            exit = fadeOut(tween(150))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(end = 12.dp)
                                    .clickable {
                                        scope.launch {
                                            onEditHabit(stat.habit.id)
                                            revealedHabitId = null
                                            offsetAnimatable.snapTo(0f)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            } // else
        } // Column
    } // Box
} // StatsScreen

@Composable
private fun HabitStatCard(
    stat: HabitStat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stat.habit.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.habit.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    if (stat.habit.isArchived) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "(Paused)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    "${stat.totalDays}d · ${stat.completedDays}d done · 🔥${stat.currentStreak}d (best ${stat.bestStreak})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${(stat.completionRate * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (stat.completionRate > 0.7f) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
