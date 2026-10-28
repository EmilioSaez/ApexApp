package app.luxion.apexapp.domain

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, CUSTOM;

    /** En [CUSTOM] la variante la decide [CustomThemeColors.base]; aquí sigue al sistema. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM, CUSTOM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}
