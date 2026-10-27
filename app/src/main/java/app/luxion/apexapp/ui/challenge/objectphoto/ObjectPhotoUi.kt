package app.luxion.apexapp.ui.challenge.objectphoto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.objectphoto.DetectedLabel
import app.luxion.apexapp.domain.challenge.objectphoto.ObjectFrameAdapter
import app.luxion.apexapp.domain.challenge.objectphoto.ObjectPhotoChallenge
import app.luxion.apexapp.domain.challenge.objectphoto.ObjectPhotoInstance
import app.luxion.apexapp.domain.challenge.objectphoto.ObjectPhotoParams
import app.luxion.apexapp.domain.challenge.objectphoto.RegisteredObject
import app.luxion.apexapp.domain.challenge.qr.FrameLivenessChecker
import app.luxion.apexapp.domain.challenge.qr.LivenessVerdict
import app.luxion.apexapp.scanner.CameraPermissionGate
import app.luxion.apexapp.scanner.ObjectLabelScanner
import app.luxion.apexapp.ui.challenge.ChallengeParamsEditor
import app.luxion.apexapp.ui.challenge.ChallengeRenderer
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/** Etiquetas que se ofrecen al registrar el objeto: las de mayor confianza. */
private const val MAX_OFFERED_LABELS = 5

/**
 * Reto de foto a un objeto: etiqueta cada fotograma y solo entrega [ChallengeAnswer.ObjectVerified] cuando el
 * objeto registrado se mantiene en el encuadre y la escena pasa la verificación de "vida".
 */
class ObjectPhotoRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = ObjectPhotoChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_object)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        val expected = (instance as ObjectPhotoInstance).expected
        // Una instancia nueva (tras un fallo) reinicia la verificación.
        val checker = remember(instance) { FrameLivenessChecker() }
        var rejection by remember(instance) { mutableStateOf<LivenessVerdict.Reason?>(null) }
        var progress by remember(instance) { mutableStateOf(0f) }

        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md),
        ) {
            Text(
                stringResource(R.string.challenge_object_prompt, expected?.text.orEmpty()),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            CameraPermissionGate(modifier = Modifier.fillMaxWidth().height(ApexTheme.spacing.xxxl * 7)) {
                ObjectLabelScanner(
                    onFrame = { frame ->
                        // Sin objeto registrado (no debería ocurrir) basta con ver cualquier etiqueta.
                        val target = expected?.index ?: frame.labels.maxByOrNull { it.confidence }?.index ?: return@ObjectLabelScanner
                        val observation = ObjectFrameAdapter.observe(frame.tNanos, frame.labels, target, frame.backgroundLuma)
                        when (val verdict = checker.onFrame(observation)) {
                            is LivenessVerdict.Collecting -> progress = verdict.progress
                            is LivenessVerdict.Rejected -> rejection = verdict.reason
                            is LivenessVerdict.Accepted -> {
                                rejection = null
                                onAnswer(ChallengeAnswer.ObjectVerified(verdict.value.toInt()))
                            }
                        }
                    },
                    onError = { onUnavailable("camera error: ${it.message}") },
                    modifier = Modifier.fillMaxWidth().height(ApexTheme.spacing.xxxl * 7),
                )
            }
            val message = when {
                wrong -> R.string.challenge_object_wrong
                rejection == LivenessVerdict.Reason.SCREEN -> R.string.challenge_object_rejected_screen
                rejection == LivenessVerdict.Reason.STATIC_IMAGE -> R.string.challenge_object_rejected_static
                progress > 0f -> R.string.challenge_object_hold
                else -> null
            }
            if (message != null) {
                Text(
                    stringResource(message),
                    modifier = Modifier.padding(horizontal = ApexTheme.spacing.screen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message == R.string.challenge_object_hold) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

/** Registro del objeto del reto; no deja guardar la alarma mientras no haya uno. */
class ObjectPhotoParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = ObjectPhotoChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_object), style = MaterialTheme.typography.titleSmall)
            ObjectRegistration(currentParams = params, onParamsChange = onChange)
        }
    }

    override fun isValid(params: String): Boolean = ObjectPhotoParams.isRegistered(params)
}

/**
 * Vista previa de cámara con las etiquetas que se reconocen ahora mismo; al elegir una se invoca
 * [onParamsChange] con los params que sustituyen al objeto anterior. Solo se guarda la etiqueta.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ObjectRegistration(currentParams: String, onParamsChange: (String) -> Unit) {
    var scanning by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf<List<DetectedLabel>>(emptyList()) }
    val registered = remember(currentParams) { ObjectPhotoParams.parse(currentParams) }

    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        Text(
            text = when {
                scanning -> stringResource(R.string.object_register_scanning)
                registered != null -> stringResource(R.string.object_register_done, registered.text)
                else -> stringResource(R.string.object_register_required)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (registered == null && !scanning) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (scanning) {
            CameraPermissionGate(modifier = Modifier.fillMaxWidth().height(ApexTheme.spacing.xxxl * 6)) {
                ObjectLabelScanner(
                    onFrame = { frame ->
                        detected = frame.labels.sortedByDescending { it.confidence }.take(MAX_OFFERED_LABELS)
                    },
                    modifier = Modifier.fillMaxWidth().height(ApexTheme.spacing.xxxl * 6),
                    onError = { scanning = false },
                )
            }
            if (detected.isEmpty()) {
                Text(
                    stringResource(R.string.object_register_none_detected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
                ) {
                    detected.forEach { label ->
                        AssistChip(
                            onClick = {
                                onParamsChange(ObjectPhotoParams.paramsFor(RegisteredObject(label.index, label.text)))
                                scanning = false
                            },
                            label = { Text(label.text) },
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.object_register_generic_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { scanning = false }, shape = MaterialTheme.shapes.small) {
                Text(stringResource(R.string.object_register_cancel))
            }
        } else {
            OutlinedButton(
                onClick = {
                    detected = emptyList()
                    scanning = true
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    stringResource(
                        if (registered == null) R.string.object_register_action else R.string.object_register_replace,
                    ),
                )
            }
            Text(
                stringResource(R.string.object_register_placement_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
