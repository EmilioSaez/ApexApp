package app.luxion.apexapp.ui.sharing

import android.net.Uri
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.sharing.SharedFileWriter
import app.luxion.apexapp.sharing.StreakStatsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreakCardViewModelTest {
    private val streak = MutableStateFlow(Streak(0, 0))
    private val files = object : SharedFileWriter {
        override fun write(name: String, bytes: ByteArray): Uri = error("unused")
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun followsTheStreakAsItChanges() = runTest {
        val viewModel = StreakCardViewModel(StreakStatsSource { streak }, files)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }

        assertFalse(viewModel.uiState.value.hasAchievements)
        streak.value = Streak(2, 5)
        assertEquals(Streak(2, 5), viewModel.uiState.value.streak)
        assertTrue(viewModel.uiState.value.hasAchievements)
    }
}
