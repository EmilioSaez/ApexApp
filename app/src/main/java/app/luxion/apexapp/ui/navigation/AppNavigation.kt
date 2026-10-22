package app.luxion.apexapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.luxion.apexapp.ui.editor.AlarmEditorScreen
import app.luxion.apexapp.ui.home.HomeScreen
import app.luxion.apexapp.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** `alarmId == 0` abre el editor para una alarma nueva. */
@Serializable
data class AlarmEditorRoute(val alarmId: Long = 0L)

@Serializable
data object SettingsRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onAddAlarm = { navController.navigate(AlarmEditorRoute()) },
                onEditAlarm = { navController.navigate(AlarmEditorRoute(it)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<AlarmEditorRoute> {
            AlarmEditorScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
