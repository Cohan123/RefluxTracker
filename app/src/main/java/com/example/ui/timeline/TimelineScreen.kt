package com.example.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.model.TimelineItem
import com.example.ui.RefluxViewModel
import com.example.ui.components.HighDensityCard
import com.example.ui.components.formatDate
import com.example.ui.components.formatTime
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalMint
import com.example.ui.theme.BotanicalSage
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import com.example.ui.theme.TriggerHighRed
import com.example.ui.theme.TriggerHighRedBg
import com.example.ui.theme.TriggerMediumAmber
import com.example.ui.theme.TriggerMediumAmberBg

@Composable
fun TimelineScreen(
    viewModel: RefluxViewModel,
    onNavigateToEditMeal: (Long) -> Unit = {},
    onNavigateToEditSymptom: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineItems by viewModel.timelineItems.collectAsState()

    var itemToDelete by remember { mutableStateOf<TimelineItem?>(null) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eintrag löschen?") },
            text = {
                Text(
                    when (val item = itemToDelete) {
                        is TimelineItem.MealItem -> "Möchtest du diese Mahlzeit (${item.mealWithFoods.meal.mealType.displayName}) wirklich löschen?"
                        is TimelineItem.SymptomItem -> "Möchtest du dieses Symptom (${item.symptom.getSymptomList().joinToString(", ")}) wirklich löschen?"
                        null -> ""
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (val item = itemToDelete) {
                            is TimelineItem.MealItem -> viewModel.deleteMeal(item.mealWithFoods.meal.id)
                            is TimelineItem.SymptomItem -> viewModel.deleteSymptom(item.symptom.id)
                            null -> {}
                        }
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Löschen", color = TriggerHighRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Timeline & Tagesverlauf",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ),
            color = BotanicalTextPrimary
        )
        Text(
            text = "Tippe auf einen Eintrag, um Details aufzuklappen oder zu bearbeiten.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = BotanicalTextSecondary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (timelineItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = BotanicalMint,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Noch keine Einträge vorhanden",
                        style = MaterialTheme.typography.titleMedium,
                        color = BotanicalTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Protokolliere Mahlzeiten oder Symptome über den Dashboard-Tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BotanicalTextSecondary
                    )
                }
            }
        } else {
            // Group items by date string
            val groupedByDate = timelineItems.groupBy { formatDate(it.timestamp) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedByDate.forEach { (dateStr, itemsForDay) ->
                    item(key = "header_$dateStr") {
                        DayHeader(dateStr = dateStr)
                    }

                    items(
                        items = itemsForDay,
                        key = { item ->
                            when (item) {
                                is TimelineItem.MealItem -> "meal_${item.mealWithFoods.meal.id}"
                                is TimelineItem.SymptomItem -> "symptom_${item.symptom.id}"
                            }
                        }
                    ) { item ->
                        ExpandableTimelineRow(
                            item = item,
                            onEditRequested = {
                                when (item) {
                                    is TimelineItem.MealItem -> onNavigateToEditMeal(item.mealWithFoods.meal.id)
                                    is TimelineItem.SymptomItem -> onNavigateToEditSymptom(item.symptom.id)
                                }
                            },
                            onDeleteRequested = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(dateStr: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(BotanicalSage.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = dateStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BotanicalForest
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            color = BotanicalCardBorder,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ExpandableTimelineRow(
    item: TimelineItem,
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Persistent expansion state per item
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow_rotation"
    )

    HighDensityCard(
        modifier = modifier
            .testTag(
                when (item) {
                    is TimelineItem.MealItem -> "timeline_meal_${item.mealWithFoods.meal.id}"
                    is TimelineItem.SymptomItem -> "timeline_symptom_${item.symptom.id}"
                }
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header Row: Time, Icon, Title, Summary preview, Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Text(
                    text = formatTime(item.timestamp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BotanicalTextPrimary,
                    modifier = Modifier.width(44.dp)
                )

                // Type Icon badge
                val (icon, badgeBg, iconTint) = when (item) {
                    is TimelineItem.MealItem -> Triple(
                        Icons.Default.Restaurant,
                        BotanicalSage.copy(alpha = 0.15f),
                        BotanicalForest
                    )
                    is TimelineItem.SymptomItem -> Triple(
                        Icons.Default.WarningAmber,
                        if (item.symptom.intensity >= 6) TriggerHighRedBg else TriggerMediumAmberBg,
                        if (item.symptom.intensity >= 6) TriggerHighRed else TriggerMediumAmber
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & short summary
                Column(modifier = Modifier.weight(1f)) {
                    when (item) {
                        is TimelineItem.MealItem -> {
                            Text(
                                text = item.mealWithFoods.meal.mealType.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BotanicalTextPrimary
                            )
                            if (!isExpanded) {
                                val foodPreview = if (item.mealWithFoods.foods.isNotEmpty()) {
                                    item.mealWithFoods.foods.joinToString(", ") { it.name }
                                } else {
                                    item.mealWithFoods.meal.portionSize.displayName
                                }
                                Text(
                                    text = foodPreview,
                                    fontSize = 12.sp,
                                    color = BotanicalTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        is TimelineItem.SymptomItem -> {
                            val symptomsList = item.symptom.getSymptomList()
                            val symptomTitle = if (symptomsList.isNotEmpty()) {
                                symptomsList.joinToString(", ")
                            } else {
                                item.symptom.symptomType.displayName
                            }
                            Text(
                                text = symptomTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BotanicalTextPrimary,
                                maxLines = 1
                            )
                            if (!isExpanded) {
                                val extras = mutableListOf<String>()
                                val activities = item.symptom.getActivityList()
                                val remedies = item.symptom.getRemedyList()
                                if (activities.isNotEmpty()) extras.add("Auslöser: ${activities.first()}")
                                if (remedies.isNotEmpty()) extras.add("Maßnahme: ${remedies.first()}")
                                val extraText = if (extras.isNotEmpty()) " • ${extras.joinToString(" • ")}" else ""
                                Text(
                                    text = "Stärke: ${item.symptom.intensity}/10 (${item.symptom.durationMinutes} Min.)$extraText",
                                    fontSize = 12.sp,
                                    color = BotanicalTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Expand arrow indicator
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Einklappen" else "Aufklappen",
                    tint = BotanicalTextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }

            // Expanded detail section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(
                        color = BotanicalCardBorder.copy(alpha = 0.6f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    when (item) {
                        is TimelineItem.MealItem -> {
                            MealExpandedDetails(
                                mealWithFoods = item.mealWithFoods,
                                onEdit = onEditRequested,
                                onDelete = onDeleteRequested
                            )
                        }
                        is TimelineItem.SymptomItem -> {
                            SymptomExpandedDetails(
                                symptom = item.symptom,
                                onEdit = onEditRequested,
                                onDelete = onDeleteRequested
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealExpandedDetails(
    mealWithFoods: MealWithFoods,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Portion Size indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "Portionsgröße: ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BotanicalTextSecondary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(BotanicalSage.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = mealWithFoods.meal.portionSize.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BotanicalForest
                )
            }
        }

        // List of foods contained
        Text(
            text = "Enthaltene Lebensmittel:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = BotanicalTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (mealWithFoods.foods.isEmpty()) {
            Text(
                text = "Keine einzelnen Lebensmittel hinterlegt.",
                fontSize = 12.sp,
                color = BotanicalTextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                mealWithFoods.foods.forEach { food ->
                    FoodDetailPill(name = food.name, category = food.category)
                }
            }
        }

        // Notes if available
        if (mealWithFoods.meal.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Notiz: ${mealWithFoods.meal.notes}",
                fontSize = 12.sp,
                color = BotanicalTextSecondary,
                lineHeight = 16.sp
            )
        }

        // Actions Row (Edit and Delete buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Mahlzeit bearbeiten",
                    tint = BotanicalForest,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Mahlzeit löschen",
                    tint = TriggerHighRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomExpandedDetails(
    symptom: SymptomEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val symptomsList = symptom.getSymptomList()
    val activitiesList = symptom.getActivityList()
    val remediesList = symptom.getRemedyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        // All symptoms
        if (symptomsList.size > 1) {
            Text(
                text = "Symptome (${symptomsList.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BotanicalTextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                symptomsList.forEach { s ->
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                        color = TriggerHighRedBg
                    ) {
                        Text(
                            text = s,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TriggerHighRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "Symptomstärke: ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BotanicalTextSecondary
            )
            val badgeColor = if (symptom.intensity >= 6) TriggerHighRed else TriggerMediumAmber
            val badgeBg = if (symptom.intensity >= 6) TriggerHighRedBg else TriggerMediumAmberBg
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${symptom.intensity} von 10",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Dauer: ca. ${symptom.durationMinutes} Min.",
                fontSize = 12.sp,
                color = BotanicalTextSecondary
            )
        }

        // Tätigkeiten / Situationen als Auslöser
        if (activitiesList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tätigkeit / Situation beim Auftreten:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BotanicalTextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                activitiesList.forEach { act ->
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                        color = BotanicalSage.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = act,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BotanicalForest,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Ergriffene Maßnahmen
        if (remediesList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ergriffene Maßnahmen zur Linderung:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BotanicalTextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                remediesList.forEach { remedy ->
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                        color = BotanicalMint.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = remedy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BotanicalForest,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (symptom.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Notiz: ${symptom.notes}",
                fontSize = 12.sp,
                color = BotanicalTextSecondary,
                lineHeight = 16.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Symptom bearbeiten",
                    tint = BotanicalForest,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Symptom löschen",
                    tint = TriggerHighRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FoodDetailPill(
    name: String,
    category: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = BotanicalSage.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BotanicalForest
            )
            if (category != "Allgemein" && category.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• $category",
                    fontSize = 10.sp,
                    color = BotanicalTextMuted
                )
            }
        }
    }
}
