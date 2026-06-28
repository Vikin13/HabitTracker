package com.habittracker.app.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    viewModel: AddEditHabitViewModel,
    habitId: Long?,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(habitId) {
        if (habitId != null) viewModel.loadHabit(habitId)
    }

    // TimePicker dialog
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = uiState.reminderHour ?: 8,
            initialMinute = uiState.reminderMinute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderSet(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (uiState.reminderHour != null) {
                        TextButton(onClick = {
                            viewModel.onReminderCleared()
                            showTimePicker = false
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            }
        )
    }

    // DatePicker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.endDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onEndDateSelected(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (uiState.endDateMillis != null) {
                        TextButton(onClick = {
                            viewModel.onEndDateCleared()
                            showDatePicker = false
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete habit") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteHabit(onSaved)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Habit" else "New Habit") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Habit name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Morning jog") }
            )

            // Emoji picker — scrollable grid, ~4 rows visible, no background
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
            ) {
                itemsIndexed(EmojiOptions) { _, emoji ->
                    val isSelected = emoji == uiState.selectedEmoji
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable { viewModel.onEmojiSelected(emoji) }
                            .then(
                                if (isSelected) Modifier.border(
                                    1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)
                                ) else Modifier
                            )
                            .padding(4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Reminder — label + value side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminder",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                val reminderDisplay = if (uiState.reminderHour != null && uiState.reminderMinute != null) {
                    String.format("%02d:%02d", uiState.reminderHour, uiState.reminderMinute)
                } else {
                    "Not set"
                }
                Text(
                    text = reminderDisplay,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { showTimePicker = true },
                    color = if (uiState.reminderHour != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // End date — label + value side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "End date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                val dateDisplay = if (uiState.endDateMillis != null) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(uiState.endDateMillis!!))
                } else {
                    "Not set"
                }
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { showDatePicker = true },
                    color = if (uiState.endDateMillis != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            uiState.endDateError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Weekly target — label + value side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly goal (times)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = if (uiState.weeklyTarget > 0) "${uiState.weeklyTarget} times / week" else "Not set",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { showTargetPicker = true },
                    color = if (uiState.weeklyTarget > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Number picker dialog
            if (showTargetPicker) {
                NumberPickerDialog(
                    currentValue = uiState.weeklyTarget,
                    range = 0..31,
                    onSelect = { viewModel.onWeeklyTargetChanged(it) },
                    onDismiss = { showTargetPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save + Delete on the same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.save(onSaved) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.name.isNotBlank() && !uiState.isSaving
                ) {
                    Text(
                        if (uiState.isSaving) "Saving..." else "Save",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Pause / Resume — only in edit mode
                if (uiState.isEditMode) {
                    TextButton(
                        onClick = {
                            if (uiState.isArchived) viewModel.resumeHabit(onSaved)
                            else viewModel.pauseHabit(onSaved)
                        }
                    ) {
                        Text(
                            if (uiState.isArchived) "Resume" else "Pause",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delete — only in edit mode
                if (uiState.isEditMode) {
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Scroll-wheel Number Picker Dialog ──────────────────────

@Composable
private fun NumberPickerDialog(
    currentValue: Int,
    range: IntRange,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentValue - range.first).coerceAtLeast(0)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Weekly goal",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Highlight bar
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.7f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.7f)
                        .padding(top = 40.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(range.toList()) { index, number ->
                        val isSelected = listState.firstVisibleItemIndex == index
                        Text(
                            text = number.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(number) }
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            val selectedIndex = listState.firstVisibleItemIndex
            val selectedNumber = range.elementAtOrElse(selectedIndex) { currentValue }
            TextButton(onClick = {
                onSelect(selectedNumber)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
