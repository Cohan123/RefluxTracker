package com.example.ui.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analysis.ActivityRisk
import com.example.analysis.AnalyticsSummary
import com.example.analysis.AnalyticsWindow
import com.example.analysis.BurdenTrend
import com.example.analysis.CorrelationAssessment
import com.example.analysis.DailyBurden
import com.example.analysis.DailySeverity
import com.example.analysis.FactorAssociation
import com.example.analysis.FactorKind
import com.example.analysis.FoodTriggerAnalysis
import com.example.analysis.MeasureEffectiveness
import com.example.analysis.SymptomCategory
import com.example.analysis.TimeOfDaySummary
import com.example.analysis.TriggerStatistic
import com.example.ui.RefluxViewModel
import com.example.ui.components.DataQualityBadge
import com.example.ui.components.MedicalDisclaimerCard
import com.example.util.DateTimeUtils
import java.util.Locale

@Composable
fun AnalysisScreen(
    viewModel: RefluxViewModel,
    modifier: Modifier = Modifier
) {
    val analyses by viewModel.triggerAnalyses.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val analyticsSummary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val selectedWindow by viewModel.selectedAnalyticsWindow.collectAsStateWithLifecycle()
    val summary = analyticsSummary

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
                    text = "Analyse",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Feste Zeiträume mit 30-Min.-bis-4-Std.-Fenster und Portionsgewichtung, " +
                        "darunter die symptomartspezifische Rangliste. Keine Kausalitätsbehauptung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Fixed-window analytics
        item {
            AnalyticsWindowSelector(
                selectedWindow = selectedWindow,
                onWindowSelected = { window -> viewModel.selectAnalyticsWindow(window) }
            )
        }

        item {
            if (summary == null) {
                AnalyticsHintCard("Die Auswertung wird berechnet …")
            } else {
                AnalyticsOverviewCard(summary = summary, window = selectedWindow)
            }
        }

        if (summary != null) {
            if (summary.insights.isNotEmpty()) {
                item { InsightsCard(insights = summary.insights) }
            }

            item {
                SectionHeader(
                    title = "Symptomverlauf / GERD-Burden",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item { BurdenTrendCard(summary = summary) }

            item {
                SectionHeader(
                    title = "Tageszeit-Heatmap",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item { TimeOfDayHeatmapCard(summary = summary) }

            item {
                SectionHeader(
                    title = "Kategorie-Korrelationen & Trigger-Matrix",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item { TriggerMatrixCard(summary = summary) }

            item {
                SectionHeader(
                    title = "Top-3-Trigger (${selectedWindow.label})",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (summary.topTriggers.isEmpty()) {
                item {
                    AnalyticsHintCard(
                        "Keine Mahlzeit mit Symptom im 30-Min.-bis-4-Std.-Fenster."
                    )
                }
            } else {
                items(
                    items = summary.topTriggers,
                    key = { "analytics-trigger-${it.itemName}" }
                ) { trigger ->
                    AnalyticsTriggerCard(trigger)
                }
            }

            if (summary.dailyAverageSeverity.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Ø Symptomstärke pro Tag",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(
                            items = summary.dailyAverageSeverity,
                            key = { it.dayStartMillis }
                        ) { day ->
                            DailySeverityCard(day)
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Häufigste Tätigkeiten",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (summary.topActivities.isEmpty()) {
                item {
                    AnalyticsHintCard("Keine Tätigkeiten im Zeitraum erfasst.")
                }
            } else {
                items(
                    items = summary.topActivities,
                    key = { "analytics-activity-${it.activityName}" }
                ) { activity ->
                    AnalyticsActivityCard(activity)
                }
            }

            item {
                SectionHeader(
                    title = "Wirksamste Maßnahmen",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (summary.topEffectiveMeasures.isEmpty()) {
                item {
                    AnalyticsHintCard(
                        if (summary.measures.isEmpty()) {
                            "Noch kein Maßnahmenvergleich. Erfasse Symptome mit und ohne Maßnahme, " +
                                "um Stärke und Dauer zu vergleichen."
                        } else {
                            "Keine Maßnahme zeigt bisher eine Verbesserung gegenüber Symptomen ohne Maßnahme."
                        }
                    )
                }
            } else {
                items(
                    items = summary.topEffectiveMeasures,
                    key = { "analytics-measure-${it.measureName}" }
                ) { measure ->
                    MeasureEffectivenessCard(measure)
                }
            }
        }

        item {
            SectionHeader(
                title = "Symptomartspezifische Trigger-Rangliste",
                color = MaterialTheme.colorScheme.onSurface
            )
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
                            text = "Noch keine auswertbaren Zusammenhänge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sobald ein Symptom im passenden Zeitfenster nach einer Mahlzeit auftritt, erscheint hier eine statistische Auswertung. Erfasse weiter Mahlzeiten und Symptome.",
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
                    if (item.symptomType != null) {
                        DetailRow(
                            label = "Symptomtyp:",
                            value = item.symptomType
                        )
                    }
                    DetailRow(
                        label = "Analysefenster:",
                        value = analysisWindowLabel(item)
                    )
                    DetailRow(
                        label = "Konsumereignisse:",
                        value = "${item.consumptionCount}x protokolliert"
                    )
                    DetailRow(
                        label = "Symptomereignisse danach:",
                        value = "${item.symptomEventsCount}x im Analysefenster"
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

private fun analysisWindowLabel(analysis: FoodTriggerAnalysis): String {
    val window = analysis.symptomType
        ?.let { SymptomCategory.forSymptomType(it).window() }
        ?: SymptomCategory.GENERAL.window()
    return formatWindow(window)
}

private fun formatWindow(window: SymptomCategory.Window): String {
    fun format(minutes: Long): String =
        if (minutes % 60 == 0L) "${minutes / 60} Std." else "$minutes Min."
    return "${format(window.startMinutes)} bis ${format(window.endMinutes)}"
}

@Composable
private fun AnalyticsWindowSelector(
    selectedWindow: AnalyticsWindow,
    onWindowSelected: (AnalyticsWindow) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("analytics_window_selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsWindow.entries.forEach { window ->
            FilterChip(
                selected = window == selectedWindow,
                onClick = { onWindowSelected(window) },
                label = { Text(window.label) },
                modifier = Modifier.testTag("analytics_window_${window.days}")
            )
        }
    }
}

@Composable
private fun AnalyticsOverviewCard(
    summary: AnalyticsSummary,
    window: AnalyticsWindow
) {
    val triggerCount = summary.triggers.count { it.symptomMealCount > 0 }
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
                    text = "${window.label}-Auswertung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$triggerCount Trigger",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetric(
                    label = "Mahlzeiten",
                    value = summary.mealCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetric(
                    label = "Symptome",
                    value = summary.symptomCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetric(
                    label = "Ø Stärke",
                    value = formatOneDecimal(summary.averageSymptomSeverity),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetric(
                    label = "Ø Dauer",
                    value = formatMinutes(summary.averageSymptomDurationMinutes),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetric(
                    label = "Symptomtage",
                    value = summary.dailyAverageSeverity.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetric(
                    label = "Fenster",
                    value = "30–240 Min.",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "30 Min. bis 4 Std. nach der Mahlzeit; Portionsgröße als Gewicht.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AnalyticsTriggerCard(trigger: TriggerStatistic) {
    val isHigh = trigger.score >= 60
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (isHigh) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${trigger.score}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isHigh) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trigger.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatPercent(trigger.triggerProbability)} gewichtet · " +
                        "${formatPercent(trigger.rawTriggerProbability)} roh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${trigger.symptomMealCount} von ${trigger.consumptionCount} Mahlzeiten · " +
                        "${trigger.matchingSymptomCount} Symptome",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ø Latenz ${formatMinutes(trigger.averageLatencyMinutes)} · " +
                        "Ø Stärke ${formatOneDecimal(trigger.averageSymptomSeverity)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                DataQualityBadge(quality = trigger.dataQuality)
            }
        }
    }
}

@Composable
private fun DailySeverityCard(day: DailySeverity) {
    Card(
        modifier = Modifier.width(112.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = DateTimeUtils.formatShortDate(day.dayStartMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format(Locale.GERMAN, "%.1f", day.averageSeverity),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${day.symptomCount} Sympt.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsActivityCard(activity: ActivityRisk) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.activityName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activity.symptomCount} Symptome · " +
                        "${formatPercent(activity.shareOfAllSymptoms)} aller Symptome",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Ø ${formatOneDecimal(activity.averageSeverity)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatMinutes(activity.averageDurationMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MeasureEffectivenessCard(measure: MeasureEffectiveness) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = measure.measureName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (measure.isReliable) {
                            "Belastbare Datenbasis"
                        } else {
                            "Vorläufige Datenbasis"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (measure.isReliable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Effekt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = measure.effectScore
                            ?.let { String.format(Locale.GERMAN, "%+.0f", it) }
                            ?: "–",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mit Maßnahme: ${measure.withMeasureCount}× · " +
                    "Ø ${formatOneDecimal(measure.withAverageSeverity)} · " +
                    formatMinutes(measure.withAverageDurationMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ohne Maßnahme: ${measure.withoutMeasureCount}× · " +
                    "Ø ${formatOneDecimal(measure.withoutAverageSeverity)} · " +
                    formatMinutes(measure.withoutAverageDurationMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Δ Stärke ${formatSignedOne(measure.severityDelta)} · " +
                    "Δ Dauer ${formatSignedMinutes(measure.durationDeltaMinutes)} " +
                    "(negativ = besser)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsHintCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatPercent(value: Double): String =
    String.format(Locale.GERMAN, "%.0f %%", value * 100.0)

private fun formatOneDecimal(value: Double?): String =
    value?.let { String.format(Locale.GERMAN, "%.1f", it) } ?: "–"

private fun formatMinutes(value: Double?): String =
    value?.let { String.format(Locale.GERMAN, "%.0f Min.", it) } ?: "–"

private fun formatSignedOne(value: Double?): String =
    value?.let { String.format(Locale.GERMAN, "%+.1f", it) } ?: "–"

private fun formatSignedMinutes(value: Double?): String =
    value?.let { String.format(Locale.GERMAN, "%+.0f Min.", it) } ?: "–"

@Composable
private fun InsightsCard(insights: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_insights"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Erkenntnisse aus deinen Daten",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            insights.forEach { insight ->
                Text(
                    text = "• $insight",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BurdenTrendCard(summary: AnalyticsSummary) {
    val days = summary.dailyBurden
    val trend = summary.trend
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val missingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val trendColor = MaterialTheme.colorScheme.tertiary
    val spikeColor = MaterialTheme.colorScheme.error
    val freeColor = Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Täglicher GERD-Burden (0–100)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "60 % Intensität + 40 % Häufigkeit (max. 4 Symptome/Tag). " +
                    "Lücken bedeuten fehlende Einträge, nicht Symptomfreiheit.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (days.isEmpty()) {
                Text(
                    text = "Keine Tagesdaten im Zeitraum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("100", style = MaterialTheme.typography.labelSmall)
                    Text("50", style = MaterialTheme.typography.labelSmall)
                    Text("0", style = MaterialTheme.typography.labelSmall)
                }
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("burden_trend_chart")
                ) {
                    val width = size.width
                    val height = size.height
                    val count = days.size
                    fun x(index: Int): Float =
                        if (count <= 1) width / 2f else width * index / (count - 1).toFloat()
                    fun y(value: Double): Float =
                        (height - (value.coerceIn(0.0, 100.0) / 100.0 * height)).toFloat()

                    listOf(0, 25, 50, 75, 100).forEach { value ->
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y(value.toDouble())),
                            end = Offset(width, y(value.toDouble())),
                            strokeWidth = 1f
                        )
                    }

                    var previous: Offset? = null
                    days.forEachIndexed { index, day ->
                        val burden = day.burden
                        val px = x(index)
                        if (burden == null) {
                            // A visible gap: missing data is never drawn as 0.
                            drawCircle(
                                color = missingColor,
                                radius = 1.8f,
                                center = Offset(px, height - 2f)
                            )
                            previous = null
                            return@forEachIndexed
                        }

                        val point = Offset(px, y(burden))
                        previous?.let { start ->
                            drawLine(
                                color = lineColor,
                                start = start,
                                end = point,
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        if (day.isSymptomFree) {
                            drawCircle(
                                color = freeColor,
                                radius = 3.2f,
                                center = Offset(px, height - 3f)
                            )
                        }
                        if (day.isSpike) {
                            drawCircle(color = spikeColor, radius = 5f, center = point)
                            drawCircle(color = Color.White, radius = 2f, center = point)
                        } else {
                            drawCircle(color = lineColor, radius = 2.4f, center = point)
                        }
                        previous = point
                    }

                    val fitStart = trend.fittedStartBurden
                    val fitEnd = trend.fittedEndBurden
                    val fitStartDay = trend.fittedStartDayStartMillis
                    val fitEndDay = trend.fittedEndDayStartMillis
                    if (fitStart != null && fitEnd != null && fitStartDay != null && fitEndDay != null) {
                        val startIndex = days.indexOfFirst { it.dayStartMillis == fitStartDay }
                        val endIndex = days.indexOfFirst { it.dayStartMillis == fitEndDay }
                        if (startIndex >= 0 && endIndex >= 0) {
                            drawLine(
                                color = trendColor,
                                start = Offset(x(startIndex), y(fitStart)),
                                end = Offset(x(endIndex), y(fitEnd)),
                                strokeWidth = 1.8f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = DateTimeUtils.formatShortDate(days.first().dayStartMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DateTimeUtils.formatShortDate(days.last().dayStartMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LegendDot(color = lineColor, label = "Burden")
                LegendDot(color = freeColor, label = "symptomfrei")
                LegendDot(color = spikeColor, label = "Spitze")
                LegendDot(color = trendColor, label = "Trend")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${trend.documentedDayCount} Tage mit Einträgen · " +
                    "${trend.symptomFreeDayCount} symptomfrei · " +
                    "${trend.missingDayCount} ohne Einträge · " +
                    "${trend.spikeDayStarts.size} Spitzen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val change = trend.changePercent
            if (change != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(
                        Locale.GERMAN,
                        "1. Hälfte Ø %.1f · 2. Hälfte Ø %.1f · Veränderung %+.0f %% " +
                            "(negativ = Besserung)",
                        trend.firstHalfAverageBurden ?: 0.0,
                        trend.secondHalfAverageBurden ?: 0.0,
                        change
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Für einen Halbzeit-Vergleich fehlen dokumentierte Tage.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimeOfDayHeatmapCard(summary: AnalyticsSummary) {
    val timeOfDay = summary.timeOfDay
    val maxWeight = timeOfDay.buckets.maxOfOrNull { it.intensitySum } ?: 0
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_of_day_heatmap"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Symptome nach Tageszeit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Farbe = Häufigkeit und Intensität; Zahl = erfasste Symptome pro Stunde.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            for (row in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (column in 0 until 6) {
                        val hour = row * 6 + column
                        val bucket = timeOfDay.buckets[hour]
                        val alpha = if (bucket.symptomCount == 0 || maxWeight == 0) {
                            0.06f
                        } else {
                            (0.16f + 0.74f * (bucket.intensitySum.toFloat() / maxWeight))
                                .coerceIn(0.06f, 0.9f)
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = primary.copy(alpha = alpha),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format(Locale.GERMAN, "%02d", hour),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bucket.symptomCount.toString(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (timeOfDay.insight != null) {
                Text(
                    text = timeOfDay.insight,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Für eine belastbare Tageszeit-Aussage fehlen noch mindestens " +
                        "3 passende Beobachtungen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Stärkstes 5-Stunden-Fenster: " +
                    String.format(
                        Locale.GERMAN,
                        "%02d:00–%02d:00 Uhr",
                        timeOfDay.peakStartHour,
                        timeOfDay.peakEndHourExclusive
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TriggerMatrixCard(summary: AnalyticsSummary) {
    val factors = summary.factorAssociations

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trigger_matrix"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Symptom-Wahrscheinlichkeit mit und ohne Faktor",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vergleich je Mahlzeit im 30-Min.–4-Std.-Fenster. Relative Veränderung " +
                    "nur ab 3 Mahlzeiten je Gruppe und mindestens 5 % Basiswert.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (factors.isEmpty()) {
                Text(
                    text = "Noch kein Faktor mit Vergleichsgruppe vorhanden. Kategorisiere " +
                        "Lebensmittel oder erfasse mehr Mahlzeiten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Faktor",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Veränderung",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                factors.forEachIndexed { index, factor ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    FactorRow(factor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dargestellt ist ein beobachteter Zusammenhang, keine Kausalität. " +
                    "Fehlende Kategorien zählen weder positiv noch negativ.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (summary.unavailableFactors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Nicht auswertbar, weil nicht erfasst: " +
                        summary.unavailableFactors.joinToString(", ") + ".",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FactorRow(factor: FactorAssociation) {
    val relative = factor.relativeChangePercent
    val changeColor = when {
        relative == null -> MaterialTheme.colorScheme.onSurfaceVariant
        relative > 0 -> MaterialTheme.colorScheme.error
        relative < 0 -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factor.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (factor.kind == FactorKind.CATEGORY) {
                        "Lebensmittel-Kategorie"
                    } else {
                        "Verhaltensfaktor"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = relative?.let {
                        String.format(Locale.GERMAN, "%+.0f %%", it)
                    } ?: "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
                Text(
                    text = String.format(
                        Locale.GERMAN,
                        "%+.0f Prozentpunkte",
                        factor.absoluteDifferencePercentagePoints
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Mit: ${formatPercent(factor.probabilityWith)} " +
                "(${factor.exposedSymptomMealCount}/${factor.exposedMealCount}) · " +
                "Ohne: ${formatPercent(factor.probabilityWithout)} " +
                "(${factor.unexposedSymptomMealCount}/${factor.unexposedMealCount})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DataQualityBadge(quality = factor.dataQuality)
            if (!factor.isReliable) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "vorläufig",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                    if (analysis.symptomType != null) {
                        Text(
                            text = "→ ${analysis.symptomType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
