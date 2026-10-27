package app.luxion.apexapp.ui.sharing

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.sharing.ImportError
import app.luxion.apexapp.sharing.ImportNote
import app.luxion.apexapp.sharing.ImportPreview
import app.luxion.apexapp.sharing.QrCodeMatrix
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharingContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun showImport(state: ImportUiState, scanning: Boolean = false, onAccept: () -> Unit = {}, onDiscard: () -> Unit = {}) {
        compose.setContent {
            ApexAppTheme {
                ImportContent(
                    state = state,
                    scanning = scanning,
                    onBack = {},
                    onScan = {},
                    onStopScan = {},
                    onScanned = {},
                    onPickFile = {},
                    onAccept = onAccept,
                    onDiscard = onDiscard,
                )
            }
        }
    }

    private val preview = ImportPreview(
        alarm = Alarm(hour = 6, minute = 45, enabled = false, label = "Gimnasio"),
        soundKey = null,
        notes = setOf(ImportNote.QR_CHALLENGE_OMITTED),
    )

    @Test
    fun entry_offersScanAndFile() {
        showImport(ImportUiState.Entry)
        compose.onNodeWithText(str(R.string.import_scan)).assertExists()
        compose.onNodeWithText(str(R.string.import_pick_file)).assertExists()
    }

    @Test
    fun preview_showsTheAlarmAndItsNotes_andAcceptsOrDiscards() {
        var accepted = 0
        var discarded = 0
        showImport(ImportUiState.Preview(preview), onAccept = { accepted++ }, onDiscard = { discarded++ })
        compose.onNodeWithText("06:45").assertExists()
        compose.onNodeWithText(str(R.string.import_note_qr)).assertExists()
        compose.onNodeWithText(str(R.string.import_disabled_note)).assertExists()
        compose.onNodeWithText(str(R.string.import_accept)).performClick()
        compose.onNodeWithText(str(R.string.import_discard)).performClick()
        assertEquals(1, accepted)
        assertEquals(1, discarded)
    }

    @Test
    fun failure_showsTheReason() {
        showImport(ImportUiState.Failed(ImportError.UNSUPPORTED_VERSION))
        compose.onNodeWithText(str(R.string.import_error_version)).assertExists()
    }

    @Test
    fun share_showsQrCaptionAndActions() {
        val matrix = QrCodeMatrix.of("apexapp://alarm?d=abc")
        compose.setContent {
            ApexAppTheme {
                ShareAlarmContent(
                    state = ShareUiState(isLoading = false, alarm = Alarm(id = 1, hour = 7, minute = 0), link = "x", qr = matrix),
                    onBack = {},
                    onShareLink = {},
                    onShareFile = {},
                )
            }
        }
        compose.onNodeWithText(str(R.string.share_qr_caption)).assertExists()
        compose.onNodeWithText(str(R.string.share_link_action)).assertExists()
        compose.onNodeWithText(str(R.string.share_file_action)).assertExists()
    }

    @Test
    fun share_withoutQr_explainsWhy() {
        compose.setContent {
            ApexAppTheme {
                ShareAlarmContent(
                    state = ShareUiState(isLoading = false, alarm = Alarm(id = 1, hour = 7, minute = 0), qr = null),
                    onBack = {},
                    onShareLink = {},
                    onShareFile = {},
                )
            }
        }
        compose.onNodeWithText(str(R.string.share_qr_too_large)).assertExists()
    }

    @Test
    fun streakCard_withoutAchievements_showsEmptyState() {
        compose.setContent {
            ApexAppTheme { StreakCardContent(StreakCardUiState(Streak(0, 0)), onBack = {}, onShare = {}) }
        }
        compose.onNodeWithText(str(R.string.streak_empty)).assertExists()
    }

    @Test
    fun streakCard_withAchievements_canBeShared() {
        var shared = 0
        compose.setContent {
            ApexAppTheme { StreakCardContent(StreakCardUiState(Streak(3, 9)), onBack = {}, onShare = { shared++ }) }
        }
        compose.onNodeWithText(str(R.string.streak_share_action)).performClick()
        assertEquals(1, shared)
    }
}
