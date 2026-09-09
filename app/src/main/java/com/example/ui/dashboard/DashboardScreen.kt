package com.example.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimelineItem
import com.example.data.model.TriggerLikelihood
import com.example.ui.RefluxViewModel
import com.example.ui.components.HighDensityCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TriggerBadge
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
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: RefluxViewModel,
    onNavigateToAddMeal: () -> Unit,
    onNavigateToAddSymptom: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineItems by viewModel.timelineItems.collectAsState()
    val triggerAnalysis by viewModel.triggerAnalysis.collectAsState()

    // Calculate today's stats
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val mealsToday = timelineItems.count { it is TimelineItem.MealItem && it.timestamp >= todayStart }
    val symptomsToday = timelineItems.count { it is TimelineItem.SymptomItem && it.timestamp >= todayStart }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // App Title & Header with Backup button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RefluxTrack",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = BotanicalForest
                    )
                    Text(
                        text = "Persönliche Trigger-Erkennung ohne pauschale Verbote",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = BotanicalTextSecondary
                    )
                }

                IconButton(
                    onClick = onNavigateToBackup,
                    modifier = Modifier.testTag("backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ImportExport,
                        contentDescription = "CSV Import & Export",
                        tint = BotanicalForest,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Quick Entry Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToAddMeal,
                    colors = ButtonDefaults.buttonColors(containerColor = BotanicalForest),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("log_meal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mahlzeit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onNavigateToAddSymptom,
                    colors = ButtonDefaults.buttonColors(containerColor = TriggerHighRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("log_symptom_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Symptom", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Today's Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HighDensityCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Heute gegessen",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BotanicalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$mealsToday Mahlzeiten",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BotanicalForest
                        )
                    }
                }

                HighDensityCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Symptome heute",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BotanicalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val symptomTextColor = if (symptomsToday > 0) TriggerHighRed else BotanicalForest
                        Text(
                            text = "$symptomsToday Ereignisse",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = symptomTextColor
                        )
                    }
                }
            }
        }

        // Top Triggers Section
        item {
            SectionHeader(
                title = "Persönliche Trigger-Tendenzen",
                subtitle = "Top Verdachtsmomente"
            )

            val suspiciousFoods = triggerAnalysis.filter {
                it.likelihood == TriggerLikelihood.HOCH || it.likelihood == TriggerLikelihood.MITTEL
            }.take(3)

            if (suspiciousFoods.isEmpty()) {
                HighDensityCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = BotanicalSage,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Noch keine auffälligen Trigger erkannt",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BotanicalTextPrimary
                            )
                            Text(
                                text = "Protokolliere Mahlzeiten und Beschwerden über 7–14 Tage für verlässliche Muster.",
                                fontSize = 11.sp,
                                color = BotanicalTextSecondary
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    suspiciousFoods.forEach { item ->
                        HighDensityCard(
                            modifier = Modifier.clickable { onNavigateToAnalysis() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.foodName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BotanicalTextPrimary
                                    )
                                    Text(
                                        text = "In ${item.timesTriggered} von ${item.timesConsumed} Mahlzeiten folgten Symptome",
                                        fontSize = 11.sp,
                                        color = BotanicalTextSecondary
                                    )
                                }
                                TriggerBadge(
                                    likelihood = item.likelihood,
                                    percentage = item.triggerPercentage
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Timeline Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Letzte Aktivitäten")
                Text(
                    text = "Alle ansehen",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BotanicalForest,
                    modifier = Modifier
                        .clickable { onNavigateToTimeline() }
                        .padding(4.dp)
                )
            }

            val recentItems = timelineItems.take(4)
            if (recentItems.isEmpty()) {
                HighDensityCard {
                    Text(
                        text = "Keine Aktivitäten aufgezeichnet. Starte mit 'Mahlzeit' oder 'Symptom'.",
                        fontSize = 12.sp,
                        color = BotanicalTextSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentItems.forEach { item ->
                        HighDensityCard(
                            modifier = Modifier.clickable { onNavigateToTimeline() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (icon, tint) = when (item) {
                                    is TimelineItem.MealItem -> Icons.Default.Restaurant to BotanicalForest
                                    is TimelineItem.SymptomItem -> Icons.Default.WarningAmber to TriggerHighRed
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = formatTime(item.timestamp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                val title = when (item) {
                                    is TimelineItem.MealItem -> "${item.mealWithFoods.meal.mealType.displayName} (${item.mealWithFoods.foods.size} Lebensmittel)"
                                    is TimelineItem.SymptomItem -> "${item.symptom.symptomType.displayName} (Stärke: ${item.symptom.intensity}/10)"
                                }
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    color = BotanicalTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Medical Disclaimer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HighDensityCard(
                backgroundColor = BotanicalSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Hinweis: RefluxTrack ist kein Medizinprodukt. Berechnete Zusammenhänge sind rein statistische Korrelationen der eigenen Protokolle und keine gesicherten medizinischen Diagnosen.",
                    fontSize = 11.sp,
                    color = BotanicalTextMuted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
