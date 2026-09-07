package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analysis.FoodTriggerAnalysis
import com.example.analysis.TriggerAnalysisEngine
import com.example.data.local.AppDatabase
import com.example.data.model.FoodEntity
import com.example.data.model.MealWithFoods
import com.example.data.model.SymptomEntity
import com.example.data.repository.RefluxRepository
import com.example.data.repository.TimelineEntry
import com.example.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        }
    }

    val allFoods: StateFlow<List<FoodEntity>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeals: StateFlow<List<MealWithFoods>> = repository.allMealsWithFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSymptoms: StateFlow<List<SymptomEntity>> = repository.allSymptoms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            onSuccess()
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
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addSymptom(timestamp, symptomType, intensity, durationMinutes, notes)
            onSuccess()
        }
    }

    fun deleteSymptom(symptomId: Long) {
        viewModelScope.launch {
            repository.deleteSymptom(symptomId)
        }
    }

    fun addCustomFood(name: String, category: String = "Eigene Lebensmittel", onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertCustomFood(name, category)
            onAdded(id)
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
