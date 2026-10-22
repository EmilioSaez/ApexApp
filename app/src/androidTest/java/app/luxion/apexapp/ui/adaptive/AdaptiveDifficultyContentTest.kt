package app.luxion.apexapp.ui.adaptive

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.adaptive.ChangeReason
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdaptiveDifficultyContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun show(state: AdaptiveUiState, onEnabled: (Boolean) -> Unit = {}, onReset: () -> Unit = {}) {
        compose.setContent {
            ApexAppTheme {
                AdaptiveDifficultyContent(state, onEnabled = onEnabled, onRange = { _, _ -> }, onReset = onReset)
            }
        }
    }

    @Test
    fun showsExplanationAndNotifiesTheSwitch() {
        var enabled: Boolean? = null
        show(AdaptiveUiState(available = true), onEnabled = { enabled = it })

        compose.onNodeWithText(str(R.string.adaptive_explanation)).assertExists()
        compose.onNodeWithText(str(R.string.adaptive_current_level, 3)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.adaptive_title)).assertExists()
        // El interruptor no tiene texto propio: se pulsa a través del nodo conmutable.
        compose.onNode(androidx.compose.ui.test.isToggleable()).performClick()
        assertEquals(true, enabled)
    }

    @Test
    fun newAlarmCannotEnableItAndExplainsWhy() {
        show(AdaptiveUiState(available = false))
        compose.onNodeWithText(str(R.string.adaptive_save_first)).assertExists()
        compose.onNode(androidx.compose.ui.test.isToggleable()).assertIsNotEnabled()
    }

    @Test
    fun enabledShowsLevelReasonAndReset() {
        var reset = false
        show(
            AdaptiveUiState(available = true, enabled = true, current = 4, min = 2, max = 5,
                reason = ChangeReason.SNOOZES, hits = 2),
            onReset = { reset = true },
        )
        compose.onNode(androidx.compose.ui.test.isToggleable()).assertIsEnabled()
        compose.onNodeWithText(str(R.string.adaptive_current_level, 4)).assertExists()
        compose.onNodeWithText(str(R.string.adaptive_reason_snoozes, 4, 2)).assertExists()
        compose.onNodeWithText(str(R.string.adaptive_range, 2, 5)).assertExists()
        compose.onNodeWithText(str(R.string.adaptive_reset)).performClick()
        assertEquals(true, reset)
    }

    @Test
    fun everyReasonHasItsOwnText() {
        val cases = mapOf(
            ChangeReason.SNOOZES to str(R.string.adaptive_reason_snoozes, 4, 2),
            ChangeReason.EASY to str(R.string.adaptive_reason_easy, 4),
            ChangeReason.FAILURES to str(R.string.adaptive_reason_failures, 2, 2),
            ChangeReason.SLOW to str(R.string.adaptive_reason_slow, 2, 3),
        )
        val notice = mutableStateOf<AdaptiveNotice?>(null)
        compose.setContent {
            ApexAppTheme { notice.value?.let { AdaptiveLevelNotice(it) } }
        }
        cases.forEach { (reason, text) ->
            val (level, hits) = when (reason) {
                ChangeReason.SNOOZES -> 4 to 2
                ChangeReason.EASY -> 4 to 3
                ChangeReason.FAILURES -> 2 to 2
                ChangeReason.SLOW -> 2 to 3
            }
            notice.value = AdaptiveNotice(level, reason, hits)
            compose.waitForIdle()
            compose.onNodeWithText(text).assertExists()
        }
    }

    @Test
    fun noticeShowsOnlyWhenPresent() {
        compose.setContent { ApexAppTheme { AdaptiveLevelNotice(AdaptiveNotice(4, ChangeReason.SNOOZES, 2)) } }
        compose.onNodeWithText(str(R.string.adaptive_reason_snoozes, 4, 2)).assertExists()
    }
}
