package app.luxion.apexapp.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.luxion.apexapp.R
import app.luxion.apexapp.ui.theme.ApexTheme

/** Estado del permiso de cámara tal como lo ve la interfaz. */
enum class CameraAccess { GRANTED, ASK, BLOCKED }

/**
 * Muestra [content] si el permiso de cámara está concedido. Si no, lo solicita (una vez) y, si el usuario
 * lo denegó de forma permanente, explica el motivo y ofrece abrir los ajustes de la aplicación.
 */
@Composable
fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(currentAccess(context, askedBefore = false)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        access = if (granted) CameraAccess.GRANTED else currentAccess(context, askedBefore = true)
    }
    // El usuario puede conceder o revocar el permiso en los ajustes del sistema y volver a la app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            access = CameraAccess.GRANTED
        } else if (access == CameraAccess.GRANTED) {
            access = currentAccess(context, askedBefore = false)
        }
    }

    when (access) {
        CameraAccess.GRANTED -> content()
        CameraAccess.ASK -> PermissionPrompt(
            message = stringResource(R.string.scanner_permission_rationale),
            action = stringResource(R.string.permission_grant),
            onAction = { launcher.launch(Manifest.permission.CAMERA) },
            modifier = modifier,
        )
        CameraAccess.BLOCKED -> PermissionPrompt(
            message = stringResource(R.string.scanner_permission_blocked),
            action = stringResource(R.string.permission_open_settings),
            onAction = { context.startActivity(appSettingsIntent(context)) },
            modifier = modifier,
        )
    }
}

@Composable
private fun PermissionPrompt(message: String, action: String, onAction: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(ApexTheme.spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg, Alignment.CenterVertically),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(onClick = onAction, shape = MaterialTheme.shapes.small) { Text(action) }
    }
}

/**
 * Sin permiso y tras haberlo pedido, Android deja de mostrar el diálogo cuando
 * `shouldShowRequestPermissionRationale` es falso: se considera bloqueado.
 */
private fun currentAccess(context: Context, askedBefore: Boolean): CameraAccess {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        return CameraAccess.GRANTED
    }
    if (!askedBefore) return CameraAccess.ASK
    val activity = context as? Activity ?: return CameraAccess.BLOCKED
    val canAskAgain = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.CAMERA,
    )
    return if (canAskAgain) CameraAccess.ASK else CameraAccess.BLOCKED
}

private fun appSettingsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
