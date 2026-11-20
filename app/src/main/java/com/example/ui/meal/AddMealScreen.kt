package com.example.ui.meal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analysis.DetectedHabit
import com.example.analysis.HabitDetector
import com.example.data.model.FoodCategories
import com.example.data.model.FoodEntity
import com.example.data.model.MealTypes
import com.example.ui.RefluxViewModel
import com.example.ui.components.DateTimePickerCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMealScreen(
    viewModel: RefluxViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    mealId: Long? = null
) {
    val isEditing = mealId != null
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    var mealTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedMealType by remember { mutableStateOf(MealTypes.FRUEHSTUECK) }
    var selectedPortion by remember { mutableStateOf("Normal") }
    var notes by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var foodPickerVisible by remember { mutableStateOf(false) }

    val selectedFoods = remember { mutableStateOf(setOf<FoodEntity>()) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var newFoodName by remember { mutableStateOf("") }
    var newFoodCategories by remember { mutableStateOf(setOf<String>()) }

    var isLoading by remember { mutableStateOf(isEditing) }

    LaunchedEffect(mealId) {
        if (mealId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        viewModel.getMealDetails(mealId) { details ->
            if (details == null) {
                onNavigateBack()
            } else {
                mealTimestamp = details.meal.timestamp
                selectedMealType = details.meal.mealType
                selectedPortion = details.meal.portion
                notes = details.meal.notes
                selectedFoods.value = details.foods.toSet()
                isLoading = false
            }
        }
    }

    // Top quick choices for one-tap selection; the full searchable list lives in
    // the keyboard-safe picker overlay.
    val quickFoods = remember(allFoods) { allFoods.take(18) }

    val filteredFoods = remember(searchQuery, allFoods) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            allFoods
        } else {
            allFoods.filter {
                it.name.contains(query, ignoreCase = true) ||
                    FoodCategories.labels(FoodCategories.decode(it.categories))
                        .any { label -> label.contains(query, ignoreCase = true) }
            }
        }
    }

    val habitSuggestions = remember(habits, selectedMealType, selectedFoods.value) {
        HabitDetector.suggestions(
            habits = habits,
            mealType = selectedMealType,
            selectedFoodNames = selectedFoods.value.map { it.name }
        )
    }

    fun resolveFoods(names: List<String>): List<FoodEntity> {
        val byKey = allFoods.associateBy { HabitDetector.normalizeKey(it.name) }
        return names.mapNotNull { byKey[HabitDetector.normalizeKey(it)] }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditing) "Mahlzeit bearbeiten" else "Mahlzeit erfassen",
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
            modifier = Modifier.fillMaxSize()
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
                        .testTag("add_meal_form"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Time / Date display
                    item {
                        DateTimePickerCard(
                            label = "Zeitpunkt der Mahlzeit",
                            timestamp = mealTimestamp,
                            onTimestampChange = { mealTimestamp = it }
                        )
                    }

                    // 2. Meal Type Selector
                    item {
                        Column {
                            Text(
                                text = "Mahlzeitentyp",
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
                                MealTypes.ALL.forEach { type ->
                                    FilterChip(
                                        selected = (selectedMealType == type),
                                        onClick = { selectedMealType = type },
                                        label = { Text(type) },
                                        leadingIcon = if (selectedMealType == type) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Detected habits for this meal type
                    if (habitSuggestions.isNotEmpty() && !isEditing) {
                        item {
                            HabitSuggestionSection(
                                habits = habitSuggestions,
                                onApply = { habit ->
                                    val foods = resolveFoods(habit.foodNames)
                                    if (foods.isNotEmpty()) {
                                        selectedFoods.value = selectedFoods.value + foods
                                    }
                                }
                            )
                        }
                    }

                    // 4. Selected Foods chips display
                    item {
                        Column {
                            Text(
                                text = "Ausgewählte Lebensmittel (${selectedFoods.value.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedFoods.value.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Wähle unten die enthaltenen Lebensmittel aus oder suche danach.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedFoods.value.forEach { food ->
                                        InputChip(
                                            selected = true,
                                            onClick = {
                                                selectedFoods.value = selectedFoods.value - food
                                            },
                                            label = { Text(food.name) },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Entfernen",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Food search (opens keyboard-safe picker) & quick selection
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lebensmittel hinzufügen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = { showAddFoodDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Neues Lebensmittel")
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    foodPickerVisible = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("food_picker_open_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Suchen & aus Liste wählen (${allFoods.size})")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Schnellauswahl",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickFoods.forEach { food ->
                                    val isSelected = selectedFoods.value.contains(food)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFoods.value = if (isSelected) {
                                                selectedFoods.value - food
                                            } else {
                                                selectedFoods.value + food
                                            }
                                        },
                                        label = { Text(food.name) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // 6. Portion selector ("Groß" is also the meal-level
                    //    "Große Portion" category used by the analysis)
                    item {
                        Column {
                            Text(
                                text = "Portionsgröße / große Portion",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Klein", "Normal", "Groß").forEach { portion ->
                                    FilterChip(
                                        selected = (selectedPortion == portion),
                                        onClick = { selectedPortion = portion },
                                        label = { Text(portion) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "„Groß“ wird in der Auswertung zusätzlich als Kategorie " +
                                    "„Große Portion“ auf Mahlzeitenebene gewertet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 7. Notes field
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meal_notes_field"),
                            label = { Text("Notiz (optional)") },
                            placeholder = { Text("Z.B. unterwegs gegessen, fettig, hastig...") },
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                    }

                    // 8. Save Button
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val editingId = mealId
                                if (editingId != null) {
                                    viewModel.updateMeal(
                                        mealId = editingId,
                                        timestamp = mealTimestamp,
                                        mealType = selectedMealType,
                                        portion = selectedPortion,
                                        notes = notes.trim(),
                                        foodIds = selectedFoods.value.map { it.id },
                                        onSuccess = onNavigateBack
                                    )
                                } else {
                                    viewModel.addMeal(
                                        timestamp = mealTimestamp,
                                        mealType = selectedMealType,
                                        portion = selectedPortion,
                                        notes = notes.trim(),
                                        foodIds = selectedFoods.value.map { it.id },
                                        onSuccess = onNavigateBack
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("save_meal_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Restaurant, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEditing) "Änderungen speichern" else "Mahlzeit speichern",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (foodPickerVisible) {
            FoodPickerOverlay(
                foods = filteredFoods,
                selectedFoods = selectedFoods.value,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onToggle = { food ->
                    selectedFoods.value = if (selectedFoods.value.contains(food)) {
                        selectedFoods.value - food
                    } else {
                        selectedFoods.value + food
                    }
                },
                onClose = {
                    foodPickerVisible = false
                    searchQuery = ""
                }
            )
        }
    }

    // Dialog for adding custom food
    if (showAddFoodDialog) {
        AlertDialog(
            onDismissRequest = { showAddFoodDialog = false },
            title = { Text("Neues Lebensmittel anlegen") },
            text = {
                Column {
                    Text(
                        text = "Füge ein persönliches Lebensmittel zu deiner Datenbank hinzu:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFoodName,
                        onValueChange = { newFoodName = it },
                        label = { Text("Name des Lebensmittels") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Kategorien (optional, Mehrfachauswahl)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FoodCategories.ALL.forEach { category ->
                            FilterChip(
                                selected = category.key in newFoodCategories,
                                onClick = {
                                    newFoodCategories = if (category.key in newFoodCategories) {
                                        newFoodCategories - category.key
                                    } else {
                                        newFoodCategories + category.key
                                    }
                                },
                                label = { Text(category.label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFoodName.isNotBlank()) {
                            val trimmed = newFoodName.trim()
                            val categories = FoodCategories.normalize(newFoodCategories)
                            viewModel.addCustomFood(
                                name = trimmed,
                                categories = categories
                            ) { newId ->
                                val addedFood = FoodEntity(
                                    id = newId,
                                    name = trimmed,
                                    category = "Eigene Lebensmittel",
                                    isCustom = true,
                                    categories = FoodCategories.encode(categories)
                                )
                                selectedFoods.value = selectedFoods.value + addedFood
                            }
                            newFoodName = ""
                            newFoodCategories = emptySet()
                            showAddFoodDialog = false
                        }
                    },
                    enabled = newFoodName.isNotBlank()
                ) {
                    Text("Hinzufügen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFoodDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Suggestion popup anchored above the IME.
 *
 * The outer box applies `imePadding()`, so the panel (and its search field)
 * always ends at the keyboard's top edge. The result list fills the remaining
 * height and scrolls. This works independently of screen size because the
 * panel uses a fraction of the *available* height, not a fixed pixel size.
 */
@Composable
private fun FoodPickerOverlay(
    foods: List<FoodEntity>,
    selectedFoods: Set<FoodEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggle: (FoodEntity) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler {
        keyboard?.hide()
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("food_picker_overlay")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                .clickable {
                    keyboard?.hide()
                    onClose()
                }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lebensmittel hinzufügen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedFoods.size} ausgewählt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            keyboard?.hide()
                            onClose()
                        },
                        modifier = Modifier.testTag("food_picker_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Auswahl schließen")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(focusRequester)
                        .testTag("food_search_field"),
                    placeholder = { Text("Z.B. Kaffee, Pizza, Tomaten, Brot...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Suchen")
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Löschen")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(top = 10.dp))

                if (foods.isEmpty()) {
                    Text(
                        text = "Kein Lebensmittel gefunden. Lege es über „Neues Lebensmittel“ an.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("food_picker_results"),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(foods, key = { it.id }) { food ->
                            val isSelected = selectedFoods.any { it.id == food.id }
                            val categoryLabels = FoodCategories.labels(
                                FoodCategories.decode(food.categories)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(food) }
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                                    .testTag("food_picker_item_${food.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val subtitle = if (categoryLabels.isEmpty()) {
                                        food.category
                                    } else {
                                        categoryLabels.joinToString(" · ")
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Ausgewählt",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Hinzufügen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Tippe Lebensmittel an, um sie hinzuzufügen oder zu entfernen. " +
                        "Die Auswahl bleibt beim Schließen erhalten.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitSuggestionSection(
    habits: List<DetectedHabit>,
    onApply: (DetectedHabit) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        habits.forEach { habit ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_suggestion"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = habit.foodNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${habit.occurrenceCount}× an ${habit.distinctDayCount} Tagen · " +
                            "meist gegen ${habit.typicalTimeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onApply(habit) },
                        modifier = Modifier.testTag("habit_apply_button")
                    ) {
                        Text("Als Mahlzeit übernehmen")
                    }
                }
            }
        }
    }
}
