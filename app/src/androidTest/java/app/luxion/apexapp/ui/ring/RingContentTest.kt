package app.luxion.apexapp.ui.ring

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RingContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(canDismiss: Boolean, onDismiss: () -> Unit = {}) {
        compose.setContent {
            ApexAppTheme {
                RingContent(Alarm(id = 1, hour = 6, minute = 5, label = "Gym"), canDismiss, onDismiss)
            }
        }
    }

    @Test
    fun showsTimeAndLabel_andDismisses() {
        var dismissed = 0
        show(canDismiss = true) { dismissed++ }
        compose.onNodeWithText("06:05").assertExists()
        compose.onNodeWithText("Gym").assertExists()
        compose.onNodeWithText(str(R.string.ring_dismiss)).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun gateDenying_hidesDismiss() {
        show(canDismiss = false)
        compose.onNodeWithText(str(R.string.ring_dismiss)).assertDoesNotExist()
    }
}
