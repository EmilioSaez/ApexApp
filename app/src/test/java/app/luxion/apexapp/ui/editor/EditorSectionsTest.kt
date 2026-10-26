package app.luxion.apexapp.ui.editor

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.alarm.FakeGateway
import app.luxion.apexapp.alarm.testController
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.ui.challenge.ChallengeParamsEditor
import app.luxion.apexapp.ui.challenge.ChallengesSection
import app.luxion.apexapp.ui.challenge.MathRenderer
import app.luxion.apexapp.ui.challenge.displayDifficulty
import app.luxion.apexapp.ui.challenge.isEnabled
import app.luxion.apexapp.ui.challenge.toggled
import app.luxion.apexapp.ui.challenge.withCount
import app.luxion.apexapp.ui.challenge.withDifficulty
import app.luxion.apexapp.ui.challenge.withMode
import app.luxion.apexapp.ui.challenge.withParams
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSection(override val order: Int, private val valid: (Alarm) -> Boolean = { true }) : EditorSection {
    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) = Unit
    override fun isValid(draft: Alarm) = valid(draft)
}

private class SectionA(order: Int) : EditorSection by FakeSection(order)
private class SectionB(order: Int) : EditorSection by FakeSection(order)

private class QrLikeEditor : ChallengeParamsEditor {
    override val type = ChallengeType("qr")

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) = Unit

    /** Un reto de QR sin código registrado no es válido. */
    override fun isValid(params: String) = params.contains("code")
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditorSectionsTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, zone).toInstant(), zone)
    private val repository = FakeAlarmRepository()
    private val controller = testController(repository, FakeGateway(), clock)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(sections: Set<EditorSection>) =
        AlarmEditorViewModel(SavedStateHandle(mapOf("alarmId" to 0L)), repository, controller, clock, sections)

    private val challengesSection = ChallengesSection(
        ChallengeRegistry(listOf(MathChallenge())),
        setOf(MathRenderer()),
        setOf(QrLikeEditor()),
    )

    @Test
    fun `sections are ordered by priority and then by a stable name`() {
        val vm = viewModel(setOf(SectionB(5), SectionA(5), FakeSection(0)))
        assertEquals(listOf(0, 5, 5), vm.sections.map { it.order })
        assertEquals(listOf("SectionA", "SectionB"), vm.sections.drop(1).map { it::class.simpleName })
    }

    @Test
    fun `draft changes from a section are saved and untouched fields are kept`() = runTest {
        val vm = viewModel(emptySet())
        vm.setLabel("Gym")
        vm.updateDraft(vm.uiState.value.draft.copy(challenges = ChallengeConfig(difficulty = 4, count = 3)))
        vm.save(9, 30)
        val saved = repository.observeAll().first().single()
        assertEquals(4, saved.challenges.difficulty)
        assertEquals(3, saved.challenges.count)
        assertEquals("Gym", saved.label)
        assertEquals(9, saved.hour)
    }

    @Test
    fun `an invalid section blocks saving`() = runTest {
        val vm = viewModel(setOf(FakeSection(0) { it.challenges.count > 1 }))
        assertFalse(vm.canSave())
        vm.save(9, 30)
        assertTrue(repository.observeAll().first().isEmpty())
        vm.updateDraft(vm.uiState.value.draft.copy(challenges = ChallengeConfig(count = 2)))
        assertTrue(vm.canSave())
    }

    @Test
    fun `challenges section is invalid when an enabled type rejects its params`() {
        val qr = ChallengeSpec("qr", "{}")
        val invalid = Alarm(hour = 7, minute = 0, challenges = ChallengeConfig(challenges = listOf(qr)))
        assertFalse(challengesSection.isValid(invalid))
        val valid = invalid.copy(challenges = ChallengeConfig(challenges = listOf(qr.copy(params = """{"code":"x"}"""))))
        assertTrue(challengesSection.isValid(valid))
    }

    @Test
    fun `a type without params editor never blocks saving`() {
        val alarm = Alarm(hour = 7, minute = 0, challenges = ChallengeConfig(challenges = listOf(ChallengeSpec("math", "garbage"))))
        assertTrue(challengesSection.isValid(alarm))
    }

    @Test
    fun `toggling types appends to the chain in the order enabled`() {
        val math = ChallengeType.MATH
        val b = ChallengeType("b")
        val config = ChallengeConfig().toggled(b).toggled(math)
        assertEquals(listOf("b", "math"), config.challenges.map { it.type })
        assertTrue(config.isEnabled(b))
        assertEquals(listOf("math"), config.toggled(b).challenges.map { it.type })
    }

    @Test
    fun `initial values of a new alarm are one challenge, difficulty 3, chained, no types`() {
        val config = Alarm(hour = 7, minute = 0).challenges
        assertEquals(1, config.count)
        assertEquals(3, config.displayDifficulty)
        assertEquals(ChallengeMode.CHAINED, config.mode)
        assertTrue(config.challenges.isEmpty())
    }

    @Test
    fun `count and difficulty edits are clamped, mode and params are set`() {
        val config = ChallengeConfig(challenges = listOf(ChallengeSpec("qr")))
            .withCount(99).withDifficulty(0).withMode(ChallengeMode.RANDOM)
            .withParams(ChallengeType("qr"), """{"code":"x"}""")
        assertEquals(10, config.count)
        assertEquals(1, config.difficulty)
        assertEquals(ChallengeMode.RANDOM, config.mode)
        assertEquals("""{"code":"x"}""", config.challenges.single().params)
    }
}
