package app.luxion.apexapp.ui.challenge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChallengesSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val section = ChallengesSection(ChallengeRegistry(listOf(MathChallenge())), setOf(MathRenderer()), emptySet())
    private var draft by mutableStateOf(Alarm(hour = 7, minute = 0))

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show() = compose.setContent {
        ApexAppTheme { section.Content(draft) { draft = it } }
    }

    @Test
    fun normalAlarm_showsExplanationAndHidesChallengeControls() {
        show()
        compose.onNodeWithText(str(R.string.challenge_editor_none)).assertExists()
        compose.onNodeWithText(str(R.string.challenge_test_difficulty)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.challenge_test_types)).assertDoesNotExist()
    }

    @Test
    fun switchingToChallenges_preselectsMathAndShowsControls() {
        show()
        compose.onNodeWithText(str(R.string.challenge_editor_mode_challenges)).performClick()
        compose.waitForIdle()
        assertEquals(listOf("math"), draft.challenges.challenges.map { it.type })
        compose.onNodeWithText(str(R.string.challenge_test_difficulty)).assertExists()
        compose.onNodeWithText(str(R.string.challenge_editor_none)).assertDoesNotExist()
    }

    @Test
    fun switchingBackToNormal_clearsTypes() {
        show()
        compose.onNodeWithText(str(R.string.challenge_editor_mode_challenges)).performClick()
        compose.onNodeWithText(str(R.string.challenge_editor_mode_normal)).performClick()
        compose.waitForIdle()
        assertTrue(draft.challenges.challenges.isEmpty())
        compose.onNodeWithText(str(R.string.challenge_editor_none)).assertExists()
    }
}
