package app.luxion.apexapp.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class TimeWheelPickerTest {
    @get:Rule
    val compose = createComposeRule()

    private var time by mutableStateOf(7 to 5)

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(hour: Int, minute: Int, is24Hour: Boolean = true) {
        time = hour to minute
        compose.setContent {
            ApexAppTheme {
                TimeWheelPicker(time.first, time.second, { h, m -> time = h to m }, is24Hour = is24Hour)
            }
        }
    }

    private fun SemanticsNodeInteraction.value() =
        fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    private fun SemanticsNodeInteraction.act(label: String) {
        val action = fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        compose.runOnUiThread { action.action() }
        compose.waitForIdle()
    }

    private fun type(digits: String) {
        compose.onNodeWithTag(TimeWheelTags.Minutes).performClick()
        compose.onNodeWithTag(TimeWheelTags.EntryField).performTextReplacement(digits)
    }

    @Test
    fun semantics_exposeCurrentTime() {
        show(7, 5)
        assertEquals("07", compose.onNodeWithTag(TimeWheelTags.Hours).value())
        assertEquals("05", compose.onNodeWithTag(TimeWheelTags.Minutes).value())
    }

    @Test
    fun accessibilityActions_stepAndWrapAround() {
        show(23, 59)
        compose.onNodeWithTag(TimeWheelTags.Minutes).act(str(R.string.time_wheel_minutes_up))
        assertEquals(23 to 0, time)
        compose.onNodeWithTag(TimeWheelTags.Hours).act(str(R.string.time_wheel_hours_up))
        assertEquals(0 to 0, time)
        compose.onNodeWithTag(TimeWheelTags.Hours).act(str(R.string.time_wheel_hours_down))
        assertEquals(23 to 0, time)
        compose.onNodeWithTag(TimeWheelTags.Minutes).act(str(R.string.time_wheel_minutes_down))
        assertEquals(23 to 59, time)
    }

    @Test
    fun swipingTheWheel_changesTheMinute() {
        show(7, 5)
        compose.onNodeWithTag(TimeWheelTags.Minutes).performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertNotEquals(5, time.second)
        assertEquals(7, time.first)
        assertEquals("%02d".format(time.second), compose.onNodeWithTag(TimeWheelTags.Minutes).value())
    }

    @Test
    fun typingDigits_setsTheTimeAndMovesTheWheels() {
        show(23, 59)
        type("705")
        compose.onNodeWithText(str(R.string.time_entry_ok)).performClick()
        compose.waitForIdle()
        assertEquals(7 to 5, time)
        assertEquals("07", compose.onNodeWithTag(TimeWheelTags.Hours).value())
        assertEquals("05", compose.onNodeWithTag(TimeWheelTags.Minutes).value())
    }

    @Test
    fun typingInvalidDigits_disablesConfirm() {
        show(7, 5)
        type("2460")
        compose.onNodeWithText(str(R.string.time_entry_invalid)).assertExists()
        compose.onNodeWithText(str(R.string.time_entry_ok)).assertIsNotEnabled()
        compose.onNodeWithTag(TimeWheelTags.EntryField).performTextReplacement("0000")
        compose.onNodeWithText(str(R.string.time_entry_ok)).assertIsEnabled().performClick()
        assertEquals(0 to 0, time)
    }

    @Test
    fun twelveHour_showsPeriodAndKeepsItWhenTyping() {
        show(0, 0, is24Hour = false)
        assertEquals("12", compose.onNodeWithTag(TimeWheelTags.Hours).value())
        compose.onNodeWithText(str(R.string.time_pm)).performClick()
        assertEquals(12 to 0, time)
        type("1159")
        compose.onNodeWithText(str(R.string.time_entry_ok)).performClick()
        assertEquals(23 to 59, time)
    }
}
