package com.example.ui.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analysis.DetectedHabit
import com.example.data.model.FoodEntity
import com.example.ui.RefluxViewModel
import com.example.util.DateTimeUtils

/**
 * Management screen for automatically detected meal habits.
 *
 * Renaming or changing the foods only affects future suggestions; the original
 * meal history is never rewritten. Deleting a habit hides it locally.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(
    viewModel: RefluxViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<DetectedHabit?>(null) }
    var deleteCandidate by remember { mutableStateOf<DetectedHabit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gewohnheiten", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("habits_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("habits_screen")
        ) {
            Text(
                text = "Erkannt aus deinen Mahlzeiten: mindestens 3 ähnliche Einträge an " +
                    "mindestens 3 verschiedenen Tagen. Alles bleibt lokal und nachvollziehbar.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (habits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Noch keine Gewohnheit erkannt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Erfasse wiederkehrende Mahlzeiten. Sobald eine Kombination " +
                            "an mindestens drei Tagen ähnlich vorkommt, erscheint sie hier " +
                            "und als Vorschlag bei der Eingabe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(habits, key = { it.signature }) { habit ->
                        HabitCard(
                            habit = habit,
                            onEdit = { editing = habit },
                            onDelete = { deleteCandidate = habit }
                        )
                    }
                }
            }
        }
    }

    editing?.let { habit ->
        HabitEditDialog(
            habit = habit,
            foods = allFoods,
            onDismiss = { editing = null },
            onSave = { name, foods ->
                viewModel.saveHabitOverride(
                    signature = habit.signature,
                    customName = name,
                    customFoods = foods,
                    isDeleted = false
                )
                editing = null
            }
        )
    }

    deleteCandidate?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Gewohnheit löschen?") },
            text = {
                Text(
                    "„${habit.name}“ wird aus den Vorschlägen entfernt. " +
                        "Deine erfassten Mahlzeiten bleiben unverändert."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(habit.signature)
                        deleteCandidate = null
                    },
                    modifier = Modifier.testTag("habit_delete_confirm")
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun HabitCard(
    habit: DetectedHabit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${habit.mealType} · meist gegen ${habit.typicalTimeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp).testTag("habit_edit")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Gewohnheit bearbeiten",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("habit_delete")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Gewohnheit löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = habit.foodNames.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${habit.occurrenceCount}× erfasst · ${habit.distinctDayCount} Tage · " +
                    "zuletzt ${DateTimeUtils.formatShortDate(habit.lastUsedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (habit.isEdited) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Manuell angepasst",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitEditDialog(
    habit: DetectedHabit,
    foods: List<FoodEntity>,
    onDismiss: () -> Unit,
    onSave: (String?, List<String>) -> Unit
) {
    var name by remember(habit.signature) { mutableStateOf(habit.name) }
    var selectedFoods by remember(habit.signature) {
        mutableStateOf(habit.foodNames.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gewohnheit bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bezeichnung") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_name_field")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lebensmittel der Gewohnheit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    foods.forEach { food ->
                        FilterChip(
                            selected = food.name in selectedFoods,
                            onClick = {
                                selectedFoods = if (food.name in selectedFoods) {
                                    selectedFoods - food.name
                                } else {
                                    selectedFoods + food.name
                                }
                            },
                            label = { Text(food.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim().takeIf { it.isNotEmpty() && it != habit.name },
                        selectedFoods.toList()
                    )
                },
                enabled = selectedFoods.isNotEmpty(),
                modifier = Modifier.testTag("habit_save_button")
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
