package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.R
import app.luxion.apexapp.domain.SoundSource
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledSoundsTest {
    @Test
    fun unknownOrNullKey_resolvesToDefault() {
        val default = SoundSource.Bundled(BundledSounds.DEFAULT_KEY)
        assertEquals(default, BundledSounds.resolve(null).source)
        assertEquals(default, BundledSounds.resolve("nope").source)
        assertEquals(default, BundledSounds.resolve(BundledSounds.DEFAULT_KEY).source)
    }

    @Test
    fun rawRes_fallsBackToDefaultTone() {
        assertEquals(R.raw.default_alarm, BundledSounds.rawRes("nope"))
        assertEquals(R.raw.default_alarm, BundledSounds.rawRes(BundledSounds.DEFAULT_KEY))
    }
}
