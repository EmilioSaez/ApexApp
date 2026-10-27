package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.AlarmEntity
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.SoundSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmConfigMappingTest {
    @Test
    fun defaults_areNeutral() {
        val alarm = Alarm(hour = 7, minute = 0)
        assertEquals(ChallengeConfig(difficulty = 0, challenges = emptyList()), alarm.challenges)
        assertEquals(100, alarm.soundConfig.volume)
        assertEquals(true, alarm.soundConfig.vibrate)
        assertEquals(false, alarm.soundConfig.ttsEnabled)
        assertEquals(30, alarm.soundConfig.rampSeconds)
        assertEquals("steady", alarm.soundConfig.vibrationPattern)
        assertEquals(AntiCheatLevel.OFF, alarm.antiCheat.level)
        assertEquals(SnoozePolicy(allowed = false, minutes = 5, maxCount = 3), alarm.antiCheat.snooze)
    }

    @Test
    fun roundTrip_keepsConfig() {
        val alarm = Alarm(
            id = 3,
            hour = 6,
            minute = 30,
            challenges = ChallengeConfig(4, listOf(ChallengeSpec("math", """{"ops":3}"""), ChallengeSpec("qr"))),
            soundConfig = SoundConfig(volume = 80, vibrate = false, ttsEnabled = true, ttsText = "Arriba", speakTime = true),
            antiCheat = AntiCheatPolicy(AntiCheatLevel.STANDARD, SnoozePolicy(allowed = true, minutes = 10, maxCount = 1)),
        )
        assertEquals(alarm, alarm.toEntity().toDomain())
    }

    @Test
    fun emptyJson_decodesToDefaults() {
        val entity = AlarmEntity(id = 1, hour = 7, minute = 0, repeatMask = 0, enabled = true, label = null, soundId = null)
        assertEquals(Alarm(id = 1, hour = 7, minute = 0), entity.toDomain())
    }

    @Test
    fun unknownKeys_areIgnoredAndMissingUseDefaults() {
        val entity = AlarmEntity(
            id = 1, hour = 7, minute = 0, repeatMask = 0, enabled = true, label = null, soundId = null,
            soundJson = """{"volume":50,"futureKey":1}""",
            antiCheatJson = """{"level":"STANDARD"}""",
        )
        val alarm = entity.toDomain()
        assertEquals(SoundConfig(volume = 50), alarm.soundConfig)
        assertEquals(AntiCheatPolicy(level = AntiCheatLevel.STANDARD), alarm.antiCheat)
    }

    @Test
    fun soundConfigExtras_roundTripAndMissingKeysUseDefaults() {
        val config = SoundConfig(rampSeconds = 45, vibrationPattern = "c:200,100", speakTime = true, speakRepeatSeconds = 30)
        val alarm = Alarm(id = 2, hour = 6, minute = 0, soundConfig = config)
        assertEquals(config, alarm.toEntity().toDomain().soundConfig)

        val old = AlarmEntity(
            id = 1, hour = 7, minute = 0, repeatMask = 0, enabled = true, label = null, soundId = null,
            soundJson = """{"volume":70,"vibrate":false,"ttsEnabled":true,"ttsText":"Hola"}""",
        )
        assertEquals(
            SoundConfig(volume = 70, vibrate = false, ttsEnabled = true, ttsText = "Hola"),
            old.toDomain().soundConfig,
        )
    }

    @Test
    fun soundConfigOutOfRange_fallsBackToDefaults() {
        val entity = AlarmEntity(
            id = 1, hour = 7, minute = 0, repeatMask = 0, enabled = true, label = null, soundId = null,
            soundJson = """{"rampSeconds":9999}""",
        )
        assertEquals(SoundConfig(), entity.toDomain().soundConfig)
    }

    @Test
    fun soundSources_roundTripAllTypes() {
        listOf(
            SoundSource.Resource(42),
            SoundSource.Uri("content://x/1"),
            SoundSource.SystemTone("content://settings/system/alarm_alert"),
            SoundSource.Bundled("default"),
        ).forEach {
            val sound = Sound(id = 1, name = "n", source = it)
            assertEquals(sound, sound.toEntity().toDomain())
        }
    }

    @Test
    fun malformedJson_fallsBackToDefaults() {
        val entity = AlarmEntity(
            id = 1, hour = 7, minute = 0, repeatMask = 0, enabled = true, label = null, soundId = null,
            challengesJson = "not json",
        )
        assertEquals(ChallengeConfig(), entity.toDomain().challenges)
    }
}
