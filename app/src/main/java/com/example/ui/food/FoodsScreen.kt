package com.example.ui.food

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.FoodCategories
import com.example.data.model.FoodEntity
import com.example.ui.RefluxViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodsScreen(
    viewModel: RefluxViewModel,
    onNavigateToHabits: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Alle") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newFoodName by remember { mutableStateOf("") }
    var newFoodCategories by remember { mutableStateOf(setOf<String>()) }
    var editingFood by remember { mutableStateOf<FoodEntity?>(null) }
    var editingCategories by remember { mutableStateOf(setOf<String>()) }

    val categories = remember(allFoods) {
        listOf("Alle") + allFoods.map { it.category }.distinct().sorted()
    }

    val displayedFoods = remember(allFoods, searchQuery, selectedCategory) {
        allFoods.filter { food ->
            val matchesCategory = (selectedCategory == "Alle" || food.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() || food.name.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    fun openEditor(food: FoodEntity) {
        editingFood = food
        editingCategories = FoodCategories.decode(food.categories).toSet()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_custom_food_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Lebensmittel hinzufügen")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("foods_screen")
        ) {
            // Header & Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Lebensmittel-Datenbank",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Neutrale Datenbank aller erfassbaren Lebensmittel. Kategorien sind " +
                        "manuell gepflegte Analysemerkmale, keine automatischen Trigger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onNavigateToHabits,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_habits_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Erkannte Gewohnheiten verwalten")
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Lebensmittel suchen...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Löschen")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Food-type filter chips (catalogue groups, not analysis categories)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // Food List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedFoods, key = { it.id }) { food ->
                    val analysisCategories = FoodCategories.labels(
                        FoodCategories.decode(food.categories)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openEditor(food) }
                            .testTag("food_row_${food.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = food.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (analysisCategories.isNotEmpty()) {
                                        Text(
                                            text = analysisCategories.joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (food.isCustom) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "Eigenes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            title = "Neues Lebensmittel",
            confirmLabel = "Speichern",
            name = newFoodName,
            onNameChange = { newFoodName = it },
            selected = newFoodCategories,
            onSelectedChange = { newFoodCategories = it },
            confirmEnabled = newFoodName.isNotBlank(),
            onDismiss = {
                showAddDialog = false
                newFoodName = ""
                newFoodCategories = emptySet()
            },
            onConfirm = {
                viewModel.addCustomFood(
                    name = newFoodName.trim(),
                    categories = FoodCategories.normalize(newFoodCategories)
                )
                showAddDialog = false
                newFoodName = ""
                newFoodCategories = emptySet()
            }
        )
    }

    editingFood?.let { food ->
        CategoryDialog(
            title = food.name,
            confirmLabel = "Kategorien speichern",
            name = null,
            onNameChange = {},
            selected = editingCategories,
            onSelectedChange = { editingCategories = it },
            confirmEnabled = true,
            onDismiss = { editingFood = null },
            onConfirm = {
                viewModel.updateFoodCategories(food.id, editingCategories.toList())
                editingFood = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(
    title: String,
    confirmLabel: String,
    name: String?,
    onNameChange: (String) -> Unit,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (name != null) {
                    Text(
                        text = "Name des Lebensmittels:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Text(
                        text = "Kategorien für die Auswertung (Mehrfachauswahl):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FoodCategories.ALL.forEach { category ->
                        FilterChip(
                            selected = category.key in selected,
                            onClick = {
                                onSelectedChange(
                                    if (category.key in selected) {
                                        selected - category.key
                                    } else {
                                        selected + category.key
                                    }
                                )
                            },
                            label = { Text(category.label) },
                            modifier = Modifier.testTag("food_category_${category.key}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
