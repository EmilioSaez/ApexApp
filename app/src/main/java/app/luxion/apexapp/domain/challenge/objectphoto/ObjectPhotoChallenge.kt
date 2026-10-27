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

@Serializable
private data class ObjectList(val objects: List<RegisteredObject> = emptyList())

/**
 * Lectura y escritura de `ChallengeSpec.params` del reto de foto a un objeto: la lista de objetos entre los
 * que se elige uno por reto. Solo se guardan etiquetas.
 */
object ObjectPhotoParams {
    private val json = Json { ignoreUnknownKeys = true }

    fun paramsFor(objects: List<RegisteredObject>): String = json.encodeToString(ObjectList(objects.distinctBy { it.index }))

    /** Los objetos registrados (sin repetir ni inválidos); vacío si [params] está vacío o no es válido. */
    fun parse(params: String): List<RegisteredObject> =
        runCatching { json.decodeFromString<ObjectList>(params.ifBlank { return emptyList() }) }
            .getOrNull()
            ?.objects
            ?.filter { it.text.isNotBlank() && it.index >= 0 }
            ?.distinctBy { it.index }
            .orEmpty()

    /** [params] con [obj] añadido; si ya estaba, no cambia. */
    fun added(params: String, obj: RegisteredObject): String = paramsFor(parse(params) + obj)

    /** [params] sin el objeto de índice [index]. */
    fun removed(params: String, index: Int): String = paramsFor(parse(params).filterNot { it.index == index })

    /** Verdadero si la alarma ya tiene algún objeto; sin él no puede guardarse con este reto. */
    fun isRegistered(params: String): Boolean = parse(params).isNotEmpty()
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
        generate(level, params, random, emptyList())

    /** Elige un objeto que no se haya pedido ya en la sesión; solo repite cuando se agotan los registrados. */
    override fun generate(
        level: Int,
        params: String,
        random: Random,
        solved: List<ChallengeInstance>,
    ): ChallengeInstance {
        val objects = ObjectPhotoParams.parse(params)
        val used = solved.mapNotNull { (it as? ObjectPhotoInstance)?.expected?.index }.toSet()
        val candidates = objects.filter { it.index !in used }.ifEmpty { objects }
        return ObjectPhotoInstance(level, candidates.randomOrNull(random))
    }

    companion object {
        val TYPE = ChallengeType("object")
    }
}
