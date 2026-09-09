package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RefluxDatabase
import com.example.data.model.FoodEntity
import com.example.data.model.FoodTriggerAnalysis
import com.example.data.model.MealEntity
import com.example.data.model.MealType
import com.example.data.model.MealWithFoods
import com.example.data.model.PortionSize
import com.example.data.model.SymptomEntity
import com.example.data.model.SymptomType
import com.example.data.model.TimelineItem
import com.example.data.repository.RefluxRepository
import com.example.util.CsvBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RefluxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RefluxRepository

    val allFoods: StateFlow<List<FoodEntity>>
    val allMealsWithFoods: StateFlow<List<MealWithFoods>>
    val allSymptoms: StateFlow<List<SymptomEntity>>
    val timelineItems: StateFlow<List<TimelineItem>>

    private val _triggerAnalysis = MutableStateFlow<List<FoodTriggerAnalysis>>(emptyList())
    val triggerAnalysis: StateFlow<List<FoodTriggerAnalysis>> = _triggerAnalysis.asStateFlow()

    private val _analysisDays = MutableStateFlow(14)
    val analysisDays: StateFlow<Int> = _analysisDays.asStateFlow()

    init {
        val db = RefluxDatabase.getDatabase(application)
        repository = RefluxRepository(db.refluxDao())

        allFoods = repository.allFoods.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allMealsWithFoods = repository.allMealsWithFoods.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allSymptoms = repository.allSymptoms.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        timelineItems = repository.timelineItems.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        refreshAnalysis()
    }

    fun setAnalysisDays(days: Int) {
        _analysisDays.value = days
        refreshAnalysis()
    }

    fun refreshAnalysis() {
        viewModelScope.launch {
            _triggerAnalysis.value = repository.analyzeTriggers(lookbackDays = _analysisDays.value)
        }
    }

    suspend fun getMealWithFoods(mealId: Long): MealWithFoods? {
        return repository.getMealWithFoodsById(mealId)
    }

    suspend fun getSymptom(symptomId: Long): SymptomEntity? {
        return repository.getSymptomById(symptomId)
    }

    fun addMeal(
        mealType: MealType,
        portionSize: PortionSize,
        foodIds: List<Long>,
        timestamp: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val meal = MealEntity(
                timestamp = timestamp,
                mealType = mealType,
                portionSize = portionSize,
                notes = notes
            )
            repository.addMeal(meal, foodIds)
            refreshAnalysis()
        }
    }

    fun updateMeal(
        mealId: Long,
        mealType: MealType,
        portionSize: PortionSize,
        foodIds: List<Long>,
        timestamp: Long,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val meal = MealEntity(
                id = mealId,
                timestamp = timestamp,
                mealType = mealType,
                portionSize = portionSize,
                notes = notes
            )
            repository.updateMeal(meal, foodIds)
            refreshAnalysis()
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
            refreshAnalysis()
        }
    }

    fun addSymptom(
        symptoms: List<String>,
        intensity: Int,
        durationMinutes: Int = 30,
        timestamp: Long = System.currentTimeMillis(),
        notes: String = "",
        activities: List<String> = emptyList(),
        remedies: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val primaryType = symptoms.firstOrNull()?.let { name ->
                SymptomType.values().find {
                    it.displayName.equals(name, ignoreCase = true) ||
                    it.name.equals(name, ignoreCase = true)
                }
            } ?: SymptomType.SODBRENNEN

            val symptom = SymptomEntity(
                timestamp = timestamp,
                symptomType = primaryType,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes,
                symptoms = symptoms.joinToString(", "),
                activities = activities.joinToString(", "),
                remedies = remedies.joinToString(", ")
            )
            repository.addSymptom(symptom)
            refreshAnalysis()
        }
    }

    fun updateSymptom(
        symptomId: Long,
        symptoms: List<String>,
        intensity: Int,
        durationMinutes: Int = 30,
        timestamp: Long,
        notes: String = "",
        activities: List<String> = emptyList(),
        remedies: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val primaryType = symptoms.firstOrNull()?.let { name ->
                SymptomType.values().find {
                    it.displayName.equals(name, ignoreCase = true) ||
                    it.name.equals(name, ignoreCase = true)
                }
            } ?: SymptomType.SODBRENNEN

            val symptom = SymptomEntity(
                id = symptomId,
                timestamp = timestamp,
                symptomType = primaryType,
                intensity = intensity,
                durationMinutes = durationMinutes,
                notes = notes,
                symptoms = symptoms.joinToString(", "),
                activities = activities.joinToString(", "),
                remedies = remedies.joinToString(", ")
            )
            repository.updateSymptom(symptom)
            refreshAnalysis()
        }
    }

    fun deleteSymptom(symptomId: Long) {
        viewModelScope.launch {
            repository.deleteSymptom(symptomId)
            refreshAnalysis()
        }
    }

    suspend fun exportCsvData(): String = withContext(Dispatchers.IO) {
        val meals = repository.getAllMealsSync()
        val symptoms = repository.getAllSymptomsSync()
        CsvBackupManager.exportToCsv(meals, symptoms)
    }

    fun validateCsvData(csvContent: String): CsvBackupManager.ValidationResult {
        return CsvBackupManager.validateCsv(csvContent)
    }

    suspend fun importCsvData(csvContent: String, skipDuplicates: Boolean = true): CsvBackupManager.ImportResult = withContext(Dispatchers.IO) {
        val result = CsvBackupManager.importCsv(csvContent, repository, skipDuplicates)
        refreshAnalysis()
        result
    }

    fun addNewFood(name: String, category: String = "Allgemein"): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = repository.insertFood(FoodEntity(name = name.trim(), category = category.trim()))
        }
        return newId
    }

    suspend fun findOrCreateFood(name: String): Long {
        val trimmed = name.trim()
        val existing = repository.getFoodByName(trimmed)
        return existing?.id ?: repository.insertFood(FoodEntity(name = trimmed))
    }

    fun deleteFood(food: FoodEntity) {
        viewModelScope.launch {
            repository.deleteFood(food)
            refreshAnalysis()
        }
    }
}
