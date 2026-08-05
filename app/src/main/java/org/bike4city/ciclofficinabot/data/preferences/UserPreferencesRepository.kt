package org.bike4city.ciclofficinabot.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.bike4city.ciclofficinabot.domain.model.BikeProfile

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val onboardingAccepted: Boolean = false,
    val bikeProfile: BikeProfile = BikeProfile()
)

class UserPreferencesRepository(private val context: Context) {
    val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::toUserPreferences)

    suspend fun acceptOnboarding() {
        context.userPreferencesDataStore.edit { it[Keys.ONBOARDING_ACCEPTED] = true }
    }

    suspend fun saveBikeProfile(profile: BikeProfile) {
        context.userPreferencesDataStore.edit { values ->
            values[Keys.BIKE_NAME] = profile.name
            values[Keys.BIKE_TYPE] = profile.bikeType
            values[Keys.BIKE_ELECTRIC] = profile.isElectric
            values[Keys.BIKE_BRAND] = profile.brand
            values[Keys.BIKE_MODEL] = profile.model
            values[Keys.BIKE_BRAKE_TYPE] = profile.brakeType
        }
    }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun clear() {
        context.userPreferencesDataStore.edit { it.clear() }
    }

    private fun toUserPreferences(values: Preferences) = UserPreferences(
        onboardingAccepted = values[Keys.ONBOARDING_ACCEPTED] ?: false,
        bikeProfile = BikeProfile(
            name = values[Keys.BIKE_NAME] ?: "La mia bici",
            bikeType = values[Keys.BIKE_TYPE] ?: "City bike",
            isElectric = values[Keys.BIKE_ELECTRIC] ?: false,
            brand = values[Keys.BIKE_BRAND] ?: "",
            model = values[Keys.BIKE_MODEL] ?: "",
            brakeType = values[Keys.BIKE_BRAKE_TYPE] ?: "Non lo so"
        )
    )

    private object Keys {
        val ONBOARDING_ACCEPTED = booleanPreferencesKey("onboarding_accepted")
        val BIKE_NAME = stringPreferencesKey("bike_name")
        val BIKE_TYPE = stringPreferencesKey("bike_type")
        val BIKE_ELECTRIC = booleanPreferencesKey("bike_electric")
        val BIKE_BRAND = stringPreferencesKey("bike_brand")
        val BIKE_MODEL = stringPreferencesKey("bike_model")
        val BIKE_BRAKE_TYPE = stringPreferencesKey("bike_brake_type")
    }
}
