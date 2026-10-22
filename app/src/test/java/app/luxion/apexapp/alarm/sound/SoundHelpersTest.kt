package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.VibrationPatterns
import java.time.LocalTime
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SoundHelpersTest {
    @Test
    fun ramp_isLinearAndMonotonic() {
        val gains = (0..30_000 step 1_000).map { VolumeRamp.gainAt(it.toLong(), 30, 1f) }
        assertEquals(gains.sorted(), gains)
        assertEquals(VolumeRamp.START_GAIN, gains.first(), 0.001f)
        assertEquals(1f, gains.last(), 0.001f)
        assertEquals(1f, VolumeRamp.gainAt(99_000, 30, 1f), 0.001f)
    }

    @Test
    fun ramp_withoutRampOrLowCeiling() {
        assertEquals(0.7f, VolumeRamp.gainAt(0, 0, 0.7f), 0.001f)
        assertEquals(0.02f, VolumeRamp.gainAt(0, 10, 0.02f), 0.001f)
        assertEquals(0f, VolumeRamp.gainAt(5_000, 10, 0f), 0.001f)
    }

    @Test
    fun presets_areValidAndStartWithPulse() {
        VibrationPatterns.presets.keys.forEach { assertTrue(it, VibrationPatterns.isValid(it)) }
        assertEquals(0L, VibrationPatterns.waveform("steady").first())
    }

    @Test
    fun customPattern_parsesAndPadsOddLength() {
        assertEquals(listOf(200L, 100L, 400L), VibrationPatterns.steps("c:200,100,400"))
        assertArrayEquals(longArrayOf(0, 200, 100, 400, 1000), VibrationPatterns.waveform("c:200,100,400"))
        assertArrayEquals(longArrayOf(0, 200, 100), VibrationPatterns.waveform("c:200,100"))
    }

    @Test
    fun invalidPatterns_areRejected() {
        listOf("", "c:", "c:abc", "c:0,100", "c:-5", "other", "c:" + List(21) { 100 }.joinToString(","), "c:5000,5000,5000")
            .forEach { assertNull(it, VibrationPatterns.steps(it)) }
        assertFalse(VibrationPatterns.isValid("nope"))
        assertEquals(listOf(200L, 100L), VibrationPatterns.steps(VibrationPatterns.custom(listOf(200, 100))))
    }

    @Test
    fun invalidWaveformFallsBackToDefault() {
        assertArrayEquals(VibrationPatterns.waveform("steady"), VibrationPatterns.waveform("broken"))
    }

    @Test
    fun soundConfig_validatesRanges() {
        SoundConfig(rampSeconds = 120, speakRepeatSeconds = 15, volume = 0)
        listOf<() -> SoundConfig>(
            { SoundConfig(volume = 101) },
            { SoundConfig(volume = -1) },
            { SoundConfig(rampSeconds = 121) },
            { SoundConfig(rampSeconds = -1) },
            { SoundConfig(speakRepeatSeconds = 14) },
            { SoundConfig(ttsText = "x".repeat(141)) },
            { SoundConfig(vibrationPattern = "zzz") },
        ).forEach {
            try {
                it()
                fail("expected IllegalArgumentException")
            } catch (_: IllegalArgumentException) {
            }
        }
        SoundConfig(ttsText = "x".repeat(140))
    }

    @Test
    fun speechScript_ordersTimeThenReminder() {
        val announce = { t: LocalTime -> "it is $t" }
        val time = LocalTime.of(7, 5)
        assertEquals(emptyList<String>(), SpeechScript.build(SoundConfig(), time, announce))
        assertEquals(listOf("it is 07:05"), SpeechScript.build(SoundConfig(speakTime = true), time, announce))
        assertEquals(listOf("hola"), SpeechScript.build(SoundConfig(ttsEnabled = true, ttsText = " hola "), time, announce))
        assertEquals(emptyList<String>(), SpeechScript.build(SoundConfig(ttsEnabled = true, ttsText = "  "), time, announce))
        assertEquals(
            listOf("it is 07:05", "hola"),
            SpeechScript.build(SoundConfig(ttsEnabled = true, ttsText = "hola", speakTime = true), time, announce),
        )
    }
}
