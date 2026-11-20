package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analysis.AnalyticsSummary
import com.example.analysis.AnalyticsWindow
import com.example.analysis.DetectedHabit
import com.example.analysis.FoodTriggerAnalysis
import com.example.analysis.HabitDetector
import com.example.analysis.RefluxAnalyticsService
import com.example.analysis.TriggerAnalysisEngine
import com.example.data.local.AppDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.FoodEntity
import com.example.data.model.MealWithFoods
import com.example.data.model.RemedyEntity
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomWithDetails
import com.example.data.repository.RefluxRepository
import com.example.data.repository.TimelineEntry
import com.example.importer.CsvImportSummary
import com.example.importer.RefluxDataImport
import com.example.report.DoctorReportBuilder
import com.example.report.DoctorReportPdf
import com.example.report.ReportSettings
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val todayMealsCount: Int = 0,
    val todaySymptomsCount: Int = 0,
    val todayAvgIntensity: Double? = null,
    val currentDayOf14: Int = 1,
    val recentTimeline: List<TimelineEntry> = emptyList(),
    val topTriggers: List<FoodTriggerAnalysis> = emptyList(),
    val totalObservations: Int = 0
)

class RefluxViewModel(
    application: Application,
    private val repository: RefluxRepository
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            repository.ensureDefaultFoods()
            repository.ensureDefaultCatalogs()
        }
    }

    val allFoods: StateFlow<List<FoodEntity>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRemedies: StateFlow<List<RemedyEntity>> = repository.allRemedies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeals: StateFlow<List<MealWithFoods>> = repository.allMealsWithFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSymptoms: StateFlow<List<SymptomEntity>> = repository.allSymptoms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSymptomsWithDetails: StateFlow<List<SymptomWithDetails>> =
        repository.allSymptomsWithDetails
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Locally detected recurring meals; recomputed from the meal history. */
    val habitOverrides = repository.allHabitOverrides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<DetectedHabit>> = combine(
        allMeals,
        habitOverrides
    ) { meals, overrides ->
        HabitDetector.detect(meals, overrides)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAnalyticsWindow = MutableStateFlow(AnalyticsWindow.SEVEN_DAYS)
    val selectedAnalyticsWindow: StateFlow<AnalyticsWindow> = _selectedAnalyticsWindow.asStateFlow()

    fun selectAnalyticsWindow(window: AnalyticsWindow) {
        _selectedAnalyticsWindow.value = window
    }

    private val reportSettings = ReportSettings(application)

    private val _doctorReportUserName = MutableStateFlow(reportSettings.userName().orEmpty())

    /** Optional display name for the PDF header; persisted locally. */
    val doctorReportUserName: StateFlow<String> = _doctorReportUserName.asStateFlow()

    fun setDoctorReportUserName(name: String) {
        _doctorReportUserName.value = name.trim()
        reportSettings.setUserName(name)
    }

    /**
     * Fixed-window analytics (30 min–4 h after a meal, portion-weighted).
     * Computed off the main thread and refreshed when meals, symptoms or the
     * selected reporting window change.
     */
    val analyticsSummary: StateFlow<AnalyticsSummary?> = combine(
        allMeals,
        allSymptomsWithDetails,
        _selectedAnalyticsWindow
    ) { meals, symptoms, window ->
        RefluxAnalyticsService.getAnalyticsSummary(
            meals = meals,
            symptoms = symptoms,
            timeWindowDays = window.days
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selected day offset: 0 for today, -1 for yesterday, etc.
    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset: StateFlow<Int> = _selectedDayOffset.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val timelineForSelectedDay: StateFlow<List<TimelineEntry>> = _selectedDayOffset
        .flatMapLatest { offset ->
            val (startMs, endMs) = DateTimeUtils.getStartAndEndOfDay(offset)
            repository.getDayTimeline(startMs, endMs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trigger analysis results derived from all meals and symptoms
    val triggerAnalyses: StateFlow<List<FoodTriggerAnalysis>> = combine(allMeals, allSymptoms) { meals, symptoms ->
        TriggerAnalysisEngine.analyze(meals, symptoms)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard State
    val dashboardState: StateFlow<DashboardUiState> = combine(
        allMeals,
        allSymptoms,
        triggerAnalyses
    ) { meals, symptoms, analyses ->
        val (todayStart, todayEnd) = DateTimeUtils.getStartAndEndOfDay(0)

        val todayMeals = meals.filter { it.meal.timestamp in todayStart..todayEnd }
        val todaySymptoms = symptoms.filter { it.timestamp in todayStart..todayEnd }

        val todayAvg = if (todaySymptoms.isNotEmpty()) {
            todaySymptoms.map { it.intensity }.average()
        } else {
            null
        }

        // Calculate 14-day tracking progress
        val earliestTs = listOfNotNull(
            meals.minOfOrNull { it.meal.timestamp },
            symptoms.minOfOrNull { it.timestamp }
        ).minOrNull()

        val dayOf14 = if (earliestTs != null) {
            DateTimeUtils.calculateDaysBetween(earliestTs, System.currentTimeMillis()).coerceIn(1, 14)
        } else {
            1
        }

        // Today's recent timeline preview
        val recentTimeline = (todayMeals.map { TimelineEntry.Meal(it) } +
                todaySymptoms.map { TimelineEntry.Symptom(it) })
            .sortedByDescending { it.timestamp }
            .take(5)

        // Top triggers (those with score > 0, limited to 3 for preview)
        val topTriggers = analyses.filter { it.score > 0 }.take(3)

        DashboardUiState(
            todayMealsCount = todayMeals.size,
            todaySymptomsCount = todaySymptoms.size,
            todayAvgIntensity = todayAvg,
            currentDayOf14 = dayOf14,
            recentTimeline = recentTimeline,
            topTriggers = topTriggers,
            totalObservations = meals.size + symptoms.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun setSelectedDayOffset(offset: Int) {
        _selectedDayOffset.value = offset
    }

    fun addMeal(
        timestamp: Long,
        mealType: String,
        portion: String,
        notes: String,
        foodIds: List<Long>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addMeal(timestamp, mealType, portion, notes, foodIds)
            // A retroactive entry must be visible immediately: the day view
            // follows the saved meal's local date. Existing entries are not
            // touched - only the selected day changes.
            _selectedDayOffset.value = DateTimeUtils.localDayOffset(timestamp)
            onSuccess()
        }
    }

    fun updateMeal(
        mealId: Long,
        timestamp: Long,
        mealType: String,
        portion: String,
        notes: String,
        foodIds: List<Long>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = repository.updateMeal(
                mealId = mealId,
                timestamp = timestamp,
                mealType = mealType,
                portion = portion,
                notes = notes,
                foodIds = foodIds
            )
            if (updated) {
                _selectedDayOffset.value = DateTimeUtils.localDayOffset(timestamp)
                onSuccess()
            }
        }
    }

    fun getMealDetails(mealId: Long, onLoaded: (MealWithFoods?) -> Unit) {
        viewModelScope.launch {
            onLoaded(repository.getMealDetails(mealId))
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
        }
    }

    fun addSymptom(
        timestamp: Long,
        symptomType: String,
        intensity: Int,
        durationMinutes: Int?,
        notes: String,
        symptomTypes: List<String> = emptyList(),
        activityIds: List<Long> = emptyList(),
        remedyIds: List<Long> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addSymptom(
                timestamp = timestamp,
                symptomType = symptomType,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes,
                symptomTypes = symptomTypes,
                activityIds = activityIds,
                remedyIds = remedyIds
            )
            onSuccess()
        }
    }

    fun addCustomActivity(name: String, onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onAdded(repository.insertCustomActivity(name))
        }
    }

    fun addCustomRemedy(name: String, onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onAdded(repository.insertCustomRemedy(name))
        }
    }

    fun updateSymptom(
        symptomId: Long,
        timestamp: Long,
        symptomType: String,
        intensity: Int,
        durationMinutes: Int?,
        notes: String,
        symptomTypes: List<String> = emptyList(),
        activityIds: List<Long> = emptyList(),
        remedyIds: List<Long> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateSymptom(
                symptomId = symptomId,
                timestamp = timestamp,
                symptomType = symptomType,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes,
                symptomTypes = symptomTypes,
                activityIds = activityIds,
                remedyIds = remedyIds
            )
            onSuccess()
        }
    }

    fun getSymptomDetails(symptomId: Long, onLoaded: (SymptomWithDetails?) -> Unit) {
        viewModelScope.launch {
            onLoaded(repository.getSymptomDetails(symptomId))
        }
    }

    fun deleteSymptom(symptomId: Long) {
        viewModelScope.launch {
            repository.deleteSymptom(symptomId)
        }
    }

    fun addCustomFood(
        name: String,
        category: String = "Eigene Lebensmittel",
        categories: List<String> = emptyList(),
        onAdded: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.insertCustomFood(name, category, categories)
            onAdded(id)
        }
    }

    fun updateFoodCategories(foodId: Long, categories: List<String>) {
        viewModelScope.launch {
            repository.updateFoodCategories(foodId, categories)
        }
    }

    fun saveHabitOverride(
        signature: String,
        customName: String?,
        customFoods: List<String>?,
        isDeleted: Boolean = false
    ) {
        viewModelScope.launch {
            repository.saveHabitOverride(signature, customName, customFoods, isDeleted)
        }
    }

    fun deleteHabit(signature: String) {
        saveHabitOverride(
            signature = signature,
            customName = null,
            customFoods = null,
            isDeleted = true
        )
    }

    /**
     * Export all locally-stored meals and symptoms as a CSV file in the
     * app's cache directory and invoke [onReady] with the resulting File.
     * The caller is responsible for wrapping it in a FileProvider URI and
     * launching the system share sheet.
     */
    fun exportCsv(
        context: android.content.Context,
        onReady: (java.io.File) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val file = com.example.export.RefluxDataExport.exportToCacheFile(
                    context.applicationContext,
                    repository
                )
                onReady(file)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    /**
     * Reads and imports the CSV/HTML backup selected by the user. The actual
     * write happens atomically in [RefluxRepository.importEntries]; the result
     * contains the number of imported/skipped entries for the UI summary.
     */
    fun importCsv(
        context: android.content.Context,
        uri: Uri,
        onComplete: (CsvImportSummary) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    RefluxDataImport.importFromUri(
                        context = context.applicationContext,
                        repository = repository,
                        uri = uri
                    )
                }
                onComplete(summary)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    /**
     * Generates the doctor report PDF for the currently selected analytics
     * window. All values come from the documented entries; missing data is
     * rendered as such. The file is written to `cacheDir/exports/`; the caller
     * is responsible for saving or sharing it via a FileProvider URI.
     */
    fun createDoctorReport(
        context: android.content.Context,
        onReady: (java.io.File) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                onReady(buildDoctorReportPdf(context))
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    /**
     * Generates the report and writes it directly to the document URI chosen by
     * the user (for example via `ActivityResultContracts.CreateDocument`).
     */
    fun saveDoctorReportTo(
        context: android.content.Context,
        uri: Uri,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val file = buildDoctorReportPdf(context)
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("Ziel-Datei konnte nicht geöffnet werden")
                    output.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                }
                onComplete()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    private suspend fun buildDoctorReportPdf(context: android.content.Context): java.io.File {
        val meals = repository.allMealsWithFoods.first()
        val symptoms = repository.allSymptomsWithDetails.first()
        val window = _selectedAnalyticsWindow.value
        val summary = withContext(Dispatchers.Default) {
            RefluxAnalyticsService.getAnalyticsSummary(
                meals = meals,
                symptoms = symptoms,
                timeWindowDays = window.days
            )
        }
        val report = withContext(Dispatchers.Default) {
            DoctorReportBuilder.build(
                meals = meals,
                symptoms = symptoms,
                summary = summary,
                userName = _doctorReportUserName.value.takeIf { it.isNotBlank() }
            )
        }
        return withContext(Dispatchers.IO) {
            DoctorReportPdf.write(context.applicationContext, report)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    val repository = RefluxRepository.create(db)
                    return RefluxViewModel(application, repository) as T
                }
            }
        }
    }
}
