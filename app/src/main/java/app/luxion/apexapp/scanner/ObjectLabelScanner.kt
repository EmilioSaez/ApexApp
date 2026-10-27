package app.luxion.apexapp.scanner

import androidx.camera.core.ImageProxy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.luxion.apexapp.domain.challenge.objectphoto.DetectedLabel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/** Etiquetas de un fotograma: instante [tNanos] y luminancia media [backgroundLuma] del anillo exterior. */
data class LabelFrame(val tNanos: Long, val labels: List<DetectedLabel>, val backgroundLuma: Float)

/**
 * Vista de cámara que etiqueta cada fotograma con el modelo base de ML Kit, sin conexión y sin guardar
 * imágenes. Componente compartido sin lógica de reto: [onFrame] recibe las etiquetas de cada fotograma.
 * El llamante debe tener concedido el permiso `CAMERA` (ver [CameraPermissionGate]).
 */
@Composable
fun ObjectLabelScanner(
    onFrame: (LabelFrame) -> Unit,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit = {},
) {
    val currentOnFrame = rememberUpdatedState(onFrame)
    val labeler = remember { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) {
        onDispose { labeler.close() }
    }

    CameraAnalysisView(
        onImage = { image -> label(image, labeler) { currentOnFrame.value(it) } },
        onError = onError,
        modifier = modifier,
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun label(imageProxy: ImageProxy, labeler: ImageLabeler, deliver: (LabelFrame) -> Unit) {
    val media = imageProxy.image
    if (media == null) {
        imageProxy.close()
        return
    }
    val timestampNanos = imageProxy.imageInfo.timestamp
    val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
    labeler.process(input)
        .addOnSuccessListener { labels ->
            deliver(
                LabelFrame(
                    tNanos = timestampNanos,
                    labels = labels.map { DetectedLabel(it.index, it.text, it.confidence) },
                    backgroundLuma = backgroundLuma(imageProxy),
                ),
            )
        }
        .addOnCompleteListener { imageProxy.close() }
}
