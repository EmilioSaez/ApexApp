package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeSessionFactory
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.FixedDifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.NoOpChallengeEventSink
import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeViewModelsTest {
    private val registry = ChallengeRegistry(listOf(MathChallenge()))
    private val renderer = MathRenderer()
    private val factory = ChallengeSessionFactory(registry, FixedDifficultyProvider(), NoOpChallengeEventSink())

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun sessionViewModel() = ChallengeSessionViewModel(factory, setOf(renderer))

    private val config = SessionConfig(1, 2, ChallengeMode.CHAINED, listOf(ChallengeSpec("math")))

    /** Resuelve el enunciado "a + b" / "a - b" de nivel 1. */
    private fun answerFor(prompt: String): String {
        val (a, op, b) = prompt.split(" ")
        return (if (op == "+") a.toInt() + b.toInt() else a.toInt() - b.toInt()).toString()
    }

    @Test
    fun `state is null until the session starts`() {
        assertNull(sessionViewModel().state.value)
    }

    @Test
    fun `progress advances and the session passes after all answers`() {
        val vm = sessionViewModel()
        vm.start(config, "s1")
        assertEquals(0, vm.state.value!!.solved)
        repeat(2) { vm.submit(ChallengeAnswer.Text(answerFor(vm.state.value!!.current!!.prompt))) }
        assertTrue(vm.state.value!!.passed)
        assertEquals(2, vm.state.value!!.solved)
    }

    @Test
    fun `wrong answer is flagged and does not advance`() {
        val vm = sessionViewModel()
        vm.start(config, "s1")
        vm.submit(ChallengeAnswer.Text("abc"))
        assertTrue(vm.state.value!!.lastAnswerWrong)
        assertEquals(0, vm.state.value!!.solved)
    }

    @Test
    fun `starting twice keeps the original session`() {
        val vm = sessionViewModel()
        vm.start(config, "s1")
        vm.submit(ChallengeAnswer.Text(answerFor(vm.state.value!!.current!!.prompt)))
        vm.start(config, "s2")
        assertEquals(1, vm.state.value!!.solved)
    }

    @Test
    fun `renderer is resolved by type`() {
        assertSame(renderer, sessionViewModel().rendererFor(ChallengeType.MATH))
        assertNull(sessionViewModel().rendererFor(ChallengeType("qr")))
    }

    @Test
    fun `test form is clamped and builds a session config`() {
        val vm = ChallengeTestViewModel(registry)
        vm.setCount(99)
        vm.setDifficulty(0)
        assertEquals(10, vm.form.value.count)
        assertEquals(1, vm.form.value.difficulty)
        vm.setCount(2)
        vm.setMode(ChallengeMode.RANDOM)
        vm.start()
        val running = vm.running.value
        assertNotNull(running)
        assertEquals(2, running!!.session.count)
        assertEquals(ChallengeMode.RANDOM, running.session.mode)
        assertEquals(listOf(ChallengeSpec("math")), running.session.challenges)
        vm.stop()
        assertNull(vm.running.value)
    }

    @Test
    fun `toggling a type removes and adds it`() {
        val vm = ChallengeTestViewModel(registry)
        vm.toggleType(ChallengeType.MATH)
        assertFalse(ChallengeType.MATH in vm.form.value.types)
        vm.toggleType(ChallengeType.MATH)
        assertTrue(ChallengeType.MATH in vm.form.value.types)
    }

    @Test
    fun `each test run gets its own session id`() {
        val vm = ChallengeTestViewModel(registry)
        vm.start()
        val first = vm.running.value!!.sessionId
        vm.stop()
        vm.start()
        assertTrue(first != vm.running.value!!.sessionId)
    }
}
