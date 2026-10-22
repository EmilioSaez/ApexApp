package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.SnoozePolicy
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedAlarmValidatorTest {
    private val types = setOf("math", "qr", "shake")
    private val sounds = setOf("default")

    private fun shared(
        hour: Int = 7,
        minute: Int = 30,
        days: List<Int> = listOf(1, 3),
        label: String? = "Trabajo",
        challenge: ChallengeConfig? = null,
        sound: SharedSound? = null,
        antiCheat: AntiCheatPolicy? = null,
    ) = SharedAlarm(
        alarm = SharedAlarmBody(hour, minute, days, label),
        challenge = challenge,
        sound = sound,
        antiCheat = antiCheat,
    )

    private fun validate(shared: SharedAlarm) = SharedAlarmValidator.validate(shared, types, sounds)
    private fun preview(shared: SharedAlarm) = (validate(shared) as ValidationResult.Valid).preview
    private fun assertInvalid(shared: SharedAlarm) =
        assertEquals(ValidationResult.Invalid(ImportError.INVALID), validate(shared))

    private fun withSpecs(vararg specs: ChallengeSpec, difficulty: Int = 0) =
        ChallengeConfig(difficulty = difficulty, challenges = specs.toList())

    @Test
    fun validContent_becomesDisabledNewAlarm() {
        val alarm = preview(shared()).alarm
        assertEquals(0L, alarm.id)
        assertFalse(alarm.enabled)
        assertEquals(null, alarm.soundId)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), alarm.repeatDays)
        assertEquals("Trabajo", alarm.label)
    }

    @Test
    fun outOfRangeTime_isRejected() {
        assertInvalid(shared(hour = 25))
        assertInvalid(shared(minute = 60))
        assertInvalid(shared(hour = -1))
    }

    @Test
    fun badDays_areRejected() {
        assertInvalid(shared(days = listOf(0)))
        assertInvalid(shared(days = listOf(8)))
        assertInvalid(shared(days = listOf(2, 2)))
    }

    @Test
    fun labelTooLong_isRejected() {
        assertInvalid(shared(label = "x".repeat(SharedAlarmValidator.MAX_LABEL_LENGTH + 1)))
        assertEquals(null, preview(shared(label = "   ")).alarm.label)
    }

    @Test
    fun challengeRanges_areChecked() {
        assertInvalid(shared(challenge = ChallengeConfig(difficulty = 6)))
        assertInvalid(shared(challenge = ChallengeConfig(count = 0)))
        assertInvalid(shared(challenge = ChallengeConfig(count = 11)))
        assertInvalid(shared(challenge = ChallengeConfig(challenges = List(11) { ChallengeSpec("math") })))
    }

    @Test
    fun badChallengeParams_areRejected() {
        assertInvalid(shared(challenge = withSpecs(ChallengeSpec("math", "no json"))))
        assertInvalid(shared(challenge = withSpecs(ChallengeSpec("math", "[1]"))))
        assertInvalid(shared(challenge = withSpecs(ChallengeSpec("math", """{"a":"${"x".repeat(2000)}"}"""))))
    }

    @Test
    fun unknownChallenge_isOmittedWithNote() {
        val result = preview(shared(challenge = withSpecs(ChallengeSpec("math"), ChallengeSpec("hack"))))
        assertEquals(listOf(ChallengeSpec("math")), result.alarm.challenges.challenges)
        assertEquals(setOf(ImportNote.UNKNOWN_CHALLENGE_OMITTED), result.notes)
    }

    @Test
    fun qrChallenge_isOmittedWithNote() {
        val result = preview(shared(challenge = withSpecs(ChallengeSpec("qr", "{}"), difficulty = 2)))
        assertTrue(result.alarm.challenges.challenges.isEmpty())
        assertEquals(2, result.alarm.challenges.difficulty)
        assertEquals(setOf(ImportNote.QR_CHALLENGE_OMITTED), result.notes)
    }

    @Test
    fun soundKey_knownIsKeptUnknownFallsBackWithNote() {
        assertEquals("default", preview(shared(sound = SharedSound(key = "default"))).soundKey)
        val unknown = preview(shared(sound = SharedSound(key = "../etc/passwd")))
        assertEquals(null, unknown.soundKey)
        assertEquals(setOf(ImportNote.UNKNOWN_SOUND_DEFAULTED), unknown.notes)
    }

    @Test
    fun missingBlocks_useDefaults() {
        val alarm = preview(shared()).alarm
        assertEquals(ChallengeConfig(), alarm.challenges)
        assertEquals(AntiCheatPolicy(), alarm.antiCheat)
    }

    @Test
    fun snoozeRanges_areChecked() {
        assertInvalid(shared(antiCheat = AntiCheatPolicy(snooze = SnoozePolicy(minutes = 0))))
        assertInvalid(shared(antiCheat = AntiCheatPolicy(snooze = SnoozePolicy(minutes = 61))))
        assertInvalid(shared(antiCheat = AntiCheatPolicy(snooze = SnoozePolicy(maxCount = 11))))
    }
}
