package app.luxion.apexapp.domain

/** Colores base del tema personalizado, en ARGB. Los tonos finales y su contraste los resuelve `ui/theme`. */
data class CustomThemeColors(val primary: Int, val secondary: Int) {
    companion object {
        val Default = CustomThemeColors(primary = 0xFFC4161C.toInt(), secondary = 0xFF4A6FA5.toInt())
    }
}
