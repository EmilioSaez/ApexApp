package app.luxion.apexapp.ui.challenge.qr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.qr.QrParams
import app.luxion.apexapp.scanner.BarcodeScanner
import app.luxion.apexapp.scanner.CameraPermissionGate
import app.luxion.apexapp.ui.theme.ApexTheme
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Registro del código de una alarma. [currentParams] es el `ChallengeSpec.params` actual del tipo QR (vacío
 * si aún no hay código); al escanear uno nuevo se invoca [onParamsChange] con los params que lo sustituyen.
 * Solo se guarda el hash del valor, nunca el código en claro.
 */
@Composable
fun QrCodeRegistration(
    currentParams: String,
    onParamsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scanning by remember { mutableStateOf(false) }
    val registered = remember(currentParams) { QrParams.parse(currentParams) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        Text(
            text = when {
                scanning -> stringResource(R.string.qr_register_scanning)
                registered != null -> stringResource(R.string.qr_register_done, formatName(registered.format))
                else -> stringResource(R.string.qr_register_required)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (registered == null && !scanning) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (scanning) {
            CameraPermissionGate(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                BarcodeScanner(
                    onScanned = { result ->
                        onParamsChange(QrParams.paramsFor(result.value, result.format))
                        scanning = false
                    },
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    onError = { scanning = false },
                )
            }
        } else {
            OutlinedButton(onClick = { scanning = true }, shape = MaterialTheme.shapes.small) {
                Text(
                    stringResource(
                        if (registered == null) R.string.qr_register_action else R.string.qr_register_replace,
                    ),
                )
            }
            Text(
                stringResource(R.string.qr_register_placement_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> stringResource(R.string.qr_format_qr)
    else -> stringResource(R.string.qr_format_barcode)
}
