package app.luxion.apexapp.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.luxion.apexapp.domain.challenge.qr.FrameObservation
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned = rememberUpdatedState(onScanned)
    val currentOnFrame = rememberUpdatedState(onFrame)
    val currentOnError = rememberUpdatedState(onError)

    val executor = androidx.compose.runtime.remember { Executors.newSingleThreadExecutor() }
    val scanner = androidx.compose.runtime.remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build(),
        )
    }
    val previewView = androidx.compose.runtime.remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var lastValue: String? = null
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    analyze(image, scanner, currentOnFrame.value != null) { observation, result ->
                        currentOnFrame.value?.invoke(observation)
                        val value = result?.value
                        if (value != lastValue) {
                            lastValue = value
                            if (result != null) currentOnScanned.value(result)
                        }
                    }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (t: Throwable) {
                currentOnError.value(t)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
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
private fun backgroundLuma(image: ImageProxy): Float {
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
