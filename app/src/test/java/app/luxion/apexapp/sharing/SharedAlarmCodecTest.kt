package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.domain.SoundConfig
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedAlarmCodecTest {
    private val alarm = Alarm(
        id = 9,
        hour = 6,
        minute = 45,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
        enabled = true,
        label = "Gimnasio",
        soundId = 4,
        challenges = ChallengeConfig(
            3,
            listOf(ChallengeSpec("math", """{"ops":3}"""), ChallengeSpec("qr", """{"format":256,"hash":"abc"}""")),
            count = 2,
        ),
        soundConfig = SoundConfig(volume = 70, ttsEnabled = true, ttsText = "Arriba"),
        antiCheat = AntiCheatPolicy(AntiCheatLevel.STANDARD, SnoozePolicy(allowed = true, minutes = 10, maxCount = 2)),
    )

    private fun deflate(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    private fun success(result: DecodeResult) = (result as DecodeResult.Success).shared
    private fun failure(result: DecodeResult) = (result as DecodeResult.Failure).error

    @Test
    fun export_leavesOutIdEnabledAndDeviceBoundData() {
        val shared = alarm.toShared(soundKey = "default")
        val text = String(InflaterInputStream(SharedAlarmCodec.encode(shared).inputStream()).readBytes())
        assertFalse(text.contains("\"id\""))
        assertFalse(text.contains("enabled"))
        assertFalse(text.contains("abc"))
        assertEquals(listOf(1, 5), shared.alarm.days)
        assertEquals(ChallengeSpec("qr"), shared.challenge!!.challenges[1])
    }

    @Test
    fun link_roundTrips() {
        val shared = alarm.toShared("default")
        val link = SharedAlarmCodec.toLink(shared)
        assertTrue(link.startsWith(SharedAlarmCodec.LINK_PREFIX))
        assertEquals(shared, success(SharedAlarmCodec.decodeLink(link)))
    }

    @Test
    fun file_roundTrips() {
        val shared = alarm.toShared(null)
        assertEquals(shared, success(SharedAlarmCodec.decode(SharedAlarmCodec.encode(shared))))
    }

    @Test
    fun decode_rejectsOversizedInput() {
        val tooBig = ByteArray(SharedAlarmCodec.MAX_INPUT_BYTES + 1)
        assertEquals(ImportError.TOO_LARGE, failure(SharedAlarmCodec.decode(tooBig)))
    }

    @Test
    fun decode_rejectsDecompressionBomb() {
        val bomb = deflate("""{"v":1,"pad":"${"a".repeat(200_000)}"}""")
        assertTrue(bomb.size < SharedAlarmCodec.MAX_INPUT_BYTES)
        assertEquals(ImportError.TOO_LARGE, failure(SharedAlarmCodec.decode(bomb)))
    }

    @Test
    fun decode_rejectsGarbage() {
        assertEquals(ImportError.MALFORMED, failure(SharedAlarmCodec.decode(byteArrayOf(1, 2, 3, 4))))
        assertEquals(ImportError.MALFORMED, failure(SharedAlarmCodec.decode(deflate("no es json"))))
        assertEquals(ImportError.MALFORMED, failure(SharedAlarmCodec.decodeLink("https://example.com")))
        assertEquals(ImportError.MALFORMED, failure(SharedAlarmCodec.decodeLink(SharedAlarmCodec.LINK_PREFIX + "***")))
    }

    @Test
    fun decode_acceptsSupportedVersionAndIgnoresUnknownKeys() {
        val bytes = deflate("""{"v":1,"alarm":{"hour":7,"minute":0,"extra":1},"future":{"x":1}}""")
        assertEquals(7, success(SharedAlarmCodec.decode(bytes)).alarm.hour)
    }

    @Test
    fun decode_rejectsNewerVersion() {
        val bytes = deflate("""{"v":2,"alarm":{"hour":7,"minute":0}}""")
        assertEquals(ImportError.UNSUPPORTED_VERSION, failure(SharedAlarmCodec.decode(bytes)))
    }

    @Test
    fun decode_rejectsMissingVersion() {
        val bytes = deflate("""{"alarm":{"hour":7,"minute":0}}""")
        assertEquals(ImportError.MALFORMED, failure(SharedAlarmCodec.decode(bytes)))
    }

    @Test
    fun decode_missingOptionalBlocksStayNull() {
        val shared = success(SharedAlarmCodec.decode(deflate("""{"v":1,"alarm":{"hour":7,"minute":0}}""")))
        assertEquals(null, shared.challenge)
        assertEquals(null, shared.sound)
        assertEquals(null, shared.antiCheat)
    }

    @Test
    fun decode_invalidSoundConfigIsRejected() {
        val bytes = deflate("""{"v":1,"alarm":{"hour":7,"minute":0},"sound":{"config":{"volume":500}}}""")
        assertEquals(ImportError.INVALID, failure(SharedAlarmCodec.decode(bytes)))
    }
}
