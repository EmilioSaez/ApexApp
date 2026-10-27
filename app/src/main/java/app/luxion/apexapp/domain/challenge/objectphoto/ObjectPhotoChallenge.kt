package app.luxion.apexapp.domain.challenge.objectphoto

import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import app.luxion.apexapp.domain.challenge.qr.FrameObservation
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Etiqueta de ML Kit: [index] es su identificador estable y [text] el nombre que se muestra. */
@Serializable
data class RegisteredObject(val index: Int, val text: String)

/** Lectura y escritura de `ChallengeSpec.params` del reto de foto a un objeto. Solo se guarda la etiqueta. */
object ObjectPhotoParams {
    private val json = Json { ignoreUnknownKeys = true }

    fun paramsFor(obj: RegisteredObject): String = json.encodeToString(obj)

    /** El objeto registrado, o null si [params] está vacío o no es válido. */
    fun parse(params: String): RegisteredObject? =
        runCatching { json.decodeFromString<RegisteredObject>(params.ifBlank { return null }) }
            .getOrNull()
            ?.takeIf { it.text.isNotBlank() && it.index >= 0 }

    /** Verdadero si la alarma ya tiene un objeto; sin él no puede guardarse con este reto. */
    fun isRegistered(params: String): Boolean = parse(params) != null
}

/** Etiqueta detectada en un fotograma, con su [confidence] entre 0 y 1. */
data class DetectedLabel(val index: Int, val text: String, val confidence: Float)

/**
 * Reduce las etiquetas de un fotograma a la [FrameObservation] que consume `FrameLivenessChecker`: el valor
 * es el índice del objeto registrado si se ve con confianza suficiente, y null si no. El etiquetado no da
 * cuadro delimitador, así que la verificación de vida se apoya en la luminancia del fondo.
 */
object ObjectFrameAdapter {
    const val MIN_CONFIDENCE = 0.7f

    fun observe(
        tNanos: Long,
        labels: List<DetectedLabel>,
        expected: Int,
        backgroundLuma: Float,
        minConfidence: Float = MIN_CONFIDENCE,
    ): FrameObservation {
        val seen = labels.any { it.index == expected && it.confidence >= minConfidence }
        return FrameObservation(tNanos, if (seen) expected.toString() else null, backgroundLuma = backgroundLuma)
    }
}

/**
 * [expected] es null solo si la configuración llegó sin objeto (no debería: el editor impide guardarla).
 * En ese caso cualquier detección verificada supera el reto, para que la alarma nunca quede sin forma de apagarse.
 */
class ObjectPhotoInstance(
    override val level: Int,
    val expected: RegisteredObject?,
) : ChallengeInstance {
    override val type: ChallengeType = ObjectPhotoChallenge.TYPE
    override val prompt: String = "object"

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.ObjectVerified && (expected == null || answer.labelIndex == expected.index)
}

class ObjectPhotoChallenge @Inject constructor(
    private val capabilities: DeviceCapabilities,
) : Challenge {
    override val type: ChallengeType = TYPE

    override fun availability(): Availability = when {
        !capabilities.hasCamera -> Availability.Unavailable("no camera")
        !capabilities.hasCameraPermission -> Availability.Unavailable("camera permission missing")
        else -> Availability.Available
    }

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance =
        ObjectPhotoInstance(level, ObjectPhotoParams.parse(params))

    companion object {
        val TYPE = ChallengeType("object")
    }
}
