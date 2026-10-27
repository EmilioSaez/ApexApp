package app.luxion.apexapp.ui.anticheat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.anticheat.Capabilities
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AntiCheatSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun show(
        initial: AntiCheatPolicy = AntiCheatPolicy(),
        capabilities: Capabilities = Capabilities(overlay = true, deviceOwner = true),
        onOpenStatus: () -> Unit = {},
    ): () -> AntiCheatPolicy {
        var policy by mutableStateOf(initial)
        compose.setContent {
            ApexAppTheme {
                AntiCheatSection(
                    policy = policy,
                    onChange = { policy = it },
                    onOpenStatus = onOpenStatus,
                    capabilities = capabilities,
                )
            }
        }
        return { policy }
    }

    @Test
    fun choosingLevel_updatesPolicy() {
        val policy = show()
        compose.onNodeWithText(str(R.string.anticheat_level_standard)).performClick()
        assertEquals(AntiCheatLevel.STANDARD, policy().level)
    }

    @Test
    fun snoozeControls_appearOnlyWhenAllowed() {
        val policy = show()
        compose.onNodeWithText(str(R.string.anticheat_snooze_minutes, 5)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.anticheat_snooze_allowed)).performClick()
        assertTrue(policy().snooze.allowed)
        compose.onNodeWithText(str(R.string.anticheat_snooze_minutes, 5)).assertExists()
        compose.onNodeWithText(str(R.string.anticheat_snooze_max, 3)).assertExists()
    }

    @Test
    fun missingOverlay_isWarnedWhenLevelIsActive() {
        show(AntiCheatPolicy(AntiCheatLevel.STANDARD), Capabilities())
        compose.onNodeWithText(str(R.string.anticheat_missing_overlay)).assertExists()
    }

    @Test
    fun noWarnings_whenLevelIsOff() {
        show(AntiCheatPolicy(AntiCheatLevel.OFF), Capabilities())
        compose.onNodeWithText(str(R.string.anticheat_missing_overlay)).assertDoesNotExist()
    }

    @Test
    fun statusLink_opensStatus() {
        var opened = false
        show(onOpenStatus = { opened = true })
        compose.onNodeWithText(str(R.string.anticheat_open_status)).performClick()
        assertTrue(opened)
    }
}
