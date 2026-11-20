package com.example.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SymptomTypesJson
import com.example.data.repository.TimelineEntry
import com.example.ui.RefluxViewModel
import com.example.ui.components.IntensityBadge
import com.example.ui.theme.MetricIntensityBg
import com.example.ui.theme.MetricMealsBg
import com.example.util.DateTimeUtils
import java.util.Calendar

@Composable
fun TimelineScreen(
    viewModel: RefluxViewModel,
    onNavigateToAddMeal: () -> Unit,
    onNavigateToAddSymptom: () -> Unit,
    onNavigateToEditMeal: (Long) -> Unit,
    onNavigateToEditSymptom: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedOffset by viewModel.selectedDayOffset.collectAsStateWithLifecycle()
    val timelineEntries by viewModel.timelineForSelectedDay.collectAsStateWithLifecycle()
    val expansionState = rememberTimelineExpansionState()

    val currentCal = remember(selectedOffset) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, selectedOffset) }
    }
    val dateDisplay = remember(selectedOffset) {
        when (selectedOffset) {
            0 -> "Heute, ${DateTimeUtils.formatShortDate(currentCal.timeInMillis)}"
            -1 -> "Gestern, ${DateTimeUtils.formatShortDate(currentCal.timeInMillis)}"
            else -> DateTimeUtils.formatDateWithYear(currentCal.timeInMillis)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("timeline_screen")
    ) {
        // High Density Date Navigation Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setSelectedDayOffset(selectedOffset - 1) },
                        modifier = Modifier.testTag("prev_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Vorheriger Tag",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dateDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${timelineEntries.size} Ereignisse",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSelectedDayOffset(selectedOffset + 1) },
                        enabled = selectedOffset < 0,
                        modifier = Modifier.testTag("next_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Nächster Tag",
                            tint = if (selectedOffset < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (selectedOffset != 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.setSelectedDayOffset(0) },
                            label = { Text("Zurück zu Heute", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Timeline Content List
        if (timelineEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Keine Einträge an diesem Tag",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Erfasse eine Mahlzeit oder ein aufgetretenes Symptom.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onNavigateToAddMeal,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("+ Mahlzeit")
                        }
                        OutlinedButton(
                            onClick = onNavigateToAddSymptom,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("+ Symptom")
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAddMeal,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("timeline_add_meal_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Mahlzeit")
                    }
                    Button(
                        onClick = onNavigateToAddSymptom,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("timeline_add_symptom_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Symptom")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(timelineEntries) { index, entry ->
                        HighDensityTimelineRow(
                            entry = entry,
                            isLast = index == timelineEntries.size - 1,
                            isExpanded = expansionState.isExpanded(entry.timestamp),
                            onToggle = { expansionState.toggle(entry.timestamp) },
                            onEdit = when (entry) {
                                is TimelineEntry.Meal -> {
                                    { onNavigateToEditMeal(entry.mealWithFoods.meal.id) }
                                }
                                is TimelineEntry.Symptom -> {
                                    { onNavigateToEditSymptom(entry.symptom.id) }
                                }
                            },
                            onDelete = {
                                when (entry) {
                                    is TimelineEntry.Meal -> viewModel.deleteMeal(entry.mealWithFoods.meal.id)
                                    is TimelineEntry.Symptom -> viewModel.deleteSymptom(entry.symptom.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighDensityTimelineRow(
    entry: TimelineEntry,
    isLast: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val isMeal = entry is TimelineEntry.Meal
    val nodeColor = if (isMeal) MetricMealsBg else MetricIntensityBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. Time Column
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = DateTimeUtils.formatTime(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Timeline Graphic Node & Connecting Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(22.dp)
        ) {
            Surface(
                modifier = Modifier.size(22.dp),
                shape = CircleShape,
                color = nodeColor,
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface)
            ) {}
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 3. Content Card (High Density Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (entry) {
                    is TimelineEntry.Meal -> {
                        val meal = entry.mealWithFoods.meal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onToggle)
                                .testTag("timeline_row_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = meal.mealType,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                onEdit?.let { edit ->
                                    IconButton(
                                        onClick = edit,
                                        modifier = Modifier.size(22.dp).testTag("timeline_row_edit")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Bearbeiten",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.size(22.dp).testTag("timeline_row_delete")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Löschen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Eintrag einklappen" else "Eintrag aufklappen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(if (isExpanded) 180f else 0f)
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            MealExpandedDetails(entry)
                        }
                    }

                    is TimelineEntry.Symptom -> {
                        val symptom = entry.symptom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onToggle)
                                .testTag("timeline_row_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = SymptomTypesJson
                                    .decodeOrPrimary(symptom.symptomTypesJson, symptom.symptomType)
                                    .joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                onEdit?.let { edit ->
                                    IconButton(
                                        onClick = edit,
                                        modifier = Modifier.size(22.dp).testTag("timeline_row_edit")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Bearbeiten",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.size(22.dp).testTag("timeline_row_delete")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Löschen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Eintrag einklappen" else "Eintrag aufklappen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(if (isExpanded) 180f else 0f)
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SymptomExpandedDetails(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealExpandedDetails(entry: TimelineEntry.Meal) {
    val meal = entry.mealWithFoods.meal
    Column(modifier = Modifier.fillMaxWidth()) {
        // Portion
        if (meal.portion.isNotBlank()) {
            Text(
                text = "Portion: ${meal.portion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Foods
        val foodNames = entry.mealWithFoods.foods.joinToString(", ") { it.name }
        if (foodNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lebensmittel: $foodNames",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Notes
        if (meal.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Notiz: ${meal.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Empty-state hint when there's literally nothing to show
        if (meal.portion.isBlank() && foodNames.isEmpty() && meal.notes.isBlank()) {
            Text(
                text = "Keine weiteren Angaben erfasst.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SymptomExpandedDetails(entry: TimelineEntry.Symptom) {
    val symptom = entry.symptom
    val typeNames = SymptomTypesJson.decodeOrPrimary(
        symptom.symptomTypesJson,
        symptom.symptomType
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        if (typeNames.size > 1) {
            Text(
                text = "Symptomarten: ${typeNames.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        // Intensity (already shown in the summary line, but re-stated here
        // prominently for clarity when expanded)
        Text(
            text = "Intensität: ${symptom.intensity}/10",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        // Duration
        if (symptom.durationMinutes != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dauer: ${symptom.durationMinutes} Minuten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Activities
        if (entry.activities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tätigkeiten: ${entry.activities.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Remedies
        if (entry.remedies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Maßnahmen: ${entry.remedies.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Notes
        if (symptom.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Notiz: ${symptom.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        // Empty-state hint
        if (symptom.durationMinutes == null &&
            symptom.notes.isBlank() &&
            entry.activities.isEmpty() &&
            entry.remedies.isEmpty() &&
            typeNames.size <= 1
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keine weiteren Angaben erfasst.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
