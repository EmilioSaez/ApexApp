package app.luxion.apexapp.ui.challenge.motion

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.sensor.MotionProgress
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MotionChallengeUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Test
    fun targetField_clampsAtMaximumAndWarns() {
        val value = mutableStateOf(MotionKind.SHAKE.max)
        compose.setContent {
            ApexAppTheme {
                MotionTargetField(MotionKind.SHAKE, value.value, onValueChange = { value.value = it })
            }
        }
        compose.onNodeWithText(str(R.string.challenge_target_increase)).performClick()
        assertEquals(MotionKind.SHAKE.max, value.value)
        compose.onNodeWithText(
            str(R.string.challenge_target_clamped, MotionKind.SHAKE.min, MotionKind.SHAKE.max),
        ).assertExists()
    }

    @Test
    fun targetField_clampsAtMinimum() {
        val value = mutableStateOf(MotionKind.STEPS.min)
        compose.setContent {
            ApexAppTheme {
                MotionTargetField(MotionKind.STEPS, value.value, onValueChange = { value.value = it })
            }
        }
        compose.onNodeWithText(str(R.string.challenge_target_decrease)).performClick()
        assertEquals(MotionKind.STEPS.min, value.value)
    }

    @Test
    fun body_showsRejectionHintWhenMovementIsRejected() {
        compose.setContent {
            ApexAppTheme {
                MotionChallengeBody(MotionKind.SHAKE, 20, MotionProgress(0f, rejected = true))
            }
        }
        compose.onNodeWithText(str(R.string.challenge_motion_rejected)).assertExists()
    }
}
