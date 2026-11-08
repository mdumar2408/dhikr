package com.dhikr.app.data.preferences

import androidx.datastore.core.CorruptionException
import com.dhikr.app.core.model.BeadStyle
import com.dhikr.app.core.model.CountingFeel
import com.dhikr.app.core.model.TargetHaptic
import com.dhikr.app.core.model.ThemeMode
import com.dhikr.app.core.model.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class UserPreferencesSerializerTest {

    private suspend fun read(json: String) = UserPreferencesSerializer.readFrom(ByteArrayInputStream(json.encodeToByteArray()))

    @Test
    fun `round trip keeps every value`() = runTest {
        val prefs = UserPreferences(ThemeMode.Light, CountingFeel(beadStyle = BeadStyle.Ring, targetHaptic = TargetHaptic.Thud))
        val out = ByteArrayOutputStream()
        UserPreferencesSerializer.writeTo(prefs, out)
        assertEquals(prefs, UserPreferencesSerializer.readFrom(ByteArrayInputStream(out.toByteArray())))
    }

    @Test
    fun `defaults are the owner's picks`() {
        val d = UserPreferencesSerializer.defaultValue.feel
        assertEquals(BeadStyle.Slide, d.beadStyle)
        assertEquals(TargetHaptic.DoubleBuzz, d.targetHaptic)
    }

    @Test
    fun `unknown keys from a newer version are ignored`() = runTest {
        val p = read("""{"themeMode":"Dark","someFutureSetting":42}""")
        assertEquals(ThemeMode.Dark, p.themeMode)
    }

    @Test
    fun `unknown enum value falls back to the default`() = runTest {
        val p = read("""{"themeMode":"Sepia","feel":{"targetHaptic":"Removed"}}""")
        assertEquals(ThemeMode.System, p.themeMode)
        assertEquals(TargetHaptic.DoubleBuzz, p.feel.targetHaptic)
    }

    @Test
    fun `a file from before Phase 4 loads with sound defaults`() = runTest {
        val p = read("""{"themeMode":"Light","feel":{"beadStyle":"Ring"}}""")
        assertEquals(com.dhikr.app.core.model.SwitchSound.Chime, p.sound.switchSound)
        assertEquals(false, p.sound.reciteWhileCounting)
        assertEquals(BeadStyle.Ring, p.feel.beadStyle)
    }

    @Test
    fun `garbage is reported as corruption`() = runTest {
        try {
            read("not json")
            fail("expected CorruptionException")
        } catch (_: CorruptionException) {
        }
    }
}
