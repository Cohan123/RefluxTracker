package com.example.ui.symptom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RefluxConstants
import com.example.data.model.SymptomType
import com.example.ui.RefluxViewModel
import com.example.ui.components.DateTimePickerRow
import com.example.ui.components.HighDensityCard
import com.example.ui.components.SectionHeader
import com.example.ui.meal.SelectableChip
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalCream
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalMint
import com.example.ui.theme.BotanicalSage
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import com.example.ui.theme.TriggerHighRed
import com.example.ui.theme.TriggerHighRedBg
import com.example.ui.theme.TriggerLowGreen
import com.example.ui.theme.TriggerLowGreenBg
import com.example.ui.theme.TriggerMediumAmber
import com.example.ui.theme.TriggerMediumAmberBg

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSymptomScreen(
    viewModel: RefluxViewModel,
    symptomId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = symptomId != null && symptomId > 0L

    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    val selectedActivities = remember { mutableStateListOf<String>() }
    val selectedRemedies = remember { mutableStateListOf<String>() }

    var customSymptomText by remember { mutableStateOf("") }
    var customActivityText by remember { mutableStateOf("") }
    var customRemedyText by remember { mutableStateOf("") }

    var intensityFloat by remember { mutableFloatStateOf(5f) }
    val intensity = intensityFloat.toInt()
    var selectedDurationMinutes by remember { mutableIntStateOf(30) }
    var notesText by remember { mutableStateOf("") }

    // Initiale Auswahl bei neuem Symptom: Sodbrennen vorwählen
    LaunchedEffect(Unit) {
        if (!isEditing && selectedSymptoms.isEmpty()) {
            selectedSymptoms.add("Sodbrennen")
        }
    }

    // Beim Bearbeiten: Bestehenden Datensatz asynchron laden
    LaunchedEffect(symptomId) {
        if (isEditing && symptomId != null) {
            val existing = viewModel.getSymptom(symptomId)
            if (existing != null) {
                selectedTimestamp = existing.timestamp
                intensityFloat = existing.intensity.toFloat()
                selectedDurationMinutes = existing.durationMinutes
                notesText = existing.notes

                selectedSymptoms.clear()
                selectedSymptoms.addAll(existing.getSymptomList())

                selectedActivities.clear()
                selectedActivities.addAll(existing.getActivityList())

                selectedRemedies.clear()
                selectedRemedies.addAll(existing.getRemedyList())
            }
        }
    }

    val (intensityColor, intensityBg, intensityLabel) = when {
        intensity <= 3 -> Triple(TriggerLowGreen, TriggerLowGreenBg, "Leicht / kaum störend")
        intensity <= 6 -> Triple(TriggerMediumAmber, TriggerMediumAmberBg, "Mäßig / spürbar")
        else -> Triple(TriggerHighRed, TriggerHighRedBg, "Stark / schmerzhaft")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Symptom bearbeiten" else "Symptom erfassen",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = BotanicalTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BotanicalCream)
            )
        },
        containerColor = BotanicalCream
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Datum & Uhrzeit rückwirkend anpassen
            item {
                SectionHeader(
                    title = "Datum & Uhrzeit",
                    subtitle = "Rückwirkend anpassbar"
                )
                DateTimePickerRow(
                    timestamp = selectedTimestamp,
                    onTimestampChanged = { selectedTimestamp = it }
                )
            }

            // 1. Symptomauswahl (Mehrfachauswahl)
            item {
                SectionHeader(
                    title = "Aufgetretene Symptome",
                    subtitle = "Mehrfachauswahl möglich (${selectedSymptoms.size} gewählt)"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RefluxConstants.STANDARD_SYMPTOMS.forEach { symptomName ->
                        val isSelected = selectedSymptoms.contains(symptomName)
                        MultiSelectChip(
                            label = symptomName,
                            selected = isSelected,
                            activeColor = TriggerHighRed,
                            onClick = {
                                if (isSelected) {
                                    if (selectedSymptoms.size > 1) {
                                        selectedSymptoms.remove(symptomName)
                                    }
                                } else {
                                    selectedSymptoms.add(symptomName)
                                }
                            }
                        )
                    }

                    // Benutzerdefinierte Symptome anzeigen
                    selectedSymptoms.filterNot { RefluxConstants.STANDARD_SYMPTOMS.contains(it) }.forEach { customName ->
                        MultiSelectChip(
                            label = customName,
                            selected = true,
                            activeColor = TriggerHighRed,
                            onClick = { selectedSymptoms.remove(customName) }
                        )
                    }
                }

                // Eigenes Symptom hinzufügen
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customSymptomText,
                        onValueChange = { customSymptomText = it },
                        placeholder = { Text("Anderes Symptom ergänzen...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BotanicalSurface,
                            unfocusedContainerColor = BotanicalSurface,
                            focusedBorderColor = TriggerHighRed,
                            unfocusedBorderColor = BotanicalCardBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = customSymptomText.trim()
                            if (trimmed.isNotEmpty() && !selectedSymptoms.contains(trimmed)) {
                                selectedSymptoms.add(trimmed)
                                customSymptomText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TriggerHighRed),
                        shape = RoundedCornerShape(8.dp),
                        enabled = customSymptomText.isNotBlank(),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Hinzufügen")
                    }
                }
            }

            // 2. Tätigkeiten / Situationen als Auslöser (Mehrfachauswahl)
            item {
                SectionHeader(
                    title = "Tätigkeit / Situation beim Auftreten",
                    subtitle = "Auslöser / Begleitumstand (Mehrfachauswahl)"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RefluxConstants.STANDARD_ACTIVITIES.forEach { actName ->
                        val isSelected = selectedActivities.contains(actName)
                        MultiSelectChip(
                            label = actName,
                            selected = isSelected,
                            activeColor = BotanicalForest,
                            onClick = {
                                if (isSelected) selectedActivities.remove(actName)
                                else selectedActivities.add(actName)
                            }
                        )
                    }

                    // Benutzerdefinierte Tätigkeiten
                    selectedActivities.filterNot { RefluxConstants.STANDARD_ACTIVITIES.contains(it) }.forEach { customAct ->
                        MultiSelectChip(
                            label = customAct,
                            selected = true,
                            activeColor = BotanicalForest,
                            onClick = { selectedActivities.remove(customAct) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customActivityText,
                        onValueChange = { customActivityText = it },
                        placeholder = { Text("Andere Situation / Tätigkeit ergänzen...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BotanicalSurface,
                            unfocusedContainerColor = BotanicalSurface,
                            focusedBorderColor = BotanicalForest,
                            unfocusedBorderColor = BotanicalCardBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = customActivityText.trim()
                            if (trimmed.isNotEmpty() && !selectedActivities.contains(trimmed)) {
                                selectedActivities.add(trimmed)
                                customActivityText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                        shape = RoundedCornerShape(8.dp),
                        enabled = customActivityText.isNotBlank(),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Hinzufügen")
                    }
                }
            }

            // 3. Maßnahmen gegen Sodbrennen (Mehrfachauswahl)
            item {
                SectionHeader(
                    title = "Ergriffene Maßnahmen zur Linderung",
                    subtitle = "Was hat geholfen? (Mehrfachauswahl)"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RefluxConstants.STANDARD_REMEDIES.forEach { remedyName ->
                        val isSelected = selectedRemedies.contains(remedyName)
                        MultiSelectChip(
                            label = remedyName,
                            selected = isSelected,
                            activeColor = BotanicalSage,
                            onClick = {
                                if (isSelected) selectedRemedies.remove(remedyName)
                                else selectedRemedies.add(remedyName)
                            }
                        )
                    }

                    // Benutzerdefinierte Maßnahmen
                    selectedRemedies.filterNot { RefluxConstants.STANDARD_REMEDIES.contains(it) }.forEach { customRemedy ->
                        MultiSelectChip(
                            label = customRemedy,
                            selected = true,
                            activeColor = BotanicalSage,
                            onClick = { selectedRemedies.remove(customRemedy) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customRemedyText,
                        onValueChange = { customRemedyText = it },
                        placeholder = { Text("Individuelle Maßnahme ergänzen...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BotanicalSurface,
                            unfocusedContainerColor = BotanicalSurface,
                            focusedBorderColor = BotanicalSage,
                            unfocusedBorderColor = BotanicalCardBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = customRemedyText.trim()
                            if (trimmed.isNotEmpty() && !selectedRemedies.contains(trimmed)) {
                                selectedRemedies.add(trimmed)
                                customRemedyText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BotanicalSage),
                        shape = RoundedCornerShape(8.dp),
                        enabled = customRemedyText.isNotBlank(),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Hinzufügen")
                    }
                }
            }

            // 4. Intensität Slider (1 bis 10)
            item {
                SectionHeader(title = "Symptomstärke (1 - 10)")
                HighDensityCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Intensität: $intensity von 10",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = intensityColor
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(intensityBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = intensityLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = intensityColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = intensityFloat,
                            onValueChange = { intensityFloat = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = intensityColor,
                                activeTrackColor = intensityColor,
                                inactiveTrackColor = BotanicalCardBorder
                            ),
                            modifier = Modifier.testTag("symptom_intensity_slider")
                        )
                    }
                }
            }

            // 5. Geschätzte Dauer
            item {
                SectionHeader(title = "Geschätzte Dauer")
                val durations = listOf(15, 30, 60, 120, 240)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    durations.forEach { d ->
                        val isSelected = selectedDurationMinutes == d
                        val label = if (d < 60) "${d}m" else "${d / 60}h"
                        SelectableChip(
                            label = label,
                            selected = isSelected,
                            onClick = { selectedDurationMinutes = d },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Optionale Notizen
            item {
                SectionHeader(title = "Optionale Notizen")
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("z.B. nach dem Bücken aufgetreten, warmes Wasser half sofort...", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BotanicalSurface,
                        unfocusedContainerColor = BotanicalSurface,
                        focusedBorderColor = TriggerHighRed,
                        unfocusedBorderColor = BotanicalCardBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 7. Speichern Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val symptomsToSave = if (selectedSymptoms.isNotEmpty()) {
                            selectedSymptoms.toList()
                        } else {
                            listOf("Sodbrennen")
                        }

                        if (isEditing && symptomId != null) {
                            viewModel.updateSymptom(
                                symptomId = symptomId,
                                symptoms = symptomsToSave,
                                intensity = intensity,
                                durationMinutes = selectedDurationMinutes,
                                timestamp = selectedTimestamp,
                                notes = notesText.trim(),
                                activities = selectedActivities.toList(),
                                remedies = selectedRemedies.toList()
                            )
                        } else {
                            viewModel.addSymptom(
                                symptoms = symptomsToSave,
                                intensity = intensity,
                                durationMinutes = selectedDurationMinutes,
                                timestamp = selectedTimestamp,
                                notes = notesText.trim(),
                                activities = selectedActivities.toList(),
                                remedies = selectedRemedies.toList()
                            )
                        }
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TriggerHighRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_symptom_button")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Healing else Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEditing) "Änderungen speichern" else "Symptom speichern",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MultiSelectChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) activeColor else BotanicalSurface
    val border = if (selected) activeColor else BotanicalCardBorder
    val textColor = if (selected) BotanicalCream else BotanicalTextPrimary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = bg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BotanicalCream,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
