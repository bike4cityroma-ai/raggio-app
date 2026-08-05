package org.bike4city.ciclofficinabot.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bike4city.ciclofficinabot.data.preferences.UserPreferencesRepository
import org.bike4city.ciclofficinabot.domain.model.BikeProfile

data class AppUiState(
    val isLoaded: Boolean = false,
    val onboardingAccepted: Boolean = false,
    val bikeProfile: BikeProfile = BikeProfile()
)

class AppViewModel(private val preferences: UserPreferencesRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.preferences.collect { saved ->
                _state.value = AppUiState(true, saved.onboardingAccepted, saved.bikeProfile)
            }
        }
    }

    fun acceptOnboarding() {
        _state.update { it.copy(onboardingAccepted = true) }
        viewModelScope.launch { preferences.acceptOnboarding() }
    }

    fun saveBikeProfile(profile: BikeProfile) {
        _state.update { it.copy(bikeProfile = profile) }
        viewModelScope.launch { preferences.saveBikeProfile(profile) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(UserPreferencesRepository(context.applicationContext)) as T
        }
    }
}
