package com.example.aharui.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.data.repository.DailyLogRepository
import com.example.aharui.data.repository.UserProfileRepository
import com.example.aharui.data.repository.WaterLogRepository
import com.example.aharui.domain.usecase.BadgeUnlockUseCase
import com.example.aharui.domain.usecase.LogWaterUseCase
import com.example.aharui.domain.usecase.LogWeightUseCase
import com.example.aharui.domain.usecase.UpdateStreakUseCase
import com.example.aharui.util.DateUtils
import com.example.aharui.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val waterLogRepository: WaterLogRepository,
    private val logWaterUseCase: LogWaterUseCase,
    private val logWeightUseCase: LogWeightUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val badgeUnlockUseCase: BadgeUnlockUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId: String
        get() = preferencesManager.currentUserId ?: "default_user"

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadChartData(DateUtils.ChartType.WEEKLY)
    }

    private fun loadData() {
        viewModelScope.launch {
            userProfileRepository.getUserProfile(userId).collectLatest { profile ->
                profile?.let {
                    _uiState.update { state ->
                        state.copy(
                            userName = it.name,
                            calorieTarget = it.dailyCalorieTarget,
                            waterTarget = preferencesManager.dailyWaterGoal
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            val today = DateUtils.getTodayString()
            dailyLogRepository.ensureTodayLogExists(userId, today)

            dailyLogRepository.getLogForDate(userId, today).collectLatest { log ->
                log?.let {
                    _uiState.update { state ->
                        state.copy(
                            caloriesConsumed = it.totalCalories,
                            currentWeight = it.weightKg
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            waterLogRepository.getTotalWaterForDay(userId, Date()).collectLatest { totalWater ->
                _uiState.update { state ->
                    state.copy(waterConsumed = totalWater)
                }
            }
        }

        viewModelScope.launch {
            when (updateStreakUseCase(userId)) {
                is Result.Success -> {
                    val unlockedBadges = badgeUnlockUseCase(userId)
                    if (unlockedBadges.isNotEmpty()) {
                        _uiState.update { it.copy(unlockedBadges = unlockedBadges) }
                    }
                }
                else -> {}
            }
        }
    }

    fun refreshWaterGoal() {
        _uiState.update { state ->
            state.copy(waterTarget = preferencesManager.dailyWaterGoal)
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = logWaterUseCase(userId, amountMl)) {
                is Result.Success -> {
                    val unlockedBadges = badgeUnlockUseCase(userId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            unlockedBadges = unlockedBadges
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun loadChartData(chartType: DateUtils.ChartType) {
        viewModelScope.launch {
            val today = DateUtils.getTodayString()
            val startDate = when (chartType) {
                DateUtils.ChartType.WEEKLY -> DateUtils.getWeekAgo()
                DateUtils.ChartType.MONTHLY -> DateUtils.getMonthAgo()
                DateUtils.ChartType.SIX_MONTHS -> DateUtils.getSixMonthsAgo()
            }

            Log.d("HomeViewModel", "Loading chart data from $startDate to $today")

            dailyLogRepository.getLogsForDateRange(userId, startDate, today)
                .collectLatest { logs ->
                    Log.d("HomeViewModel", "Received ${logs.size} logs")

                    // Get all dates in the range
                    val allDates = DateUtils.getDatesBetween(startDate, today)
                    Log.d("HomeViewModel", "All dates count: ${allDates.size}")

                    // Create a map of date to weight
                    val weightMap = logs.associate { log ->
                        log.date to log.weightKg
                    }.filterValues { it != null }  // Filter out null weights

                    Log.d("HomeViewModel", "Weight map size: ${weightMap.size}, entries: $weightMap")

                    // Get chart data and labels based on chart type
                    val (chartData, chartLabels) = when (chartType) {
                        DateUtils.ChartType.WEEKLY -> {
                            // For weekly, only show dates that have weight data
                            val datesWithData = allDates.filter { weightMap.containsKey(it) }
                            val data = datesWithData.mapNotNull { date ->
                                weightMap[date]?.toFloat()
                            }
                            val labels = datesWithData.map { date ->
                                DateUtils.parseDate(date)?.let {
                                    SimpleDateFormat("EEE", Locale.getDefault()).format(it)
                                } ?: ""
                            }
                            Log.d("HomeViewModel", "Weekly - Data points: ${data.size}, Labels: $labels")
                            data to labels
                        }
                        DateUtils.ChartType.MONTHLY -> {
                            // Show dates with data, preferring every 5th day
                            val filteredDates = allDates.filter { weightMap.containsKey(it) }
                            val data = filteredDates.mapNotNull { date ->
                                weightMap[date]?.toFloat()
                            }
                            val labels = filteredDates.map { date ->
                                DateUtils.parseDate(date)?.let {
                                    SimpleDateFormat("d", Locale.getDefault()).format(it)
                                } ?: ""
                            }
                            data to labels
                        }
                        DateUtils.ChartType.SIX_MONTHS -> {
                            // Show one data point per month (first available weight of each month)
                            val monthlyData = mutableListOf<Float>()
                            val monthlyLabels = mutableListOf<String>()
                            var currentMonth = ""

                            allDates.forEach { date ->
                                val monthLabel = DateUtils.parseDate(date)?.let {
                                    SimpleDateFormat("MMM", Locale.getDefault()).format(it)
                                } ?: ""

                                if (monthLabel != currentMonth && weightMap.containsKey(date)) {
                                    weightMap[date]?.toFloat()?.let { weight ->
                                        monthlyData.add(weight)
                                        monthlyLabels.add(monthLabel)
                                        currentMonth = monthLabel
                                    }
                                }
                            }
                            monthlyData to monthlyLabels
                        }
                    }

                    Log.d("HomeViewModel", "Final chart data: $chartData")
                    Log.d("HomeViewModel", "Final chart labels: $chartLabels")

                    // Update state with chart data
                    if (chartData.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                chartData = chartData,
                                chartLabels = chartLabels
                            )
                        }
                        Log.d("HomeViewModel", "Chart updated with ${chartData.size} points")
                    } else {
                        _uiState.update {
                            it.copy(
                                chartData = emptyList(),
                                chartLabels = emptyList()
                            )
                        }
                        Log.d("HomeViewModel", "No chart data available")
                    }
                }
        }
    }

    fun clearBadgeNotifications() {
        _uiState.update { it.copy(unlockedBadges = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun logWeight(weightKg: Double, date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = logWeightUseCase(userId, weightKg, date)) {
                is Result.Success -> {
                    Log.d("HomeViewModel", "Weight logged successfully: $weightKg kg on $date")
                    // Reload chart data to show new weight
                    loadChartData(DateUtils.ChartType.WEEKLY)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentWeight = weightKg,
                            weightLoggedSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    Log.e("HomeViewModel", "Failed to log weight: ${result.message}")
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun clearWeightLoggedState() {
        _uiState.update { it.copy(weightLoggedSuccess = false) }
    }

    // Temporary debug function - add some test weight data
    fun addTestWeightData() {
        viewModelScope.launch {
            val today = DateUtils.getTodayString()
            for (i in 0..6) {
                val date = DateUtils.getDaysAgo(i)
                val testWeight = 70.0 + (i * 0.5) // Simulated weight progression
                dailyLogRepository.updateWeight(userId, date, testWeight)
                Log.d("HomeViewModel", "Added test weight: $testWeight kg for date: $date")
            }
            // Reload chart
            loadChartData(DateUtils.ChartType.WEEKLY)
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    val caloriesConsumed: Int = 0,
    val calorieTarget: Int = 2000,
    val waterConsumed: Int = 0,
    val waterTarget: Int = 2500,
    val currentWeight: Double? = null,
    val chartData: List<Float> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val weightLoggedSuccess: Boolean = false,
    val unlockedBadges: List<String> = emptyList(),
    val error: String? = null
)