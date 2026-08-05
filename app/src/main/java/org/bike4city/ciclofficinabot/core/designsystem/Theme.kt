package org.bike4city.ciclofficinabot.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BikeGreen = Color(0xFF16784A)
val MobilityBlue = Color(0xFF176B9C)
val AttentionYellow = Color(0xFFF3B61F)
val RiskRed = Color(0xFFBA1A1A)

private val Light = lightColorScheme(primary = BikeGreen, secondary = MobilityBlue, error = RiskRed)
private val Dark = darkColorScheme(primary = Color(0xFF65D89C), secondary = Color(0xFF85CFFF), error = Color(0xFFFFB4AB))

@Composable
fun Bike4CityTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
