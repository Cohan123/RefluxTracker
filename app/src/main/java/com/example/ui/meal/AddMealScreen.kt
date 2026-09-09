package com.example.ui.meal

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodEntity
import com.example.data.model.MealType
import com.example.data.model.PortionSize
import com.example.ui.RefluxViewModel
import com.example.ui.components.DateTimePickerRow
import com.example.ui.components.HighDensityCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BotanicalCardBg
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalCream
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalSage
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMealScreen(
    viewModel: RefluxViewModel,
    mealId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allFoods by viewModel.allFoods.collectAsState()
    val scope = rememberCoroutineScope()
    val isEditing = mealId != null && mealId > 0L

    var selectedMealType by remember { mutableStateOf(MealType.MITTAGESSEN) }
    var selectedPortion by remember { mutableStateOf(PortionSize.MITTEL) }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val selectedFoodIds = remember { mutableStateListOf<Long>() }
    var foodSearchQuery by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    LaunchedEffect(mealId) {
        if (isEditing && mealId != null) {
            val mwf = viewModel.getMealWithFoods(mealId)
            if (mwf != null) {
                selectedMealType = mwf.meal.mealType
                selectedPortion = mwf.meal.portionSize
                selectedTimestamp = mwf.meal.timestamp
                notesText = mwf.meal.notes
                selectedFoodIds.clear()
                selectedFoodIds.addAll(mwf.foods.map { it.id })
            }
        }
    }

    val filteredFoods = remember(foodSearchQuery, allFoods) {
        if (foodSearchQuery.isBlank()) {
            allFoods
        } else {
            allFoods.filter { it.name.contains(foodSearchQuery, ignoreCase = true) }
        }
    }

    val hasMatchingFood = allFoods.any { it.name.equals(foodSearchQuery.trim(), ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Mahlzeit bearbeiten" else "Mahlzeit protokollieren",
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

            // Meal Type Selector
            item {
                SectionHeader(title = "Art der Mahlzeit")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MealType.values().forEach { type ->
                        val isSelected = selectedMealType == type
                        SelectableChip(
                            label = type.displayName,
                            selected = isSelected,
                            onClick = { selectedMealType = type }
                        )
                    }
                }
            }

            // Portion Size Selector
            item {
                SectionHeader(title = "Portionsgröße")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PortionSize.values().forEach { portion ->
                        val isSelected = selectedPortion == portion
                        SelectableChip(
                            label = portion.displayName,
                            selected = isSelected,
                            onClick = { selectedPortion = portion },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Food Search & Selection
            item {
                SectionHeader(
                    title = "Enthaltene Lebensmittel",
                    subtitle = "Tippe zum Auswählen"
                )

                // Search Input with quick add
                OutlinedTextField(
                    value = foodSearchQuery,
                    onValueChange = { foodSearchQuery = it },
                    placeholder = { Text("Lebensmittel suchen oder neu eingeben...", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BotanicalSurface,
                        unfocusedContainerColor = BotanicalSurface,
                        focusedBorderColor = BotanicalForest,
                        unfocusedBorderColor = BotanicalCardBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        if (foodSearchQuery.isNotBlank() && !hasMatchingFood) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val newId = viewModel.findOrCreateFood(foodSearchQuery.trim())
                                        if (!selectedFoodIds.contains(newId)) {
                                            selectedFoodIds.add(newId)
                                        }
                                        foodSearchQuery = ""
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BotanicalSage),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .height(34.dp)
                            ) {
                                Text("+ Hinzufügen", fontSize = 11.sp)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_search_input")
                )

                // Selected Foods Pills (if any)
                if (selectedFoodIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ausgewählt (${selectedFoodIds.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BotanicalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedFoodIds.forEach { id ->
                            val food = allFoods.find { it.id == id }
                            val name = food?.name ?: "Lebensmittel #$id"
                            SelectedFoodPill(
                                name = name,
                                onRemove = { selectedFoodIds.remove(id) }
                            )
                        }
                    }
                }

                // Available Foods list chips
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vorschläge aus Datenbank:",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredFoods.take(18).forEach { food ->
                        val isSelected = selectedFoodIds.contains(food.id)
                        FoodSelectionChip(
                            name = food.name,
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedFoodIds.remove(food.id)
                                else selectedFoodIds.add(food.id)
                            }
                        )
                    }
                }
            }

            // Notes Input
            item {
                SectionHeader(title = "Optionale Notizen")
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("z.B. spät abends gegessen, hastig geschlungen...", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BotanicalSurface,
                        unfocusedContainerColor = BotanicalSurface,
                        focusedBorderColor = BotanicalForest,
                        unfocusedBorderColor = BotanicalCardBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (isEditing && mealId != null) {
                            viewModel.updateMeal(
                                mealId = mealId,
                                mealType = selectedMealType,
                                portionSize = selectedPortion,
                                foodIds = selectedFoodIds.toList(),
                                timestamp = selectedTimestamp,
                                notes = notesText.trim()
                            )
                        } else {
                            viewModel.addMeal(
                                mealType = selectedMealType,
                                portionSize = selectedPortion,
                                foodIds = selectedFoodIds.toList(),
                                timestamp = selectedTimestamp,
                                notes = notesText.trim()
                            )
                        }
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_meal_button")
                ) {
                    Text(
                        text = if (isEditing) "Änderungen speichern" else "Mahlzeit speichern",
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
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) BotanicalForest else BotanicalSurface
    val border = if (selected) BotanicalForest else BotanicalCardBorder
    val text = if (selected) BotanicalCream else BotanicalTextPrimary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = bg
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = text
            )
        }
    }
}

@Composable
fun FoodSelectionChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) BotanicalSage else BotanicalSurface
    val text = if (selected) BotanicalSurface else BotanicalTextPrimary
    val border = if (selected) BotanicalSage else BotanicalCardBorder

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = bg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BotanicalSurface,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = name,
                fontSize = 12.sp,
                color = text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun SelectedFoodPill(
    name: String,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        color = BotanicalForest
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BotanicalSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Entfernen",
                    tint = BotanicalSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
