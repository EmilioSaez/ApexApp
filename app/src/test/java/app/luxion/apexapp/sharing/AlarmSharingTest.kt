package app.luxion.apexapp.sharing

import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.alarm.sound.BundledSounds
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.MathChallenge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSoundRepository(initial: List<Sound> = emptyList()) : SoundRepository {
    private val sounds = MutableStateFlow(initial.associateBy { it.id })

    override fun observeAll(): Flow<List<Sound>> = sounds.map { it.values.toList() }
    override suspend fun get(id: Long): Sound? = sounds.value[id]
    override suspend fun save(sound: Sound): Long {
        val id = if (sound.id == 0L) sounds.value.size + 1L else sound.id
        sounds.value = sounds.value + (id to sound.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        sounds.value = sounds.value - id
    }
}

class AlarmSharingTest {
    private val alarms = FakeAlarmRepository()
    private val sounds = FakeSoundRepository(
        listOf(
            Sound(1, "default", SoundSource.Bundled(BundledSounds.DEFAULT_KEY)),
            Sound(2, "mi tono", SoundSource.Uri("content://media/external/audio/7")),
            Sound(3, "sistema", SoundSource.SystemTone("content://settings/system/alarm_alert")),
        ),
    )
    private val exporter = AlarmExporter(sounds)
    private val importer = AlarmImporter(ChallengeRegistry(listOf(MathChallenge())), alarms, sounds)

    private fun alarm(soundId: Long? = null) = Alarm(
        id = 5,
        hour = 6,
        minute = 15,
        enabled = true,
        label = "Correr",
        soundId = soundId,
        challenges = ChallengeConfig(2, listOf(ChallengeSpec("math"), ChallengeSpec("qr", """{"hash":"abc"}"""))),
    )

    @Test
    fun export_omitsDefaultAndNonPortableSounds() = runTest {
        assertNull(exporter.sharedOf(alarm(soundId = null)).sound!!.key)
        assertNull(exporter.sharedOf(alarm(soundId = 1)).sound!!.key)
        assertNull(exporter.sharedOf(alarm(soundId = 2)).sound!!.key)
        assertNull(exporter.sharedOf(alarm(soundId = 3)).sound!!.key)
    }

    @Test
    fun link_exportedThenPreviewed_isValid() = runTest {
        val preview = (importer.previewOf(exporter.linkOf(alarm())) as ValidationResult.Valid).preview
        assertEquals(6, preview.alarm.hour)
        assertEquals("Correr", preview.alarm.label)
        assertEquals(listOf(ChallengeSpec("math")), preview.alarm.challenges.challenges)
        assertEquals(setOf(ImportNote.UNKNOWN_CHALLENGE_OMITTED), preview.notes)
    }

    @Test
    fun file_exportedThenPreviewed_isValid() = runTest {
        val result = importer.previewOf(exporter.fileBytesOf(alarm()))
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun invalidContent_neverReachesPreview() {
        assertEquals(ValidationResult.Invalid(ImportError.MALFORMED), importer.previewOf("apexapp://alarm?d=###"))
        assertEquals(ValidationResult.Invalid(ImportError.MALFORMED), importer.previewOf(byteArrayOf(9, 9)))
    }

    @Test
    fun accepting_createsDisabledNewAlarm_andCancelCreatesNothing() = runTest {
        val preview = (importer.previewOf(exporter.linkOf(alarm())) as ValidationResult.Valid).preview
        assertTrue(alarmsNow().isEmpty())

        val id = importer.import(preview)

        val saved = alarms.get(id)!!
        assertFalse(saved.enabled)
        assertNull(saved.soundId)
        assertEquals("Correr", saved.label)
    }

    @Test
    fun importingTwice_createsTwoIndependentAlarms() = runTest {
        val preview = (importer.previewOf(exporter.linkOf(alarm())) as ValidationResult.Valid).preview
        val first = importer.import(preview)
        val second = importer.import(preview)
        assertTrue(first != second)
        assertEquals(2, alarmsNow().size)
    }

    private suspend fun alarmsNow() = alarms.observeAll().first()
}
