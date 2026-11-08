package com.dhikr.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.dhikr.app.core.model.SoundSettings
import com.dhikr.app.domain.flow.FlowEvent
import java.io.File

/**
 * Plays counter cues (Phase 4):
 * - **Chime** via a static AudioTrack (pre-rendered PCM → lowest latency). Uses the sonification
 *   usage and is skipped when the ringer is silent or vibrate, so it never sounds in a quiet mosque.
 * - **Recitations** via MediaPlayer with transient audio focus (other audio ducks). One player:
 *   a new interrupting recitation stops the previous one; a non-interrupting one is skipped while
 *   something is playing (no overlapping phrases).
 * All methods are main-thread safe and never block it: MediaPlayer prepares asynchronously.
 */
class AudioCuePlayer(context: Context, private val audioStore: AudioStore) {

    private val app = context.applicationContext
    private val audioManager = app.getSystemService(AudioManager::class.java)

    private val sonification = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val media = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val chimes = HashMap<Int, AudioTrack>()
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    /** True while a recitation is preparing or playing. */
    val isReciting: Boolean get() = player != null

    fun onFlowEvent(event: FlowEvent, sound: SoundSettings) = play(AudioCues.forEvent(event, sound))

    fun play(cue: Cue) {
        when (cue) {
            Cue.None -> Unit
            is Cue.Chime -> playChime(cue.count)
            is Cue.Recite -> {
                if (!cue.interrupt && isReciting) return
                val file = cue.dhikr.audioRef?.let(audioStore::resolve)?.takeIf(File::exists) ?: return
                recite(file)
            }
        }
    }

    /** Preview a recording from the editor. */
    fun preview(ref: String) {
        audioStore.resolve(ref).takeIf(File::exists)?.let(::recite)
    }

    fun stop() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        abandonFocus()
    }

    private fun playChime(count: Int) {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val track = chimes.getOrPut(count) { buildTrack(ChimeSynth.chime(count)) }
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }.onFailure { Log.w(TAG, "Chime failed", it) }
    }

    private fun buildTrack(pcm: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(sonification)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(ChimeSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun recite(file: File) {
        stop()
        if (!requestFocus()) return
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setAudioAttributes(media)
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { if (player === it) stop() }
            mp.setOnErrorListener { p, what, extra ->
                Log.w(TAG, "Recitation error $what/$extra")
                if (player === p) stop()
                true
            }
            mp.prepareAsync()
        }.onFailure {
            Log.w(TAG, "Could not play ${file.name}", it)
            stop()
        }
    }

    private fun requestFocus(): Boolean {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(media)
            .setOnAudioFocusChangeListener { change ->
                // A call or another app took audio: stop rather than talk over it.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
            }
            .build()
        focus = req
        return audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
    }

    private companion object {
        const val TAG = "AudioCuePlayer"
    }
}
