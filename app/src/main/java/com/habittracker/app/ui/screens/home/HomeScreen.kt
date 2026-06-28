package com.habittracker.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.habittracker.app.data.local.entity.HabitEntity
import com.habittracker.app.data.local.entity.isCurrentlyPaused
import kotlin.math.roundToInt
import com.habittracker.app.ui.BackgroundManager
import com.habittracker.app.ui.BackgroundType
import com.habittracker.app.ui.SchemeMode
import com.habittracker.app.ui.backgroundPresets
import com.habittracker.app.ui.components.HabitItem
import com.habittracker.app.ui.rememberUriPainter
import com.habittracker.app.ui.scrimAlpha
import androidx.compose.foundation.Image as ComposeImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddHabitClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bg by BackgroundManager.settings.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf("") }
    // Sync title from ViewModel (including persisted value) whenever dialog opens
    LaunchedEffect(showSettings) {
        if (showSettings) editTitleText = uiState.titleText
    }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingImageUri = it }
    }

    // ── Image preview dialog (before applying) ──
    pendingImageUri?.let { uri ->
        ImagePreviewDialog(
            uri = uri,
            onApply = { scale, offsetXDp, offsetYDp ->
                BackgroundManager.setImageUri(uri.toString(), scale, offsetXDp, offsetYDp)
                pendingImageUri = null
            },
            onCancel = { pendingImageUri = null }
        )
    }

    // ── Settings dialog ──
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editTitleText,
                        onValueChange = { editTitleText = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Background",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backgroundPresets.forEachIndexed { index, (colorInt, name) ->
                            val color = Color(colorInt)
                            val isSelected = bg.type == BackgroundType.COLOR && bg.colorIndex == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { BackgroundManager.setColorIndex(index) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (index == 0) MaterialTheme.colorScheme.surface
                                            else color,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.dp, MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (bg.type == BackgroundType.IMAGE) {
                        Text(
                            text = "Photo set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                        Text(if (bg.type == BackgroundType.IMAGE) "Change photo" else "Pick a photo")
                    }
                    if (bg.type != BackgroundType.SYSTEM) {
                        TextButton(onClick = { BackgroundManager.resetToSystem() }) {
                            Text("Reset to system default")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Color scheme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            SchemeMode.SYSTEM to "Auto",
                            SchemeMode.LIGHT to "Light",
                            SchemeMode.DARK to "Dark"
                        ).forEach { (mode, label) ->
                            val isActive = bg.schemeMode == mode
                            OutlinedButton(
                                onClick = { BackgroundManager.setSchemeMode(mode) },
                                modifier = Modifier.weight(1f),
                                colors = if (isActive) ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editTitleText.isNotBlank()) viewModel.setTitle(editTitleText)
                    showSettings = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("Cancel") }
            }
        )
    }

    // ── Main content ──
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

        // ── Scrim overlay for dark backgrounds ──
        val scrimAlpha = bg.scrimAlpha()
        if (scrimAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        }

        // ── Foreground ──
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = uiState.titleText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.habits.isNotEmpty()) {
                // Weekly grid capped at ~5 rows, scrollable for many habits
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        WeeklyGrid(
                            habits = uiState.habits,
                            weekDates = uiState.weekDates,
                            weekDayLabels = uiState.weekDayLabels,
                            weeklyCompletions = uiState.weeklyCompletions
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "Today's habits",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                if (uiState.habits.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No habits yet",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the + button",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                items(uiState.habits, key = { it.id }) { habit ->
                    HabitItem(
                        habit = habit,
                        isCompleted = uiState.completedToday.contains(habit.id),
                        isPaused = habit.isCurrentlyPaused,
                        onToggle = { viewModel.toggleHabit(habit.id) },
                        onClick = { },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }

        // Bottom-left: Settings
        FloatingActionButton(
            onClick = {
                editTitleText = uiState.titleText
                showSettings = true
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(40.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        // Bottom-right: Add habit
        FloatingActionButton(
            onClick = onAddHabitClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add habit",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Weekly Grid ──────────────────────────────────────────

@Composable
private fun WeeklyGrid(
    habits: List<HabitEntity>,
    weekDates: List<Long>,
    weekDayLabels: List<String>,
    weeklyCompletions: Map<Long, Set<Long>>
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(modifier = Modifier.width(32.dp))
            weekDayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        habits.forEach { habit ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = habit.emoji,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                weekDates.forEach { dateMillis ->
                    GridDot(checked = weeklyCompletions[habit.id]?.contains(dateMillis) ?: false)
                }
            }
        }
    }
}

// ── Image Preview Dialog ─────────────────────────────────

@Composable
private fun ImagePreviewDialog(
    uri: Uri,
    onApply: (scale: Float, offsetXDp: Float, offsetYDp: Float) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val painter = rememberUriPainter(uri.toString(), context)
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    // Screen aspect ratio for the preview box
    val screenAspect = config.screenWidthDp.toFloat() / config.screenHeightDp.toFloat()

    var scale by remember { mutableStateOf(1.0f) }
    var offsetXDp by remember { mutableStateOf(0f) }
    var offsetYDp by remember { mutableStateOf(0f) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Preview") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pinch to zoom · Drag to move",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Preview box with screen aspect ratio ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(screenAspect)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.3f, 3.0f)
                                // Convert pixel pan delta to Dp
                                offsetXDp += pan.x / density.density
                                offsetYDp += pan.y / density.density
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (painter != null) {
                        ComposeImage(
                            painter = painter,
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(scale)
                                .offset(x = offsetXDp.dp, y = offsetYDp.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.25f
                        )
                    } else {
                        Text("Unable to load image", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(scale, offsetXDp, offsetYDp) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Composable
private fun GridDot(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
