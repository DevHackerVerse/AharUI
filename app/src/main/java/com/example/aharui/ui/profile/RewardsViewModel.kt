package com.example.aharui.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aharui.data.model.Badge
import com.example.aharui.data.model.RewardData
import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.data.repository.RewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardRepository: RewardRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId: String
        get() = preferencesManager.currentUserId ?: "default_user"

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadRewards()
    }

    private fun loadRewards() {
        viewModelScope.launch {
            rewardRepository.getRewards(userId).collectLatest { rewards ->
                _uiState.update {
                    it.copy(
                        pointsTotal = rewards?.pointsTotal ?: 0,
                        streakDays = rewards?.streakDays ?: 0,
                        badges = rewards?.badges ?: emptyList(),
                        unlockedBadges = rewards?.badges?.filter { badge -> badge.isUnlocked } ?: emptyList(),
                        lockedBadges = rewards?.badges?.filter { badge -> !badge.isUnlocked } ?: emptyList()
                    )
                }
            }
        }
    }
}

data class RewardsUiState(
    val pointsTotal: Int = 0,
    val streakDays: Int = 0,
    val badges: List<Badge> = emptyList(),
    val unlockedBadges: List<Badge> = emptyList(),
    val lockedBadges: List<Badge> = emptyList()
)