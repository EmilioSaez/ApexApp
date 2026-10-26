package app.luxion.apexapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.ui.theme.ApexTheme

/** Única acción destacada de una pantalla: relleno con el acento. */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = MaterialTheme.shapes.small,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = ApexTheme.spacing.xl, vertical = ApexTheme.spacing.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(ApexTheme.spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Hora con cifras tabulares. El tamaño sale de la tipografía del tema. */
@Composable
fun TimeDisplay(
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    Text(
        text = "%02d:%02d".format(hour, minute),
        modifier = modifier,
        style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** Cabecera de sección: rótulo en mayúsculas con tracking, anclado a la izquierda y subordinado al contenido. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, horizontalPadding: Dp = ApexTheme.spacing.screen) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = ApexTheme.spacing.md),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Estado vacío con mensaje y, opcionalmente, una pista de qué hacer. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, hint: String? = null) {
    Column(
        modifier = modifier.padding(horizontal = ApexTheme.spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
    ) {
        Text(message, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
