package com.example.aharui.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aharui.data.model.UserProfile
import com.example.aharui.data.preferences.PreferencesManager
import com.example.aharui.data.repository.RewardRepository
import com.example.aharui.data.repository.UserProfileRepository
import com.example.aharui.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val rewardRepository: RewardRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId: String
        get() = preferencesManager.currentUserId ?: "default_user"

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        Log.d("ProfileViewModel", "Initializing with userId: $userId")
        loadProfile()
        loadRewards()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userProfileRepository.getUserProfile(userId).collectLatest { profile ->
                Log.d("ProfileViewModel", "Profile loaded: $profile")
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    private fun loadRewards() {
        viewModelScope.launch {
            rewardRepository.getRewards(userId).collectLatest { rewards ->
                _uiState.update {
                    it.copy(
                        totalPoints = rewards?.pointsTotal ?: 0,
                        streakDays = rewards?.streakDays ?: 0
                    )
                }
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Updating profile: $profile")
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Ensure userId is set correctly
            val profileToSave = profile.copy(userId = userId)
            Log.d("ProfileViewModel", "Saving profile with userId: ${profileToSave.userId}")

            when (val result = userProfileRepository.saveUserProfile(profileToSave)) {
                is Result.Success -> {
                    Log.d("ProfileViewModel", "Profile saved successfully")

                    // Also save to preferences for backward compatibility
                    preferencesManager.saveUserProfile(profileToSave)

                    _uiState.update {
                        it.copy(isLoading = false, updateSuccess = true)
                    }
                }
                is Result.Error -> {
                    Log.e("ProfileViewModel", "Profile save failed: ${result.message}")
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                else -> {
                    Log.e("ProfileViewModel", "Unknown result type")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear preferences
            preferencesManager.clearAll()

            // Update UI state
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun clearSuccessState() {
        _uiState.update { it.copy(updateSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ProfileUiState(
    val profile: UserProfile? = null,
    val totalPoints: Int = 0,
    val streakDays: Int = 0,
    val isLoading: Boolean = false,
    val updateSuccess: Boolean = false,
    val loggedOut: Boolean = false,
    val error: String? = null
)