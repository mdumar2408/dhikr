package com.dhikr.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhikr.app.design.components.PillChip
import com.dhikr.app.design.dotmatrix.DotMatrixLabel
import com.dhikr.app.design.theme.DhikrTheme

/** Library tab: the dhikr library and the after-prayer set editor, side by side as two views. */
@Composable
fun LibraryHost(library: LibraryViewModel, audio: com.dhikr.app.audio.AudioCuePlayer, prayerSet: PrayerSetViewModel, startOnPrayerSet: Boolean = false) {
    val sp = DhikrTheme.spacing
    var showPrayerSet by rememberSaveable { mutableStateOf(startOnPrayerSet) }
    Column(Modifier.fillMaxSize().background(DhikrTheme.colors.canvas)) {
        Column(Modifier.padding(horizontal = sp.gutter)) {
            DotMatrixLabel("LIBRARY", Modifier.padding(top = sp.l).width(150.dp).height(22.dp), maxPitch = 4.dp)
            Spacer(Modifier.height(sp.m))
            Row(horizontalArrangement = Arrangement.spacedBy(sp.xs)) {
                PillChip("All dhikr", !showPrayerSet, { showPrayerSet = false })
                PillChip("After prayer", showPrayerSet, { showPrayerSet = true })
            }
            Spacer(Modifier.height(sp.m))
        }
        if (showPrayerSet) PrayerSetScreen(prayerSet) else LibraryScreen(library, audio)
    }
}
