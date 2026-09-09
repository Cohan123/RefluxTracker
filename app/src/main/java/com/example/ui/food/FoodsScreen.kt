package com.example.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RefluxViewModel
import com.example.ui.components.HighDensityCard
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import com.example.ui.theme.TriggerHighRed

@Composable
fun FoodsScreen(
    viewModel: RefluxViewModel,
    modifier: Modifier = Modifier
) {
    val foods by viewModel.allFoods.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var newFoodName by remember { mutableStateOf("") }
    var newFoodCategory by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    val filtered = remember(foods, searchQuery) {
        if (searchQuery.isBlank()) foods
        else foods.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lebensmittel-Bibliothek",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = BotanicalTextPrimary
                )
                Text(
                    text = "${foods.size} gespeicherte Nahrungsmittel",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = BotanicalTextSecondary
                )
            }

            Button(
                onClick = { isAddingNew = !isAddingNew },
                colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("toggle_add_food_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(if (isAddingNew) "Schließen" else "Neu", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isAddingNew) {
            HighDensityCard(modifier = Modifier.padding(bottom = 10.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Neues Lebensmittel anlegen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFoodName,
                        onValueChange = { newFoodName = it },
                        placeholder = { Text("Name (z.B. Espresso, Dinkelbrot)", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BotanicalSurface,
                            unfocusedContainerColor = BotanicalSurface,
                            focusedBorderColor = BotanicalForest,
                            unfocusedBorderColor = BotanicalCardBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newFoodCategory,
                        onValueChange = { newFoodCategory = it },
                        placeholder = { Text("Kategorie (z.B. Getränke, Obst)", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BotanicalSurface,
                            unfocusedContainerColor = BotanicalSurface,
                            focusedBorderColor = BotanicalForest,
                            unfocusedBorderColor = BotanicalCardBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newFoodName.isNotBlank()) {
                                viewModel.addNewFood(
                                    name = newFoodName.trim(),
                                    category = if (newFoodCategory.isNotBlank()) newFoodCategory.trim() else "Allgemein"
                                )
                                newFoodName = ""
                                newFoodCategory = ""
                                isAddingNew = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("In Bibliothek aufnehmen", fontSize = 13.sp)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Lebensmittel suchen...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = BotanicalTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BotanicalSurface,
                unfocusedContainerColor = BotanicalSurface,
                focusedBorderColor = BotanicalForest,
                unfocusedBorderColor = BotanicalCardBorder
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id }) { food ->
                HighDensityCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = food.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BotanicalTextPrimary
                            )
                            Text(
                                text = food.category,
                                fontSize = 11.sp,
                                color = BotanicalTextSecondary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteFood(food) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Löschen",
                                tint = TriggerHighRed.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
