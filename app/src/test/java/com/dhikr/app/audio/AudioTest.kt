package com.dhikr.app.audio

import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.SoundSettings
import com.dhikr.app.core.model.SwitchSound
import com.dhikr.app.domain.flow.FlowEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioCuesTest {
    private val withAudio = Dhikr(1, "ا", "A", "", 33, true, 0, audioRef = "audio/a.m4a")
    private val noAudio = Dhikr(2, "ب", "B", "", 33, true, 1)

    @Test
    fun `switch cue follows the setting`() {
        val e = FlowEvent.TargetReached(noAudio, withAudio, 1)
        assertEquals(Cue.None, AudioCues.forEvent(e, SoundSettings(switchSound = SwitchSound.Off)))
        assertEquals(Cue.Chime(), AudioCues.forEvent(e, SoundSettings(switchSound = SwitchSound.Chime)))
        assertEquals(Cue.Recite(withAudio, interrupt = true), AudioCues.forEvent(e, SoundSettings(switchSound = SwitchSound.ReciteNext)))
    }

    @Test
    fun `recite next falls back to the chime when the next dhikr has no recording`() {
        val e = FlowEvent.TargetReached(withAudio, noAudio, 1)
        assertEquals(Cue.Chime(), AudioCues.forEvent(e, SoundSettings(switchSound = SwitchSound.ReciteNext)))
    }

    @Test
    fun `routine complete is a double chime unless sound is off`() {
        val e = FlowEvent.RoutineComplete(withAudio, 1)
        assertEquals(Cue.Chime(count = 2), AudioCues.forEvent(e, SoundSettings()))
        assertEquals(Cue.None, AudioCues.forEvent(e, SoundSettings(switchSound = SwitchSound.Off)))
    }

    @Test
    fun `ordinary counts recite only when enabled, never interrupting`() {
        val e = FlowEvent.Counted(withAudio, 5)
        assertEquals(Cue.None, AudioCues.forEvent(e, SoundSettings(reciteWhileCounting = false)))
        assertEquals(Cue.Recite(withAudio, interrupt = false), AudioCues.forEvent(e, SoundSettings(reciteWhileCounting = true)))
        assertEquals(Cue.None, AudioCues.forEvent(FlowEvent.Counted(noAudio, 5), SoundSettings(reciteWhileCounting = true)))
    }
}

class ChimeSynthTest {
    @Test
    fun `chime is short, not clipped, and fades out`() {
        val pcm = ChimeSynth.chime()
        val seconds = pcm.size / ChimeSynth.SAMPLE_RATE.toDouble()
        assertTrue("length $seconds", seconds in 0.2..0.4)
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue("peak $peak", peak in 3_000..(0.6 * Short.MAX_VALUE).toInt())
        val tailPeak = pcm.takeLast(200).maxOf { abs(it.toInt()) }
        assertTrue("tail $tailPeak", tailPeak < peak / 5)
    }

    @Test
    fun `double chime is longer than one`() {
        assertTrue(ChimeSynth.chime(2).size > ChimeSynth.chime(1).size * 2)
    }
}
