package org.bike4city.ciclofficinabot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.bike4city.ciclofficinabot.data.preferences.UserPreferencesRepository
import org.bike4city.ciclofficinabot.domain.model.BikeProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryTest {
    @Test fun consenso_e_profilo_vengono_salvati_e_riletti() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = UserPreferencesRepository(context)
        repository.clear()
        try {
            repository.acceptOnboarding()
            repository.saveBikeProfile(
                BikeProfile(
                    name = "Bici di prova",
                    bikeType = "Cargo bike",
                    isElectric = true,
                    brand = "Bike4City",
                    model = "Test"
                )
            )

            val saved = repository.current()
            assertTrue(saved.onboardingAccepted)
            assertEquals("Bici di prova", saved.bikeProfile.name)
            assertEquals("Cargo bike", saved.bikeProfile.bikeType)
            assertTrue(saved.bikeProfile.isElectric)
            assertEquals("Bike4City", saved.bikeProfile.brand)
            assertEquals("Test", saved.bikeProfile.model)
        } finally {
            repository.clear()
        }
    }
}
