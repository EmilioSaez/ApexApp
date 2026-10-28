package app.luxion.apexapp.domain

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, CUSTOM;

    /** En [CUSTOM] la variante clara/oscura sigue al sistema; los dos colores elegidos se aplican sobre ella. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM, CUSTOM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}
