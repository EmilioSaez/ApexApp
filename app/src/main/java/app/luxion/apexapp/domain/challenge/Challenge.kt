package app.luxion.apexapp.domain.challenge

import kotlin.random.Random

/** Identificador estable de un tipo de reto; es el mismo `String` que persiste `ChallengeSpec.type`. */
@JvmInline
value class ChallengeType(val id: String) {
    companion object {
        val MATH = ChallengeType("math")
    }
}

/** Respuesta del usuario. Los retos QR y de sensores añaden sus propias variantes. */
sealed interface ChallengeAnswer {
    data class Text(val value: String) : ChallengeAnswer

    /** Un reto de movimiento (agitar, pasos, levantarse) verificó localmente su objetivo. */
    data class MotionVerified(val type: ChallengeType) : ChallengeAnswer

    /** El reto QR verificó en vivo un código; [codeHash] es el SHA-256 en hexadecimal de su valor. */
    data class ScanVerified(val codeHash: String) : ChallengeAnswer

    /** Casillas tocadas por el usuario, en orden, en el reto de memoria. */
    data class Pattern(val type: ChallengeType, val taps: List<Int>) : ChallengeAnswer
}

sealed interface Availability {
    data object Available : Availability
    data class Unavailable(val reason: String) : Availability
}

/** Fábrica de instancias de un tipo de reto. No guarda estado: el estado vive en [ChallengeInstance]. */
interface Challenge {
    val type: ChallengeType

    /** Si no es [Availability.Available] la sesión recurre a la reserva. */
    fun availability(): Availability = Availability.Available

    /** [level] va de 1 a 5; [params] es el JSON opaco de `ChallengeSpec.params` de este tipo. */
    fun generate(level: Int, params: String, random: Random): ChallengeInstance
}

interface ChallengeInstance {
    val type: ChallengeType
    val level: Int
    val prompt: String

    /** Si es verdadero, un fallo conserva esta instancia en vez de regenerar el reto (p. ej. la misma secuencia). */
    val keepOnFailure: Boolean get() = false

    fun check(answer: ChallengeAnswer): Boolean
}
