package app.luxion.apexapp.sharing

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON → Deflate. El archivo `.apexalarm` son esos bytes; el enlace los pasa además a Base64 URL-safe.
 * La entrada se trata como no confiable: se acota antes y después de descomprimir.
 */
object SharedAlarmCodec {
    const val SCHEMA_VERSION = 1
    const val LINK_PREFIX = "apexapp://alarm?d="
    const val FILE_EXTENSION = "apexalarm"
    const val MIME_TYPE = "application/vnd.apexapp.alarm"

    const val MAX_INPUT_BYTES = 8 * 1024
    const val MAX_JSON_BYTES = 16 * 1024

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(shared: SharedAlarm): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(json.encodeToString(shared).toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    fun toLink(shared: SharedAlarm): String =
        LINK_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(encode(shared))

    /** Extrae el contenido de un enlace de la app; null si no lo es. */
    fun payloadOf(link: String): String? = link.trim().takeIf { it.startsWith(LINK_PREFIX) }?.removePrefix(LINK_PREFIX)

    fun decodeLink(link: String): DecodeResult {
        val payload = payloadOf(link) ?: return DecodeResult.Failure(ImportError.MALFORMED)
        if (payload.length > MAX_INPUT_BYTES * 2) return DecodeResult.Failure(ImportError.TOO_LARGE)
        val bytes = runCatching { Base64.getUrlDecoder().decode(payload) }.getOrNull()
            ?: return DecodeResult.Failure(ImportError.MALFORMED)
        return decode(bytes)
    }

    fun decode(bytes: ByteArray): DecodeResult {
        if (bytes.size > MAX_INPUT_BYTES) return DecodeResult.Failure(ImportError.TOO_LARGE)
        val text = inflate(bytes) ?: return DecodeResult.Failure(ImportError.MALFORMED)
        if (text == TOO_LARGE_MARK) return DecodeResult.Failure(ImportError.TOO_LARGE)
        val version = runCatching { (json.parseToJsonElement(text) as JsonObject).getValue("v").jsonPrimitive.intOrNull }
            .getOrNull() ?: return DecodeResult.Failure(ImportError.MALFORMED)
        if (version > SCHEMA_VERSION) return DecodeResult.Failure(ImportError.UNSUPPORTED_VERSION)
        if (version < 1) return DecodeResult.Failure(ImportError.MALFORMED)
        return try {
            DecodeResult.Success(json.decodeFromString<SharedAlarm>(text))
        } catch (_: SerializationException) {
            DecodeResult.Failure(ImportError.MALFORMED)
        } catch (_: IllegalArgumentException) {
            DecodeResult.Failure(ImportError.INVALID)
        }
    }

    private const val TOO_LARGE_MARK = "\u0000too-large"

    /** Texto descomprimido, [TOO_LARGE_MARK] si supera el tope, o null si no es un flujo Deflate válido. */
    private fun inflate(bytes: ByteArray): String? = try {
        InflaterInputStream(bytes.inputStream()).use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_JSON_BYTES) return TOO_LARGE_MARK
                out.write(buffer, 0, read)
            }
            out.toString(Charsets.UTF_8.name())
        }
    } catch (_: java.io.IOException) {
        null
    }
}

sealed interface DecodeResult {
    data class Success(val shared: SharedAlarm) : DecodeResult
    data class Failure(val error: ImportError) : DecodeResult
}

enum class ImportError { MALFORMED, TOO_LARGE, UNSUPPORTED_VERSION, INVALID }
