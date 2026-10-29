package app.luxion.apexapp.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconVariantTest {
    @Test
    fun eachAccentMapsToItself() {
        AppIconVariant.entries.forEach { assertEquals(it, AppIconVariant.nearestTo(it.accent)) }
    }

    @Test
    fun defaultThemeRedKeepsOriginalIcon() {
        assertEquals(AppIconVariant.DEFAULT, AppIconVariant.nearestTo(CustomThemeColors.Default.primary))
    }

    @Test
    fun quantizesToNearestHue() {
        assertEquals(AppIconVariant.BLUE, AppIconVariant.nearestTo(0xFF0A3CFF.toInt()))
        assertEquals(AppIconVariant.GREEN, AppIconVariant.nearestTo(0xFF00AA20.toInt()))
        assertEquals(AppIconVariant.PURPLE, AppIconVariant.nearestTo(0xFF8030D0.toInt()))
        // Magenta intenso: el tono 300° cae entre morado y rosa; manda el más cercano.
        assertEquals(AppIconVariant.PINK, AppIconVariant.nearestTo(0xFFFF0090.toInt()))
    }

    @Test
    fun hueWrapsAroundRed() {
        assertEquals(AppIconVariant.DEFAULT, AppIconVariant.nearestTo(0xFFFF0010.toInt()))
        assertEquals(AppIconVariant.DEFAULT, AppIconVariant.nearestTo(0xFFFF0000.toInt()))
    }

    @Test
    fun colorsWithoutHueAreNeutral() {
        listOf(0xFFFFFFFF, 0xFF000000, 0xFF808080, 0xFF1A0000, 0xFFE8E0E0).forEach {
            assertEquals(AppIconVariant.NEUTRAL, AppIconVariant.nearestTo(it.toInt()))
        }
    }
}

class ApplyAppIconTest {
    private class FakeSettings : SettingsRepository {
        var icon = AppIconVariant.DEFAULT
        override fun observeThemeMode() = throw NotImplementedError()
        override suspend fun setThemeMode(mode: ThemeMode) = throw NotImplementedError()
        override fun observeCustomThemeColors() = throw NotImplementedError()
        override suspend fun setCustomThemeColors(colors: CustomThemeColors) = throw NotImplementedError()
        override fun observeAppIcon() = throw NotImplementedError()
        override suspend fun setAppIcon(variant: AppIconVariant) { icon = variant }
    }

    @Test
    fun appliesAndPersists() = runTest {
        val settings = FakeSettings()
        val applied = mutableListOf<AppIconVariant>()
        ApplyAppIcon(settings, object : AppIconSwitcher {
            override fun apply(variant: AppIconVariant) { applied += variant }
        })(AppIconVariant.TEAL)
        assertEquals(listOf(AppIconVariant.TEAL), applied)
        assertEquals(AppIconVariant.TEAL, settings.icon)
    }

    @Test
    fun doesNotPersistWhenSwitchFails() = runTest {
        val settings = FakeSettings()
        val failing = object : AppIconSwitcher {
            override fun apply(variant: AppIconVariant) = throw IllegalStateException("sin permiso")
        }
        val result = runCatching { ApplyAppIcon(settings, failing)(AppIconVariant.PINK) }
        assertTrue(result.isFailure)
        assertEquals(AppIconVariant.DEFAULT, settings.icon)
    }
}
