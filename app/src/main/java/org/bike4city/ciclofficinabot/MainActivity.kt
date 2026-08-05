package org.bike4city.ciclofficinabot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.bike4city.ciclofficinabot.core.designsystem.Bike4CityTheme
import org.bike4city.ciclofficinabot.navigation.Bike4CityApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Bike4CityTheme { Bike4CityApp() } }
    }
}
