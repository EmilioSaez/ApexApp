package app.luxion.apexapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.luxion.apexapp.ui.challenge.ChallengeTestScreen
import app.luxion.apexapp.ui.editor.AlarmEditorScreen
import app.luxion.apexapp.ui.home.HomeScreen
import app.luxion.apexapp.ui.reliability.ReliabilityScreen
import app.luxion.apexapp.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** `alarmId == 0` abre el editor para una alarma nueva. */
@Serializable
data class AlarmEditorRoute(val alarmId: Long = 0L)

@Serializable
data object SettingsRoute

@Serializable
data object ReliabilityRoute
data object ChallengeTestRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onAddAlarm = { navController.navigate(AlarmEditorRoute()) },
                onEditAlarm = { navController.navigate(AlarmEditorRoute(it)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenReliability = { navController.navigate(ReliabilityRoute) },
                onOpenChallengeTest = { navController.navigate(ChallengeTestRoute) },
            )
        }
        composable<AlarmEditorRoute> {
            AlarmEditorScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<ReliabilityRoute> {
            ReliabilityScreen(onBack = { navController.popBackStack() })
        }
        composable<ChallengeTestRoute> {
            ChallengeTestScreen(onBack = { navController.popBackStack() })
        }
    }
}
