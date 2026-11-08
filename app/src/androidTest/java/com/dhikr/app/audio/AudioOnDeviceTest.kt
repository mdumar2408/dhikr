package com.dhikr.app.audio

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dhikr.app.core.model.Dhikr
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Real filesystem and MediaPlayer on the device. */
@RunWith(AndroidJUnit4::class)
class AudioOnDeviceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AudioStore(context.filesDir, context.contentResolver)

    /** A valid 16-bit mono WAV of the chime, so the test needs no bundled media. */
    private fun wavFile(): File {
        val pcm = ChimeSynth.chime(1)
        val data = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply { pcm.forEach { putShort(it) } }.array()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(ChimeSynth.SAMPLE_RATE); putInt(ChimeSynth.SAMPLE_RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }.array()
        return File(context.cacheDir, "test-chime.wav").apply {
            writeBytes(ByteArrayOutputStream().apply { write(header); write(data) }.toByteArray())
        }
    }

    @Test
    fun importCopiesIntoAppStorageAndDeleteRemovesIt() = runTest {
        val source = wavFile()
        val result = store.import(Uri.fromFile(source))
        assertTrue(result is AudioStore.ImportResult.Ok)
        val ref = (result as AudioStore.ImportResult.Ok).ref
        assertTrue(ref.startsWith("audio/") && ref.endsWith(".wav"))
        val copy = store.resolve(ref)
        assertEquals(source.length(), copy.length())
        source.delete() // the original can go away — the app keeps its copy
        assertTrue(copy.exists())
        store.delete(ref)
        assertFalse(copy.exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun refsCannotEscapeTheAudioFolder() {
        store.resolve("audio/../databases/dhikr.db")
    }

    @Test
    fun recitationPlaysAndStops() = runTest {
        val ref = (store.import(Uri.fromFile(wavFile())) as AudioStore.ImportResult.Ok).ref
        val player = AudioCuePlayer(context, store)
        val d = Dhikr(1, "ا", "A", "", 33, true, 0, audioRef = ref)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { player.play(Cue.Recite(d, interrupt = true)) }
        instrumentation.runOnMainSync { assertTrue(player.isReciting) }
        // A non-interrupting cue while reciting is skipped (no overlap) — state unchanged.
        instrumentation.runOnMainSync { player.play(Cue.Recite(d, interrupt = false)); assertTrue(player.isReciting) }
        instrumentation.runOnMainSync { player.stop(); assertFalse(player.isReciting) }
        instrumentation.runOnMainSync { player.play(Cue.Chime()) } // must not throw
        store.delete(ref)
    }
}
