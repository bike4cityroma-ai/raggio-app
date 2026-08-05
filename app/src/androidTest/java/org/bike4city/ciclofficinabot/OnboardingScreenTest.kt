package org.bike4city.ciclofficinabot

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.bike4city.ciclofficinabot.core.designsystem.Bike4CityTheme
import org.bike4city.ciclofficinabot.presentation.OnboardingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun consenso_sicurezza_abilita_il_pulsante_inizia() {
        composeRule.setContent { Bike4CityTheme { OnboardingScreen(onAccept = {}) } }
        composeRule.onNodeWithText("Inizia").assertIsNotEnabled()
        composeRule.onNodeWithTag("safety-consent").performClick()
        composeRule.onNodeWithText("Inizia").assertIsEnabled()
    }
}
