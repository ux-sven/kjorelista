package no.svenway.kjorelista.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import no.svenway.kjorelista.data.AppStore
import no.svenway.kjorelista.data.AuthState
import no.svenway.kjorelista.data.Preferences
import no.svenway.kjorelista.ui.screens.HomeScreen
import no.svenway.kjorelista.ui.screens.LoginScreen
import no.svenway.kjorelista.ui.screens.PrivacyScreen
import no.svenway.kjorelista.ui.screens.SettingsScreen
import no.svenway.kjorelista.ui.screens.TripScreen

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val TRIP = "trip?id={id}&copy={copy}"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"

    fun newTrip() = "trip"
    fun editTrip(id: String) = "trip?id=$id"
    fun duplicateTrip(id: String) = "trip?copy=$id"
}

/** Innlogget → hovedflyt, ellers innlogging. Hver tilstand har sin egen navigasjonsstabel. */
@Composable
fun AppRoot(store: AppStore, preferences: Preferences) {
    val auth by store.authState.collectAsStateWithLifecycle()
    when (auth) {
        AuthState.Initializing -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        is AuthState.SignedIn -> key("main") { MainNavigation(store, preferences) }
        AuthState.SignedOut -> key("auth") { AuthNavigation(store) }
    }
}

@Composable
private fun MainNavigation(store: AppStore, preferences: Preferences) {
    val nav = rememberNavController()
    fun back() {
        if (nav.previousBackStackEntry != null) nav.popBackStack()
    }
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                store = store,
                onAddTrip = { nav.navigate(Routes.newTrip()) },
                onEditTrip = { nav.navigate(Routes.editTrip(it.id)) },
                onDuplicateTrip = { nav.navigate(Routes.duplicateTrip(it.id)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.TRIP,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("copy") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
            // Ny/rediger tur glir opp nedenfra som en fullskjermdialog
            enterTransition = { slideInVertically(tween(300)) { it } + fadeIn(tween(150)) },
            exitTransition = { slideOutVertically(tween(250)) { it } + fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { slideOutVertically(tween(250)) { it } + fadeOut(tween(250)) },
        ) { entry ->
            TripScreen(
                store = store,
                preferences = preferences,
                editId = entry.arguments?.getString("id"),
                copyId = entry.arguments?.getString("copy"),
                onClose = ::back,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                store = store,
                preferences = preferences,
                onBack = ::back,
                onOpenPrivacy = { nav.navigate(Routes.PRIVACY) },
            )
        }
        composable(Routes.PRIVACY) { PrivacyScreen(onBack = ::back) }
    }
}

@Composable
private fun AuthNavigation(store: AppStore) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen(store = store, onOpenPrivacy = { nav.navigate(Routes.PRIVACY) }) }
        composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
    }
}
