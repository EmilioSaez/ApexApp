package app.luxion.apexapp.domain.challenge.qr

import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Código registrado en una alarma: formato de `Barcode.FORMAT_*` y SHA-256 hexadecimal de su valor. */
@Serializable
data class RegisteredCode(val format: Int, val hash: String)

/** Lectura y escritura de `ChallengeSpec.params` del reto QR. El valor del código nunca se guarda. */
object QrParams {
    private val json = Json { ignoreUnknownKeys = true }

    fun hashOf(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** Params para una alarma cuyo código es [value]; sustituye a cualquier código anterior. */
    fun paramsFor(value: String, format: Int): String =
        json.encodeToString(RegisteredCode(format, hashOf(value)))

    /** El código registrado, o null si [params] está vacío o no es válido. */
    fun parse(params: String): RegisteredCode? =
        runCatching { json.decodeFromString<RegisteredCode>(params.ifBlank { return null }) }
            .getOrNull()
            ?.takeIf { it.hash.isNotBlank() }

    /** Verdadero si la alarma ya tiene un código; sin él no puede guardarse con reto QR. */
    fun isRegistered(params: String): Boolean = parse(params) != null
}

/**
 * [expected] es null solo si la configuración llegó sin código (no debería: el editor impide guardarla).
 * En ese caso cualquier escaneo en vivo supera el reto, para que la alarma nunca quede sin forma de apagarse.
 */
class QrInstance(
    override val level: Int,
    val expected: RegisteredCode?,
) : ChallengeInstance {
    override val type: ChallengeType = QrChallenge.TYPE
    override val prompt: String = "qr"

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.ScanVerified && (expected == null || answer.codeHash == expected.hash)
}

class QrChallenge @Inject constructor(
    private val capabilities: DeviceCapabilities,
) : Challenge {
    override val type: ChallengeType = TYPE

    override fun availability(): Availability = when {
        !capabilities.hasCamera -> Availability.Unavailable("no camera")
        !capabilities.hasCameraPermission -> Availability.Unavailable("camera permission missing")
        else -> Availability.Available
    }

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        return QrInstance(level, QrParams.parse(params))
    }

    companion object {
        val TYPE = ChallengeType("qr")
    }
}
