package app.luxion.apexapp.domain

/** Variante sobre la que se aplican los colores del tema personalizado. */
enum class CustomBase {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

/** Colores base del tema personalizado, en ARGB, y la [base] clara u oscura. Los tonos finales y su contraste los resuelve `ui/theme`. */
data class CustomThemeColors(val primary: Int, val secondary: Int, val base: CustomBase = CustomBase.SYSTEM) {
    companion object {
        val Default = CustomThemeColors(primary = 0xFFC4161C.toInt(), secondary = 0xFF4A6FA5.toInt())
    }
}
