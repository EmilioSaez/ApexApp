package app.luxion.apexapp.domain.challenge.objectphoto

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeEventSink
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeSession
import app.luxion.apexapp.domain.challenge.FixedDifficultyProvider
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import app.luxion.apexapp.domain.challenge.qr.FrameLivenessChecker
import app.luxion.apexapp.domain.challenge.qr.LivenessVerdict
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectPhotoChallengeTest {
    private class Caps(
        override val hasCamera: Boolean = true,
        override val hasCameraPermission: Boolean = true,
    ) : DeviceCapabilities {
        override val hasAccelerometer = true
        override val hasStepSensor = true
        override val hasActivityRecognitionPermission = true
    }

    private val tap = RegisteredObject(index = 42, text = "Tap")
    private val mug = RegisteredObject(index = 7, text = "Mug")
    private val plant = RegisteredObject(index = 9, text = "Plant")
    private val params = ObjectPhotoParams.paramsFor(listOf(tap))
    private val frameNanos = 33_000_000L

    private fun instance(params: String) = ObjectPhotoChallenge(Caps()).generate(3, params, Random(1))

    @Test
    fun `los params guardan solo etiquetas`() {
        assertEquals(listOf(tap), ObjectPhotoParams.parse(params))
        assertTrue(ObjectPhotoParams.isRegistered(params))
    }

    @Test
    fun `anadir y quitar objetos sin repetir`() {
        val two = ObjectPhotoParams.added(ObjectPhotoParams.added(params, mug), mug)
        assertEquals(listOf(tap, mug), ObjectPhotoParams.parse(two))
        assertEquals(listOf(mug), ObjectPhotoParams.parse(ObjectPhotoParams.removed(two, tap.index)))
        assertFalse(ObjectPhotoParams.isRegistered(ObjectPhotoParams.removed(params, tap.index)))
    }

    @Test
    fun `con varios objetos no se repite uno ya superado`() {
        val all = ObjectPhotoParams.paramsFor(listOf(tap, mug, plant))
        val challenge = ObjectPhotoChallenge(Caps())
        repeat(20) { seed ->
            val random = Random(seed)
            val solved = mutableListOf<app.luxion.apexapp.domain.challenge.ChallengeInstance>()
            repeat(3) {
                val next = challenge.generate(3, all, random, solved) as ObjectPhotoInstance
                assertTrue(solved.none { (it as ObjectPhotoInstance).expected == next.expected })
                solved += next
            }
        }
    }

    @Test
    fun `una sesion con dos retos de foto pide objetos distintos`() {
        val all = ObjectPhotoParams.paramsFor(listOf(tap, mug))
        repeat(20) { seed ->
            val session = ChallengeSession(
                sessionId = "s",
                alarmId = null,
                config = SessionConfig(3, 2, ChallengeMode.CHAINED, listOf(ChallengeSpec(ObjectPhotoChallenge.TYPE.id, all))),
                registry = ChallengeRegistry(listOf(ObjectPhotoChallenge(Caps()))),
                difficultyProvider = FixedDifficultyProvider(),
                eventSink = ChallengeEventSink {},
                random = Random(seed),
            )
            val asked = mutableListOf<Int>()
            repeat(2) {
                val expected = (session.state.value.current as ObjectPhotoInstance).expected!!
                asked += expected.index
                session.submit(ChallengeAnswer.ObjectVerified(expected.index))
            }
            assertTrue(session.state.value.passed)
            assertEquals(2, asked.toSet().size)
        }
    }

    @Test
    fun `si se agotan los objetos se repite`() {
        val challenge = ObjectPhotoChallenge(Caps())
        val first = challenge.generate(3, params, Random(1)) as ObjectPhotoInstance
        val second = challenge.generate(3, params, Random(1), listOf(first)) as ObjectPhotoInstance
        assertEquals(tap, second.expected)
    }

    @Test
    fun `params vacios o invalidos no cuentan como registrados`() {
        assertTrue(ObjectPhotoParams.parse("").isEmpty())
        assertTrue(ObjectPhotoParams.parse("{}").isEmpty())
        assertTrue(ObjectPhotoParams.parse("no es json").isEmpty())
        assertFalse(ObjectPhotoParams.isRegistered("""{"objects":[{"index":-1,"text":"x"}]}"""))
    }

    @Test
    fun `etiqueta coincidente supera el reto`() {
        assertTrue(instance(params).check(ChallengeAnswer.ObjectVerified(42)))
    }

    @Test
    fun `etiqueta distinta u otra respuesta no supera el reto`() {
        val i = instance(params)
        assertFalse(i.check(ChallengeAnswer.ObjectVerified(7)))
        assertFalse(i.check(ChallengeAnswer.Text("Tap")))
        assertFalse(i.check(ChallengeAnswer.ScanVerified("42")))
    }

    @Test
    fun `sin objeto registrado cualquier deteccion verificada supera el reto`() {
        assertTrue(instance("").check(ChallengeAnswer.ObjectVerified(7)))
    }

    @Test
    fun `no disponible sin camara o sin permiso`() {
        assertEquals(Availability.Available, ObjectPhotoChallenge(Caps()).availability())
        assertTrue(ObjectPhotoChallenge(Caps(hasCamera = false)).availability() is Availability.Unavailable)
        assertTrue(ObjectPhotoChallenge(Caps(hasCameraPermission = false)).availability() is Availability.Unavailable)
    }

    @Test
    fun `el adaptador solo da valor con la etiqueta esperada y confianza suficiente`() {
        val labels = listOf(DetectedLabel(42, "Tap", 0.8f), DetectedLabel(7, "Room", 0.95f))
        assertEquals("42", ObjectFrameAdapter.observe(0, labels, 42, 100f).value)
        assertNull(ObjectFrameAdapter.observe(0, labels, 99, 100f).value)
        assertNull(ObjectFrameAdapter.observe(0, listOf(DetectedLabel(42, "Tap", 0.69f)), 42, 100f).value)
        assertNull(ObjectFrameAdapter.observe(0, emptyList(), 42, 100f).value)
    }

    /** Alimenta fotogramas [range] y devuelve el primer veredicto que no sea de recogida, o el último. */
    private fun feed(
        checker: FrameLivenessChecker,
        range: IntRange,
        seen: Boolean = true,
        luma: (Int) -> Float,
    ): LivenessVerdict {
        val labels = if (seen) listOf(DetectedLabel(42, "Tap", 0.9f)) else emptyList()
        val all = range.map { checker.onFrame(ObjectFrameAdapter.observe(it * frameNanos, labels, 42, luma(it))) }
        return all.firstOrNull { it !is LivenessVerdict.Collecting } ?: all.last()
    }

    /** Escena real: la luz del fondo fluctúa de forma irregular al mover la mano. */
    private fun naturalLuma(): (Int) -> Float {
        val r = Random(1)
        return { 120f + r.nextFloat() * 3f }
    }

    @Test
    fun `objeto enfocado de forma continua se acepta`() {
        assertEquals(LivenessVerdict.Accepted("42"), feed(FrameLivenessChecker(), 0..59, luma = naturalLuma()))
    }

    @Test
    fun `objeto que sale del encuadre reinicia el progreso`() {
        val checker = FrameLivenessChecker()
        val luma = naturalLuma()
        assertTrue(feed(checker, 0..29, luma = luma) is LivenessVerdict.Collecting)
        assertEquals(LivenessVerdict.Collecting(0f), feed(checker, 30..30, seen = false, luma = luma))
        // Tras perderse hace falta una ventana completa otra vez.
        assertTrue(feed(checker, 31..45, luma = luma) is LivenessVerdict.Collecting)
    }

    @Test
    fun `foto estatica se rechaza`() {
        val verdict = feed(FrameLivenessChecker(), 0..59) { 120f }
        assertEquals(LivenessVerdict.Rejected(LivenessVerdict.Reason.STATIC_IMAGE), verdict)
    }

    @Test
    fun `pantalla con parpadeo periodico se rechaza`() {
        val verdict = feed(FrameLivenessChecker(), 0..89) { 120f + 5f * sin(2 * PI * it / 6.0).toFloat() }
        assertEquals(LivenessVerdict.Rejected(LivenessVerdict.Reason.SCREEN), verdict)
    }
}
