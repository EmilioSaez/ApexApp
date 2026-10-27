package app.luxion.apexapp.sharing

import app.luxion.apexapp.alarm.sound.BundledSounds
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Configuración compartible de una alarma guardada. */
class AlarmExporter @Inject constructor(private val sounds: SoundRepository) {
    /** Solo viaja la clave de un sonido incluido en la app; el de por defecto y los no portables se omiten. */
    suspend fun sharedOf(alarm: Alarm): SharedAlarm {
        val source = alarm.soundId?.let { sounds.get(it) }?.source
        val key = (source as? SoundSource.Bundled)?.key?.takeIf { it != BundledSounds.DEFAULT_KEY }
        return alarm.toShared(key)
    }

    suspend fun linkOf(alarm: Alarm): String = SharedAlarmCodec.toLink(sharedOf(alarm))

    suspend fun fileBytesOf(alarm: Alarm): ByteArray = SharedAlarmCodec.encode(sharedOf(alarm))
}

/** Del contenido recibido a una alarma nueva, solo tras la confirmación del usuario. */
class AlarmImporter @Inject constructor(
    private val challenges: ChallengeRegistry,
    private val alarms: AlarmRepository,
    private val sounds: SoundRepository,
) {
    fun previewOf(link: String): ValidationResult = validated(SharedAlarmCodec.decodeLink(link))

    fun previewOf(fileBytes: ByteArray): ValidationResult = validated(SharedAlarmCodec.decode(fileBytes))

    private fun validated(decoded: DecodeResult): ValidationResult = when (decoded) {
        is DecodeResult.Failure -> ValidationResult.Invalid(decoded.error)
        is DecodeResult.Success -> SharedAlarmValidator.validate(
            decoded.shared,
            knownChallengeTypes = challenges.types.mapTo(mutableSetOf()) { it.id },
            knownSoundKeys = BundledSounds.keys,
        )
    }

    /** Guarda la alarma desactivada y sin programar; devuelve su identificador. */
    suspend fun import(preview: ImportPreview): Long {
        val alarm = preview.alarm.copy(id = 0, enabled = false, soundId = soundIdFor(preview.soundKey))
        return alarms.save(alarm)
    }

    /** Reutiliza la fila del sonido incluido o la crea; null para el sonido por defecto. */
    private suspend fun soundIdFor(key: String?): Long? {
        if (key == null || key == BundledSounds.DEFAULT_KEY) return null
        val existing = sounds.observeAll().first().firstOrNull { (it.source as? SoundSource.Bundled)?.key == key }
        return existing?.id ?: sounds.save(BundledSounds.resolve(key))
    }
}
