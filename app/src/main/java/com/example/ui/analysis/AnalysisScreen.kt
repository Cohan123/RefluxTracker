package com.example.ui.analysis

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodTriggerAnalysis
import com.example.data.model.TriggerLikelihood
import com.example.ui.RefluxViewModel
import com.example.ui.components.HighDensityCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TriggerBadge
import com.example.ui.meal.SelectableChip
import com.example.ui.theme.BotanicalCardBorder
import com.example.ui.theme.BotanicalForest
import com.example.ui.theme.BotanicalMint
import com.example.ui.theme.BotanicalSage
import com.example.ui.theme.BotanicalSurface
import com.example.ui.theme.BotanicalTextMuted
import com.example.ui.theme.BotanicalTextPrimary
import com.example.ui.theme.BotanicalTextSecondary
import com.example.ui.theme.TriggerHighRed
import com.example.ui.theme.TriggerLowGreen
import com.example.ui.theme.TriggerMediumAmber

@Composable
fun AnalysisScreen(
    viewModel: RefluxViewModel,
    modifier: Modifier = Modifier
) {
    val analysisList by viewModel.triggerAnalysis.collectAsState()
    val selectedDays by viewModel.analysisDays.collectAsState()

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
                    text = "Trigger-Analyse",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = BotanicalTextPrimary
                )
                Text(
                    text = "Statistische Auswertung deiner persönlichen Daten",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = BotanicalTextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.refreshAnalysis() },
                modifier = Modifier.testTag("refresh_analysis_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Neu berechnen",
                    tint = BotanicalForest
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timeframe selector (7d, 14d, 30d, all)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(7 to "7 Tage", 14 to "14 Tage", 30 to "30 Tage", 0 to "Gesamt").forEach { (days, label) ->
                SelectableChip(
                    label = label,
                    selected = selectedDays == days,
                    onClick = { viewModel.setAnalysisDays(days) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (analysisList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = BotanicalMint,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Noch nicht genügend Daten",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BotanicalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trage Mahlzeiten und Symptome über mehrere Tage ein. Die App analysiert automatisch Zeitabstände von 15 Minuten bis 4 Stunden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BotanicalTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Rangliste möglicher Trigger",
                        subtitle = "Sortiert nach Häufigkeit & Symptomrate"
                    )
                }

                items(analysisList, key = { it.foodId }) { item ->
                    TriggerAnalysisCard(item = item)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Methodological Disclaimer Card
                    HighDensityCard(backgroundColor = BotanicalSurface) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BotanicalSage,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Statistischer Hinweis: Eine zeitliche Korrelation ist kein Beweis für eine Kausalität. Auch Kombinationen, späte Uhrzeiten oder Stress können eine Rolle spielen. Nutze die Daten als Orientierung für ein ärztliches oder ernährungswissenschaftliches Gespräch.",
                                fontSize = 11.sp,
                                color = BotanicalTextMuted,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TriggerAnalysisCard(
    item: FoodTriggerAnalysis,
    modifier: Modifier = Modifier
) {
    HighDensityCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Name & Likelihood badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.foodName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalTextPrimary
                    )
                    Text(
                        text = item.category,
                        fontSize = 11.sp,
                        color = BotanicalTextSecondary
                    )
                }

                TriggerBadge(
                    likelihood = item.likelihood,
                    percentage = item.triggerPercentage
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage Bar
            val progressColor = when (item.likelihood) {
                TriggerLikelihood.HOCH -> TriggerHighRed
                TriggerLikelihood.MITTEL -> TriggerMediumAmber
                TriggerLikelihood.GERING -> TriggerLowGreen
                TriggerLikelihood.UNBEKANNT -> BotanicalMint
            }

            LinearProgressIndicator(
                progress = { item.triggerPercentage / 100f },
                color = progressColor,
                trackColor = BotanicalCardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "Konsumiert",
                    value = "${item.timesConsumed}x"
                )
                MetricColumn(
                    label = "Symptome danach",
                    value = "${item.timesTriggered}x (${item.triggerPercentage}%)"
                )
                if (item.avgDelayMinutes > 0) {
                    MetricColumn(
                        label = "Ø Abstand",
                        value = "${item.avgDelayMinutes} Min."
                    )
                }
                if (item.avgIntensity > 0) {
                    MetricColumn(
                        label = "Ø Stärke",
                        value = "%.1f/10".format(item.avgIntensity)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            color = BotanicalTextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BotanicalTextPrimary
        )
    }
}
