package app.luxion.apexapp.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.ui.components.TimeWheelTags
import app.luxion.apexapp.ui.theme.ApexAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlarmEditorContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(
        state: EditorUiState = EditorUiState(isLoading = false, hour = 6, minute = 45),
        onSave: (Int, Int) -> Unit = { _, _ -> },
        onBack: () -> Unit = {},
    ) {
        compose.setContent {
            ApexAppTheme {
                AlarmEditorContent(
                    state = state,
                    onSave = onSave,
                    onBack = onBack,
                    onLabelChange = {},
                    onToggleDay = {},
                    is24Hour = true,
                )
            }
        }
    }

    @Test
    fun save_reportsPickedTime() {
        var saved: Pair<Int, Int>? = null
        show(onSave = { h, m -> saved = h to m })
        compose.onNodeWithText(str(R.string.editor_save)).performClick()
        assertEquals(6 to 45, saved)
    }

    @Test
    fun back_leavesWithoutSaving() {
        var saved = false
        var left = false
        show(onSave = { _, _ -> saved = true }, onBack = { left = true })
        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        assertEquals(true, left)
        assertEquals(false, saved)
    }

    @Test
    fun editing_showsEditTitle() {
        show(EditorUiState(isLoading = false, isNew = false))
        compose.onNodeWithText(str(R.string.editor_title_edit)).assertExists()
    }

    @Test
    fun typedTime_isSaved() {
        var saved: Pair<Int, Int>? = null
        show(onSave = { h, m -> saved = h to m })
        compose.onNodeWithTag(TimeWheelTags.Hours).performClick()
        compose.onNodeWithTag(TimeWheelTags.EntryField).performTextReplacement("2359")
        compose.onNodeWithText(str(R.string.time_entry_ok)).performClick()
        compose.onNodeWithText(str(R.string.editor_save)).performClick()
        assertEquals(23 to 59, saved)
    }
}
