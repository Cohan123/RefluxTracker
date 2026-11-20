package com.example.ui.symptom

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SymptomTypes
import com.example.data.model.SymptomTypesJson
import com.example.ui.RefluxViewModel
import com.example.ui.components.DateTimePickerCard
import com.example.ui.components.IntensityBadge
import com.example.ui.theme.SymptomMild
import com.example.ui.theme.SymptomModerate
import com.example.ui.theme.SymptomSevere
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSymptomScreen(
    viewModel: RefluxViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    symptomId: Long? = null
) {
    val isEditing = symptomId != null
    val allActivities by viewModel.allActivities.collectAsStateWithLifecycle()
    val allRemedies by viewModel.allRemedies.collectAsStateWithLifecycle()

    var symptomTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedSymptomTypes by remember { mutableStateOf(setOf(SymptomTypes.SODBRENNEN)) }
    var primarySymptomType by remember { mutableStateOf<String?>(null) }
    var intensityFloat by remember { mutableFloatStateOf(5.0f) }
    var selectedDurationMinutes by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }

    var selectedActivityIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedRemedyIds by remember { mutableStateOf(setOf<Long>()) }

    var showAddActivityDialog by remember { mutableStateOf(false) }
    var showAddRemedyDialog by remember { mutableStateOf(false) }
    var newActivityName by remember { mutableStateOf("") }
    var newRemedyName by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(isEditing) }

    LaunchedEffect(symptomId) {
        if (symptomId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        viewModel.getSymptomDetails(symptomId) { details ->
            if (details == null) {
                onNavigateBack()
            } else {
                symptomTimestamp = details.symptom.timestamp
                selectedSymptomTypes = SymptomTypesJson
                    .decodeOrPrimary(
                        details.symptom.symptomTypesJson,
                        details.symptom.symptomType
                    )
                    .toSet()
                primarySymptomType = details.symptom.symptomType
                intensityFloat = details.symptom.intensity.toFloat()
                selectedDurationMinutes = details.symptom.durationMinutes
                notes = details.symptom.notes
                selectedActivityIds = details.activities.map { it.id }.toSet()
                selectedRemedyIds = details.remedies.map { it.id }.toSet()
                isLoading = false
            }
        }
    }

    val intensityInt = intensityFloat.roundToInt()

    val sliderColor = when {
        intensityInt <= 3 -> SymptomMild
        intensityInt <= 6 -> SymptomModerate
        else -> SymptomSevere
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Symptom bearbeiten" else "Symptom erfassen",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("add_symptom_form"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Time / Date display
            item {
                DateTimePickerCard(
                    label = "Zeitpunkt des Auftretens",
                    timestamp = symptomTimestamp,
                    onTimestampChange = { symptomTimestamp = it }
                )
            }

            // 2. Symptom Type Selector (multi-select)
            item {
                Column {
                    Text(
                        text = "Art des Symptoms",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mehrfachauswahl möglich",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SymptomTypes.ALL.forEach { type ->
                            val isSelected = type in selectedSymptomTypes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSymptomTypes = if (isSelected) {
                                        selectedSymptomTypes - type
                                    } else {
                                        selectedSymptomTypes + type
                                    }
                                },
                                label = { Text(type) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                    if (selectedSymptomTypes.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bitte mindestens eine Symptomart auswählen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 3. Intensity Slider (0..10)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Symptomstärke (0–10)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IntensityBadge(intensity = intensityInt)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Slider(
                            value = intensityFloat,
                            onValueChange = { intensityFloat = it },
                            valueRange = 0f..10f,
                            steps = 9,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("intensity_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = sliderColor,
                                activeTrackColor = sliderColor
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0 (Keine)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("5 (Mittel)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("10 (Sehr stark)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 4. Optional Duration
            item {
                Column {
                    Text(
                        text = "Dauer (optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            null to "Keine Angabe",
                            15 to "15 Min",
                            30 to "30 Min",
                            60 to "1 Stunde",
                            120 to "2 Stunden",
                            240 to "Mehrere Std."
                        ).forEach { (minutes, label) ->
                            val isSelected = (selectedDurationMinutes == minutes)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDurationMinutes = minutes },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // 5. Activities (possible triggers, multi-select)
            item {
                CatalogChipsSection(
                    title = "Tätigkeiten / Auslöser (optional)",
                    hint = "Wähle aus, was du in der Nähe des Auftretens gemacht hast.",
                    options = allActivities.map { CatalogOption(it.id, it.name) },
                    selectedIds = selectedActivityIds,
                    onToggle = { id ->
                        selectedActivityIds = if (id in selectedActivityIds) {
                            selectedActivityIds - id
                        } else {
                            selectedActivityIds + id
                        }
                    },
                    onAddCustom = { showAddActivityDialog = true },
                    addLabel = "Neue Tätigkeit",
                    testTag = "activity_chips"
                )
            }

            // 6. Remedies (multi-select)
            item {
                CatalogChipsSection(
                    title = "Maßnahmen (optional)",
                    hint = "Wähle aus, was du gegen das Symptom unternommen hast.",
                    options = allRemedies.map { CatalogOption(it.id, it.name) },
                    selectedIds = selectedRemedyIds,
                    onToggle = { id ->
                        selectedRemedyIds = if (id in selectedRemedyIds) {
                            selectedRemedyIds - id
                        } else {
                            selectedRemedyIds + id
                        }
                    },
                    onAddCustom = { showAddRemedyDialog = true },
                    addLabel = "Neue Maßnahme",
                    testTag = "remedy_chips"
                )
            }

            // 7. Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("symptom_notes_field"),
                    label = { Text("Notiz (optional)") },
                    placeholder = { Text("Z.B. im Liegen aufgetreten, nach dem Sport...") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // 8. Save Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Canonical order first, then any unknown legacy types.
                        // The original primary is kept when it is still selected
                        // so editing does not silently change analysis/CSV keys.
                        val orderedTypes = SymptomTypes.ALL.filter { it in selectedSymptomTypes } +
                            selectedSymptomTypes.filter { it !in SymptomTypes.ALL }
                        val typesToSave = orderedTypes.ifEmpty { selectedSymptomTypes.toList() }
                        val primary = primarySymptomType
                            ?.takeIf { it in typesToSave }
                            ?: typesToSave.first()
                        val editingId = symptomId
                        if (editingId != null) {
                            viewModel.updateSymptom(
                                symptomId = editingId,
                                timestamp = symptomTimestamp,
                                symptomType = primary,
                                intensity = intensityInt,
                                durationMinutes = selectedDurationMinutes,
                                notes = notes.trim(),
                                symptomTypes = typesToSave,
                                activityIds = selectedActivityIds.toList(),
                                remedyIds = selectedRemedyIds.toList(),
                                onSuccess = onNavigateBack
                            )
                        } else {
                            viewModel.addSymptom(
                                timestamp = symptomTimestamp,
                                symptomType = primary,
                                intensity = intensityInt,
                                durationMinutes = selectedDurationMinutes,
                                notes = notes.trim(),
                                symptomTypes = typesToSave,
                                activityIds = selectedActivityIds.toList(),
                                remedyIds = selectedRemedyIds.toList(),
                                onSuccess = onNavigateBack
                            )
                        }
                    },
                    enabled = !isLoading && selectedSymptomTypes.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_symptom_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Änderungen speichern" else "Symptom speichern",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        }
    }

    if (showAddActivityDialog) {
        AddCatalogEntryDialog(
            title = "Neue Tätigkeit anlegen",
            label = "Name der Tätigkeit",
            value = newActivityName,
            onValueChange = { newActivityName = it },
            onConfirm = {
                val name = newActivityName.trim()
                viewModel.addCustomActivity(name) { id ->
                    selectedActivityIds = selectedActivityIds + id
                }
                newActivityName = ""
                showAddActivityDialog = false
            },
            onDismiss = {
                newActivityName = ""
                showAddActivityDialog = false
            }
        )
    }

    if (showAddRemedyDialog) {
        AddCatalogEntryDialog(
            title = "Neue Maßnahme anlegen",
            label = "Name der Maßnahme",
            value = newRemedyName,
            onValueChange = { newRemedyName = it },
            onConfirm = {
                val name = newRemedyName.trim()
                viewModel.addCustomRemedy(name) { id ->
                    selectedRemedyIds = selectedRemedyIds + id
                }
                newRemedyName = ""
                showAddRemedyDialog = false
            },
            onDismiss = {
                newRemedyName = ""
                showAddRemedyDialog = false
            }
        )
    }
}

private data class CatalogOption(val id: Long, val name: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogChipsSection(
    title: String,
    hint: String,
    options: List<CatalogOption>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onAddCustom: () -> Unit,
    addLabel: String,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddCustom) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(addLabel)
            }
        }

        if (options.isEmpty()) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option.id in selectedIds
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(option.id) },
                        label = { Text(option.name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCatalogEntryDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = "Der Eintrag wird dauerhaft in deiner Liste gespeichert.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = value.isNotBlank()
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
