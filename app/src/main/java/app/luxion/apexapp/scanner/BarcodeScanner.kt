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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/** Código leído: su [value] y el [format] de `Barcode.FORMAT_*`. */
data class ScanResult(val value: String, val format: Int)

/**
 * Vista de cámara que lee códigos QR y de barras sin conexión (importar alarmas):
 * [onScanned] recibe cada código nuevo (se ignora el mismo valor repetido en fotogramas consecutivos).
 * El llamante debe tener concedido el permiso `CAMERA` (ver [CameraPermissionGate]); la cámara se
 * enlaza al ciclo de vida y se libera al salir de la composición.
 */
@Composable
fun BarcodeScanner(
    onScanned: (ScanResult) -> Unit,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned = rememberUpdatedState(onScanned)
    val currentOnError = rememberUpdatedState(onError)

    val executor = androidx.compose.runtime.remember { Executors.newSingleThreadExecutor() }
    val scanner = androidx.compose.runtime.remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build(),
        )
    }
    val previewView = androidx.compose.runtime.remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView: se recorta y se desplaza con el contenido; la SurfaceView se pintaba fuera de su sitio dentro de un scroll.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
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
                    analyze(image, scanner) { result ->
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

    // FILL_CENTER escala la imagen más allá del hueco: hay que recortarla a los límites de la vista.
    AndroidView(factory = { previewView }, modifier = modifier.clipToBounds())
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyze(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    deliver: (ScanResult?) -> Unit,
) {
    val media = imageProxy.image
    if (media == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }
            deliver(barcode?.rawValue?.let { ScanResult(it, barcode.format) })
        }
        .addOnCompleteListener { imageProxy.close() }
}
