package app.luxion.apexapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.luxion.apexapp.domain.CustomThemeColors

@Composable
fun ApexAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Paleta propia por defecto; el color dinámico (Android 12+) queda como opción futura.
    dynamicColor: Boolean = false,
    /** Si no es nulo, sustituye primary y secondary de la paleta por un esquema generado desde estos colores. */
    customColors: CustomThemeColors? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        customColors != null -> customColorScheme(Color(customColors.primary), Color(customColors.secondary), darkTheme)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
