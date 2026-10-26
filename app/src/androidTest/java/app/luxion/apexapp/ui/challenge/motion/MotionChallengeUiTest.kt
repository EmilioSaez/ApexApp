package app.luxion.apexapp.ui.challenge.motion

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.sensor.MotionProgress
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Rule
import org.junit.Test

class MotionChallengeUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Test
    fun targetInfo_showsTheTargetWithoutControls() {
        compose.setContent {
            ApexAppTheme { MotionTargetInfo("Objetivo con esta dificultad: 55 pasos") }
        }
        compose.onNodeWithText("Objetivo con esta dificultad: 55 pasos").assertExists()
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
