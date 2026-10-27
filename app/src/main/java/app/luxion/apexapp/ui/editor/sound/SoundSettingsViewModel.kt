package app.luxion.apexapp.ui.editor.sound

import androidx.lifecycle.ViewModel
import app.luxion.apexapp.alarm.sound.AlarmSoundEngine
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** Elección de sonido y previsualización de la sección "Sonido" del editor de alarmas. */
@HiltViewModel
class SoundSettingsViewModel @Inject constructor(
    private val sounds: SoundRepository,
    private val importer: SoundImporter,
    private val engine: AlarmSoundEngine,
) : ViewModel() {
    val previewing: StateFlow<Boolean> = engine.playing

    suspend fun sound(id: Long?): Sound? = id?.let { sounds.get(it) }

    /** Id del sonido guardado para el archivo, o null si no es audio o no se puede leer. */
    suspend fun pickFile(uri: String): Long? {
        val name = importer.importFile(uri) ?: return null
        return saveOnce(Sound(name = name, source = SoundSource.Uri(uri)))
    }

    suspend fun pickSystemTone(uri: String): Long? {
        val name = importer.systemToneName(uri) ?: return null
        return saveOnce(Sound(name = name, source = SoundSource.SystemTone(uri)))
    }

    private suspend fun saveOnce(sound: Sound): Long =
        sounds.observeAll().first().firstOrNull { it.source == sound.source }?.id ?: sounds.save(sound)

    /** Solo se detiene lo que arrancó esta pantalla, nunca una alarma real que esté sonando. */
    private var startedPreview = false

    fun togglePreview(alarm: Alarm) {
        if (startedPreview) {
            stopPreview()
        } else if (!engine.playing.value) {
            engine.start(alarm)
            startedPreview = true
        }
    }

    fun stopPreview() {
        if (!startedPreview) return
        startedPreview = false
        engine.stop()
    }

    override fun onCleared() = stopPreview()
}
