package app.luxion.apexapp.ui.challenge.motion

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import app.luxion.apexapp.R
import app.luxion.apexapp.ui.theme.ApexTheme

/**
 * Configuración del reto de pasos: pide `ACTIVITY_RECOGNITION` y muestra [content] cuando está concedido.
 * Si el dispositivo no tiene contador de pasos ([hasStepSensor] falso) el reto no se ofrece: se muestra un aviso
 * y [content] no se compone.
 */
@Composable
fun ActivityRecognitionGate(
    hasStepSensor: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!hasStepSensor) {
        Text(
            stringResource(R.string.steps_unavailable),
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val context = LocalContext.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED
    var hasPermission by remember { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    if (hasPermission) {
        content()
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
            Text(stringResource(R.string.steps_permission_rationale), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                shape = MaterialTheme.shapes.small,
            ) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}
