package app.luxion.apexapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.ui.theme.ApexAppTheme

@Composable
private fun Gallery() {
    Surface {
        Column(Modifier.padding(16.dp)) {
            SectionHeader("Apariencia")
            TimeDisplay(hour = 6, minute = 45, large = true)
            PrimaryAction(text = "Guardar alarma", onClick = {})
            EmptyState(message = "No hay alarmas todavía", hint = "Crea una alarma para empezar.")
        }
    }
}

@Preview(name = "Claro")
@Composable
private fun GalleryLight() = ApexAppTheme(darkTheme = false) { Gallery() }

@Preview(name = "Oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GalleryDark() = ApexAppTheme(darkTheme = true) { Gallery() }

@Preview(name = "Fuente grande", fontScale = 1.5f)
@Composable
private fun GalleryLargeFont() = ApexAppTheme(darkTheme = false) { Gallery() }
