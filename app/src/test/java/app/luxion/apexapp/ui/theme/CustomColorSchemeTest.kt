package app.luxion.apexapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomColorSchemeTest {
    private val seeds = listOf(
        Color.White, Color.Black, Color.Yellow, Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFC4161C),
    )

    @Test
    fun accentsAndTheirContentMeetAaContrast() {
        for (dark in listOf(false, true)) {
            val surface = (if (dark) DarkColorScheme else LightColorScheme).surface
            for (seed in seeds) {
                val scheme = customColorScheme(seed, seed, dark)
                assertTrue("primary $seed dark=$dark", contrast(scheme.primary, surface) >= 4.5f)
                assertTrue("onPrimary $seed dark=$dark", contrast(scheme.onPrimary, scheme.primary) >= 4.5f)
                assertTrue("onPrimaryContainer $seed dark=$dark", contrast(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f)
                assertTrue("onSecondary $seed dark=$dark", contrast(scheme.onSecondary, scheme.secondary) >= 4.5f)
            }
        }
    }
}
