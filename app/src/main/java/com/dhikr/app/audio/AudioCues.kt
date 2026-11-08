package com.dhikr.app.audio

import com.dhikr.app.core.model.Dhikr
import com.dhikr.app.core.model.SoundSettings
import com.dhikr.app.core.model.SwitchSound
import com.dhikr.app.domain.flow.FlowEvent

/** What to play for one counter event. Pure decision; playing is AudioCuePlayer's job. */
sealed interface Cue {
    data object None : Cue

    /** Short synthesized chime; [count] = 2 for "routine / set complete". */
    data class Chime(val count: Int = 1) : Cue

    /**
     * Play a dhikr's own recording. [interrupt] = stop whatever is playing first (the switch cue);
     * otherwise only start if nothing is playing (no overlapping phrases while counting).
     */
    data class Recite(val dhikr: Dhikr, val interrupt: Boolean) : Cue
}

object AudioCues {

    /**
     * - A switch (target reached → next dhikr): the chosen switch sound. "Recite next" falls back
     *   to the chime when the next dhikr has no recording, so the switch is never silent by accident.
     * - Routine / prayer set complete: a double chime (unless switch sound is Off).
     * - Ordinary counts: the current dhikr's recording if "recite while counting" is on — only when
     *   idle, so fast tapping never stacks phrases.
     */
    fun forEvent(event: FlowEvent, sound: SoundSettings): Cue = when (event) {
        is FlowEvent.TargetReached -> when (sound.switchSound) {
            SwitchSound.Off -> None
            SwitchSound.Chime -> Cue.Chime()
            SwitchSound.ReciteNext ->
                if (event.next.audioRef != null && event.next.id != event.completed.id) Cue.Recite(event.next, interrupt = true)
                else Cue.Chime()
        }
        is FlowEvent.RoutineComplete -> if (sound.switchSound == SwitchSound.Off) None else Cue.Chime(count = 2)
        is FlowEvent.Counted ->
            if (sound.reciteWhileCounting && event.dhikr.audioRef != null) Cue.Recite(event.dhikr, interrupt = false) else None
        FlowEvent.NothingEnabled -> None
    }

    private val None = Cue.None
}
