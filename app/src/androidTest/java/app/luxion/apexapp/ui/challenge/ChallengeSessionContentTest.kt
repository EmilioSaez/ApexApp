package app.luxion.apexapp.ui.challenge

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionState
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** El flujo de sonar solo termina (apaga) desde la pantalla de sesión superada. */
class ChallengeSessionContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val instance = object : ChallengeInstance {
        override val type = ChallengeType.MATH
        override val level = 1
        override val prompt = "2 + 2"
        override fun check(answer: ChallengeAnswer) = false
    }

    private fun show(state: SessionState, onFinished: () -> Unit = {}) = compose.setContent {
        ApexAppTheme {
            ChallengeSessionContent(
                state = state,
                rendererFor = { MathRenderer() },
                onAnswer = {},
                onUnavailable = {},
                onFinished = onFinished,
            )
        }
    }

    @Test
    fun unsolvedSession_offersNoWayToFinish() {
        show(SessionState(solved = 0, total = 2, current = instance, passed = false))
        compose.onNodeWithText(str(R.string.challenge_done)).assertDoesNotExist()
    }

    @Test
    fun passedSession_finishesOnlyFromItsButton() {
        var finished = 0
        show(SessionState(solved = 2, total = 2, current = null, passed = true)) { finished++ }
        assertEquals(0, finished)
        compose.onNodeWithText(str(R.string.challenge_done)).performClick()
        assertEquals(1, finished)
    }

    @Test
    fun keypad_buildsTheAnswerAndSubmitsIt() {
        var answered: ChallengeAnswer? = null
        compose.setContent {
            ApexAppTheme {
                ChallengeSessionContent(
                    state = SessionState(solved = 0, total = 2, current = instance, passed = false),
                    rendererFor = { MathRenderer() },
                    onAnswer = { answered = it },
                    onUnavailable = {},
                    onFinished = {},
                )
            }
        }
        // Sin respuesta no se puede comprobar; con ella, el borrado quita el ultimo digito.
        compose.onNodeWithText(str(R.string.challenge_confirm)).assertIsNotEnabled()
        compose.onNodeWithText("4").performClick()
        compose.onNodeWithText("7").performClick()
        compose.onNodeWithText("9").performClick()
        compose.onNodeWithContentDescription(str(R.string.challenge_backspace)).performClick()
        compose.onNodeWithText(str(R.string.challenge_confirm)).performClick()
        assertEquals(ChallengeAnswer.Text("47"), answered)
    }
}
