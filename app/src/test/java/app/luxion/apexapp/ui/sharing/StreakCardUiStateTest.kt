package app.luxion.apexapp.ui.sharing

import app.luxion.apexapp.domain.Streak
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCardUiStateTest {
    @Test
    fun loading_hasNothingToShare() {
        assertFalse(StreakCardUiState().hasAchievements)
    }

    @Test
    fun noDaysYet_hasNothingToShare() {
        assertFalse(StreakCardUiState(Streak(0, 0)).hasAchievements)
    }

    @Test
    fun brokenStreakWithAPastBest_stillHasAchievements() {
        assertTrue(StreakCardUiState(Streak(0, 4)).hasAchievements)
    }

    @Test
    fun activeStreak_hasAchievements() {
        assertTrue(StreakCardUiState(Streak(3, 3)).hasAchievements)
    }
}
