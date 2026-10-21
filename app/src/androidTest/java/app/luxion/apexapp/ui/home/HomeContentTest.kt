package app.luxion.apexapp.ui.home

import androidx.compose.ui.test.isToggleable
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.luxion.apexapp.alarm.PermissionState
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(state: HomeUiState, onToggle: (Long, Boolean) -> Unit = { _, _ -> }) {
        compose.setContent {
            ApexAppTheme {
                HomeContent(state, onAddAlarm = {}, onToggle = onToggle, onRequestNotifications = {}, onOpenExactAlarmSettings = {})
            }
        }
    }

    @Test
    fun withoutAlarms_showsEmptyState() {
        show(HomeUiState(isLoading = false))
        compose.onNodeWithText(str(R.string.home_empty)).assertExists()
    }

    @Test
    fun withAlarms_showsList() {
        show(HomeUiState(isLoading = false, alarms = listOf(Alarm(id = 1, hour = 7, minute = 5, label = "Gym"))))
        compose.onNodeWithText("07:05").assertExists()
        compose.onNodeWithText("Gym · " + str(R.string.alarm_once)).assertExists()
        compose.onNodeWithText(str(R.string.home_empty)).assertDoesNotExist()
    }

    @Test
    fun missingPermissions_showBanners() {
        show(HomeUiState(isLoading = false, permissions = PermissionState(notificationsGranted = false, exactAlarmsGranted = false)))
        compose.onNodeWithText(str(R.string.permission_grant)).assertExists()
        compose.onNodeWithText(str(R.string.permission_open_settings)).assertExists()
    }

    @Test
    fun grantedPermissions_showNoBanners() {
        show(HomeUiState(isLoading = false))
        compose.onNodeWithText(str(R.string.permission_grant)).assertDoesNotExist()
    }

    @Test
    fun toggle_reportsAlarmId() {
        var toggled: Pair<Long, Boolean>? = null
        show(HomeUiState(isLoading = false, alarms = listOf(Alarm(id = 4, hour = 7, minute = 5, enabled = true)))) { id, on ->
            toggled = id to on
        }
        compose.onNode(isToggleable()).performClick()
        assertEquals(4L to false, toggled)
    }
}
