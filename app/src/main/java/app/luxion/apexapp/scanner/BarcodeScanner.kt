package app.luxion.apexapp.scanner

import androidx.camera.core.ImageProxy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.luxion.apexapp.domain.challenge.qr.FrameObservation
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/** Código leído: su [value] y el [format] de `Barcode.FORMAT_*`. */
data class ScanResult(val value: String, val format: Int)

/**
 * Vista de cámara que lee códigos QR y de barras sin conexión. Componente compartido sin lógica de reto:
 * [onScanned] recibe cada código nuevo (se ignora el mismo valor repetido en fotogramas consecutivos).
 * [onFrame] es opcional y entrega una observación por fotograma para verificaciones de "vida" (el reto QR).
 * El llamante debe tener concedido el permiso `CAMERA` (ver [CameraPermissionGate]); la cámara se
 * enlaza al ciclo de vida y se libera al salir de la composición.
 */
@Composable
fun BarcodeScanner(
    onScanned: (ScanResult) -> Unit,
    modifier: Modifier = Modifier,
    onFrame: ((FrameObservation) -> Unit)? = null,
    onError: (Throwable) -> Unit = {},
) {
    val currentOnScanned = rememberUpdatedState(onScanned)
    val currentOnFrame = rememberUpdatedState(onFrame)
    val scanner = androidx.compose.runtime.remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build(),
        )
    }
    // Se escribe desde el hilo del analizador: no es estado de Compose.
    val lastValue = androidx.compose.runtime.remember { java.util.concurrent.atomic.AtomicReference<String?>(null) }
    DisposableEffect(Unit) {
        onDispose { scanner.close() }
    }

    CameraAnalysisView(
        onImage = { image ->
            analyze(image, scanner, currentOnFrame.value != null) { observation, result ->
                currentOnFrame.value?.invoke(observation)
                val value = result?.value
                if (value != lastValue.get()) {
                    lastValue.set(value)
                    if (result != null) currentOnScanned.value(result)
                }
            }
        },
        onError = onError,
        modifier = modifier,
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyze(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    wantFrames: Boolean,
    deliver: (FrameObservation, ScanResult?) -> Unit,
) {
    val media = imageProxy.image
    if (media == null) {
        imageProxy.close()
        return
    }
    val timestampNanos = imageProxy.imageInfo.timestamp
    val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }
            val box = barcode?.boundingBox
            val result = barcode?.rawValue?.let { ScanResult(it, barcode.format) }
            val luma = if (wantFrames) backgroundLuma(imageProxy) else 0f
            deliver(
                FrameObservation(
                    tNanos = timestampNanos,
                    value = result?.value,
                    centerX = box?.exactCenterX() ?: 0f,
                    centerY = box?.exactCenterY() ?: 0f,
                    backgroundLuma = luma,
                ),
                result,
            )
        }
        .addOnCompleteListener { imageProxy.close() }
}

/**
 * Luminancia media (0..255) del anillo exterior del plano Y, muestreando una malla gruesa. El código suele
 * ocupar el centro del encuadre, y el anillo no depende de la rotación del sensor.
 */
internal fun backgroundLuma(image: ImageProxy): Float {
    val plane = image.planes.firstOrNull() ?: return 0f
    val buffer = plane.buffer
    val width = image.width
    val height = image.height
    var sum = 0L
    var count = 0
    val step = 16
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val inCenter = x in (width / 4)..(width * 3 / 4) && y in (height / 4)..(height * 3 / 4)
            if (!inCenter) {
                val index = y * plane.rowStride + x * plane.pixelStride
                if (index < buffer.limit()) {
                    sum += buffer.get(index).toInt() and 0xFF
                    count++
                }
            }
            x += step
        }
        y += step
    }
    return if (count == 0) 0f else sum.toFloat() / count
}
