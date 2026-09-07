package com.example.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analysis.CorrelationAssessment
import com.example.analysis.FoodTriggerAnalysis
import com.example.ui.RefluxViewModel
import com.example.ui.components.DataQualityBadge
import com.example.ui.components.MedicalDisclaimerCard

@Composable
fun AnalysisScreen(
    viewModel: RefluxViewModel,
    modifier: Modifier = Modifier
) {
    val analyses by viewModel.triggerAnalyses.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()

    var selectedDetail by remember { mutableStateOf<FoodTriggerAnalysis?>(null) }

    val probableTriggers = remember(analyses) {
        analyses.filter { it.assessment == CorrelationAssessment.PROBABLE }
    }
    val possibleTriggers = remember(analyses) {
        analyses.filter { it.assessment == CorrelationAssessment.POSSIBLE }
    }
    val unremarkable = remember(analyses) {
        analyses.filter { it.assessment == CorrelationAssessment.UNREMARKABLE }
    }
    val insufficient = remember(analyses) {
        analyses.filter { it.assessment == CorrelationAssessment.INSUFFICIENT_DATA }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analysis_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Trigger-Rangliste",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Statistische Korrelation zwischen Lebensmitteln und nachfolgenden Symptomen im Analysefenster (30 Min. bis 4 Std.). Keine Kausalitätsbehauptung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 14-Day Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "14-Tage-Analysephase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tag ${dashboardState.currentDayOf14} / 14",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val progress = (dashboardState.currentDayOf14 / 14f).coerceIn(0.05f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (dashboardState.currentDayOf14 >= 14) {
                            "Deine erste persönliche Triggeranalyse ist verfügbar. Die Ergebnisse basieren auf deinem individuellen Erfassungszeitraum."
                        } else {
                            "Vorläufige Zusammenhänge werden bereits dargestellt. Je mehr Tage du protokollierst, desto fundierter wird deine persönliche Rangliste."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section: Wahrscheinliche Trigger
        if (probableTriggers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Wahrscheinliche Trigger (${probableTriggers.size})",
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(probableTriggers) { item ->
                TriggerCard(
                    analysis = item,
                    onClick = { selectedDetail = item }
                )
            }
        }

        // Section: Mögliche Trigger
        if (possibleTriggers.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Mögliche Trigger (${possibleTriggers.size})",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            items(possibleTriggers) { item ->
                TriggerCard(
                    analysis = item,
                    onClick = { selectedDetail = item }
                )
            }
        }

        // Section: Keine auffälligen Zusammenhänge
        if (unremarkable.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Keine auffälligen Zusammenhänge (${unremarkable.size})",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(unremarkable) { item ->
                TriggerCard(
                    analysis = item,
                    onClick = { selectedDetail = item }
                )
            }
        }

        // Section: Zu wenige Daten
        if (insufficient.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Noch zu wenige Daten (${insufficient.size})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(insufficient) { item ->
                TriggerCard(
                    analysis = item,
                    onClick = { selectedDetail = item }
                )
            }
        }

        // If no foods logged at all
        if (analyses.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Noch keine Daten für Triggeranalyse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Beginne mit dem Erfassen von Mahlzeiten und auftretenden Symptomen, damit deine persönliche statistische Rangliste berechnet werden kann.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            MedicalDisclaimerCard()
        }
    }

    // Detail Dialog when clicking a trigger
    selectedDetail?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedDetail = null },
            title = {
                Column {
                    Text(
                        text = item.foodName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DataQualityBadge(quality = item.dataQuality)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(
                        label = "Trigger-Score:",
                        value = "${item.score} %",
                        isHighlight = true
                    )
                    DetailRow(
                        label = "Statistische Bewertung:",
                        value = item.assessment.label
                    )
                    HorizontalDivider()
                    DetailRow(
                        label = "Konsumereignisse:",
                        value = "${item.consumptionCount}x protokolliert"
                    )
                    DetailRow(
                        label = "Symptomereignisse danach:",
                        value = "${item.symptomEventsCount}x innerhalb 4 Std."
                    )
                    DetailRow(
                        label = "Ø Symptomstärke nach Konsum:",
                        value = if (item.symptomEventsCount > 0) String.format("%.1f / 10", item.averageSymptomIntensityAfter) else "Keine"
                    )
                    DetailRow(
                        label = "Persönlicher Basiswert:",
                        value = if (item.baselineSymptomIntensity > 0) String.format("%.1f / 10", item.baselineSymptomIntensity) else "-"
                    )
                    if (item.averageDelayMinutes > 0) {
                        DetailRow(
                            label = "Ø Zeit bis Symptom:",
                            value = "${item.averageDelayMinutes.toInt()} Minuten"
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hinweis: Dieser statistische Wert beschreibt eine Korrelation in deinen Daten und keine ärztlich nachgewiesene Kausalität.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDetail = null }) {
                    Text("Schließen")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun TriggerCard(
    analysis: FoodTriggerAnalysis,
    onClick: () -> Unit
) {
    val isHigh = analysis.score >= 60
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isHigh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${analysis.score}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isHigh) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = analysis.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${analysis.symptomEventsCount} von ${analysis.consumptionCount} Mal Symptome",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DataQualityBadge(quality = analysis.dataQuality)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${analysis.consumptionCount}x",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = analysis.assessment.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
