package app.luxion.apexapp.ui.awake

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import app.luxion.apexapp.R
import app.luxion.apexapp.alarm.AwakeVerificationPolicy
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

private const val MILLIS_PER_MINUTE = 60_000L

/** Sección del editor con la opción de verificar que el usuario sigue despierto, tras Anti-trampas. */
class AwakeVerificationEditorSection @Inject constructor() : EditorSection {
    override val order: Int = ORDER

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) {
        // Se lee en cada composición: al volver de Ajustes tras conceder el permiso el aviso desaparece.
        val notificationsEnabled = NotificationManagerCompat.from(LocalContext.current).areNotificationsEnabled()
        AwakeVerificationSection(
            enabled = draft.awakeVerification,
            onChange = { onChange(draft.copy(awakeVerification = it)) },
            notificationsEnabled = notificationsEnabled,
        )
    }

    private companion object {
        const val ORDER = 350
    }
}

@Composable
fun AwakeVerificationSection(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    notificationsEnabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        SectionHeader(stringResource(R.string.awake_verification_section), horizontalPadding = 0.dp)
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.awake_verification_option),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = enabled, onCheckedChange = null)
        }
        Text(
            stringResource(
                R.string.awake_verification_hint,
                (AwakeVerificationPolicy.PROMPT_DELAY_MILLIS / MILLIS_PER_MINUTE).toInt(),
                (AwakeVerificationPolicy.CONFIRM_WINDOW_MILLIS / MILLIS_PER_MINUTE).toInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (enabled && !notificationsEnabled) {
            Text(
                stringResource(R.string.awake_verification_missing_notifications),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Composable
private fun AwakeLight() = ApexAppTheme(darkTheme = false) {
    Surface { AwakeVerificationSection(true, {}, Modifier.padding(ApexTheme.spacing.screen), notificationsEnabled = false) }
}

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AwakeDark() = ApexAppTheme(darkTheme = true) {
    Surface { AwakeVerificationSection(true, {}, Modifier.padding(ApexTheme.spacing.screen)) }
}

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun AwakeLargeFont() = ApexAppTheme(darkTheme = false) {
    Surface { AwakeVerificationSection(false, {}, Modifier.padding(ApexTheme.spacing.screen)) }
}
