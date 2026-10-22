package app.luxion.apexapp.domain.challenge.sensor

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Parámetros de un reto de movimiento, tal como viajan en `ChallengeSpec.params`. */
@Serializable
data class MotionParams(val target: Int = 0)

/** Límites y valor por defecto del objetivo de cada reto de movimiento. */
enum class MotionKind(val typeId: String, val min: Int, val max: Int, val default: Int) {
    SHAKE("shake", min = 10, max = 100, default = 20),
    STEPS("steps", min = 10, max = 200, default = 30),

    /** El objetivo es la duración mínima de la marcha, en segundos. */
    STAND_UP("standup", min = 5, max = 60, default = 10),
    ;

    /** Ajusta [raw] al rango permitido; 0 (sin definir) equivale al valor por defecto. */
    fun clamp(raw: Int): Int = if (raw == 0) default else raw.coerceIn(min, max)

    /** Lee el objetivo de [params]; un JSON vacío o inválido da el valor por defecto. */
    fun targetFrom(params: String): Int {
        val parsed = runCatching { json.decodeFromString<MotionParams>(params.ifBlank { "{}" }) }.getOrNull()
        return clamp(parsed?.target ?: 0)
    }

    fun paramsFor(target: Int): String = json.encodeToString(MotionParams(clamp(target)))

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
