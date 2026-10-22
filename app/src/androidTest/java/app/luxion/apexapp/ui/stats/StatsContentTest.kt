package app.luxion.apexapp.ui.stats

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.ui.theme.ApexAppTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StatsContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun days(n: Int) = (n - 1 downTo 0).map {
        DayPoint(LocalDate.of(2026, 3, 10).minusDays(it.toLong()), 150_000L, 1, 0, 1.0)
    }

    private fun show(
        state: StatsUiState,
        onSelectRange: (StatsRange) -> Unit = {},
        onSetLastNightBedtime: (LocalTime) -> Unit = {},
    ) {
        compose.setContent {
            ApexAppTheme {
                StatsContent(state, onBack = {}, onSelectRange, onSetLastNightBedtime, onSetUsualBedtime = {})
            }
        }
    }

    @Test
    fun withoutData_showsEmptyState() {
        show(StatsUiState(isLoading = false, hasData = false, days = days(7)))
        compose.onNodeWithText(str(R.string.stats_empty)).assertExists()
        compose.onNodeWithText(str(R.string.stats_empty_hint)).assertExists()
    }

    @Test
    fun withData_showsChartsWithAccessibleDescriptions() {
        show(StatsUiState(isLoading = false, hasData = true, days = days(7), streak = Streak(3, 5)))
        compose.onNodeWithText(str(R.string.stats_empty)).assertDoesNotExist()
        compose.onAllNodes(hasContentDescription(str(R.string.stats_wake_time), substring = true)).assertCountEquals(1)
        compose.onAllNodes(hasContentDescription(str(R.string.stats_sleep), substring = true)).assertCountEquals(1)
    }

    @Test
    fun selectingRange_notifiesCallback() {
        var selected: StatsRange? = null
        show(StatsUiState(isLoading = false), onSelectRange = { selected = it })
        compose.onNodeWithText(str(R.string.stats_range_days).format(30)).performClick()
        assertEquals(StatsRange.MONTH, selected)
    }

    @Test
    fun bedtimeDialog_confirmsSelectedTime() {
        var saved: LocalTime? = null
        show(StatsUiState(isLoading = false), onSetLastNightBedtime = { saved = it })
        compose.onNodeWithText(str(R.string.stats_set_bedtime)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.stats_dialog_ok)).performClick()
        assertEquals(LocalTime.of(23, 0), saved)
    }
}
