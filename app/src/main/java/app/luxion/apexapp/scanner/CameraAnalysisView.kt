package app.luxion.apexapp.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

/**
 * Vista previa de la cámara trasera con un flujo de fotogramas para analizar. Componente compartido por los
 * lectores de códigos y de objetos: [onImage] recibe cada fotograma y debe cerrarlo (`ImageProxy.close`).
 * El llamante debe tener concedido el permiso `CAMERA` (ver [CameraPermissionGate]); la cámara se enlaza al
 * ciclo de vida y se libera al salir de la composición.
 */
@Composable
fun CameraAnalysisView(
    onImage: (ImageProxy) -> Unit,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnImage = rememberUpdatedState(onImage)
    val currentOnError = rememberUpdatedState(onError)

    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView: se recorta y se desplaza con el contenido; la SurfaceView se pintaba fuera de su sitio dentro de un scroll.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image -> currentOnImage.value(image) }
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
        onDispose { executor.shutdown() }
    }

    // FILL_CENTER escala la imagen más allá del hueco: hay que recortarla a los límites de la vista.
    AndroidView(factory = { previewView }, modifier = modifier.clipToBounds())
}
