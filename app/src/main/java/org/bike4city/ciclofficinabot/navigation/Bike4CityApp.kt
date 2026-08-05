package org.bike4city.ciclofficinabot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.bike4city.ciclofficinabot.presentation.*
import org.bike4city.ciclofficinabot.R
import kotlinx.coroutines.delay

private object Route {
    const val Welcome = "welcome"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Bike = "bike"
    const val Chat = "chat"
    const val Stop = "stop/{sessionId}"
    const val Report = "report/{sessionId}"
    const val History = "history"
    const val Privacy = "privacy"
}

@Composable
fun Bike4CityApp() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3_000)
        showSplash = false
    }
    if (showSplash) {
        RaggioSplashScreen()
        return
    }

    val context = LocalContext.current.applicationContext
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(context))
    val state by appViewModel.state.collectAsState()
    if (!state.isLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Route.Welcome) {
        composable(Route.Welcome) {
            WelcomeScreen(
                onStart = {
                    nav.navigate(if (state.onboardingAccepted) Route.Home else Route.Onboarding) {
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Onboarding) {
            OnboardingScreen(
                onAccept = {
                    appViewModel.acceptOnboarding()
                    nav.navigate(Route.Home) { popUpTo(Route.Onboarding) { inclusive = true } }
                },
                onPrivacy = { nav.navigate(Route.Privacy) }
            )
        }
        composable(Route.Home) {
            HomeScreen(
                state.bikeProfile,
                onStart = { nav.navigate(Route.Bike) },
                onHistory = { nav.navigate(Route.History) },
                onPrivacy = { nav.navigate(Route.Privacy) }
            )
        }
        composable(Route.Bike) {
            BikeProfileScreen(state.bikeProfile, onBack = nav::popBackStack, onContinue = { profile ->
                appViewModel.saveBikeProfile(profile)
                nav.navigate(Route.Chat)
            })
        }
        composable(Route.Chat) {
            ChatScreen(
                onBack = nav::popBackStack,
                onStop = { sessionId -> nav.navigate("stop/$sessionId") },
                onReport = { sessionId -> nav.navigate("report/$sessionId") }
            )
        }
        composable(Route.Stop, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) { entry ->
            val sessionId = entry.arguments?.getString("sessionId").orEmpty()
            StopScreen(onReport = { nav.navigate("report/$sessionId") })
        }
        composable(Route.Report, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) { entry ->
            val sessionId = entry.arguments?.getString("sessionId").orEmpty()
            ReportScreen(sessionId, onHome = { nav.navigate(Route.Home) { popUpTo(Route.Home) { inclusive = true } } })
        }
        composable(Route.History) {
            HistoryScreen(onBack = nav::popBackStack, onReport = { sessionId -> nav.navigate("report/$sessionId") })
        }
        composable(Route.Privacy) {
            PrivacyScreen(onBack = nav::popBackStack)
        }
    }
}

@Composable
private fun RaggioSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.raggio_logo),
            contentDescription = "RAGGIÒ, l'assistente della ciclofficina",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
