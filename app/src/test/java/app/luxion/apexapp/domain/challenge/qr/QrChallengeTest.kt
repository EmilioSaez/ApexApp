package app.luxion.apexapp.domain.challenge.qr

import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrChallengeTest {
    private class Caps(
        override val hasCamera: Boolean = true,
        override val hasCameraPermission: Boolean = true,
    ) : DeviceCapabilities {
        override val hasAccelerometer = true
        override val hasStepSensor = true
        override val hasActivityRecognitionPermission = true
    }

    private fun instance(params: String) = QrChallenge(Caps()).generate(3, params, Random(1))

    @Test
    fun `el hash es SHA-256 hexadecimal conocido`() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", QrParams.hashOf("hello"))
    }

    @Test
    fun `los params no contienen el valor del codigo`() {
        val params = QrParams.paramsFor("secreto-123", 256)
        assertFalse(params.contains("secreto-123"))
        assertEquals(QrParams.hashOf("secreto-123"), QrParams.parse(params)?.hash)
    }

    @Test
    fun `codigo correcto supera el reto`() {
        val i = instance(QrParams.paramsFor("champu", 256))
        assertTrue(i.check(ChallengeAnswer.ScanVerified(QrParams.hashOf("champu"))))
    }

    @Test
    fun `codigo distinto no supera el reto`() {
        val i = instance(QrParams.paramsFor("champu", 256))
        assertFalse(i.check(ChallengeAnswer.ScanVerified(QrParams.hashOf("otro"))))
        assertFalse(i.check(ChallengeAnswer.Text("champu")))
    }

    @Test
    fun `sustituir el codigo invalida el anterior`() {
        val old = QrParams.hashOf("viejo")
        val i = instance(QrParams.paramsFor("nuevo", 256))
        assertFalse(i.check(ChallengeAnswer.ScanVerified(old)))
        assertTrue(i.check(ChallengeAnswer.ScanVerified(QrParams.hashOf("nuevo"))))
        assertNotEquals(old, QrParams.parse(QrParams.paramsFor("nuevo", 256))?.hash)
    }

    @Test
    fun `sin codigo registrado no se puede guardar`() {
        assertFalse(QrParams.isRegistered(""))
        assertFalse(QrParams.isRegistered("{}"))
        assertFalse(QrParams.isRegistered("no es json"))
        assertNull(QrParams.parse("{}"))
        assertTrue(QrParams.isRegistered(QrParams.paramsFor("x", 1)))
    }

    @Test
    fun `sin camara o permiso no esta disponible`() {
        assertTrue(QrChallenge(Caps(hasCamera = false)).availability() is Availability.Unavailable)
        assertTrue(QrChallenge(Caps(hasCameraPermission = false)).availability() is Availability.Unavailable)
        assertEquals(Availability.Available, QrChallenge(Caps()).availability())
    }
}
