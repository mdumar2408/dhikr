package com.dhikr.app.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Generates the switch chime as 16-bit mono PCM. Synthesized in code — no bundled sound file,
 * so nothing to license (D-029). Pure Kotlin: unit-tested on the JVM.
 *
 * Sound: two soft sine "bell" tones (A5 → E6, a fifth apart), each with a quick attack and an
 * exponential decay, plus a little second harmonic for warmth. ~260 ms, peak ≈ 45 % of full scale.
 */
object ChimeSynth {
    const val SAMPLE_RATE = 44_100

    fun chime(count: Int = 1): ShortArray {
        val one = tone(880.0, 0.13) + tone(1318.5, 0.15)
        if (count <= 1) return one
        val gap = ShortArray((SAMPLE_RATE * 0.06).roundToInt())
        return (1 until count).fold(one) { acc, _ -> acc + gap + one }
    }

    private fun tone(freq: Double, seconds: Double, amplitude: Double = 0.45): ShortArray {
        val n = (SAMPLE_RATE * seconds).roundToInt()
        val attack = (SAMPLE_RATE * 0.004).roundToInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = (if (i < attack) i / attack.toDouble() else 1.0) * exp(-t * 18.0)
            val s = sin(2 * PI * freq * t) + 0.25 * sin(4 * PI * freq * t)
            (s / 1.25 * env * amplitude * Short.MAX_VALUE).roundToInt().toShort()
        }
    }
}
