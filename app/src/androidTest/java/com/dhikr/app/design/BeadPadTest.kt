package com.dhikr.app.design

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dhikr.app.design.components.BeadPad
import com.dhikr.app.design.theme.DhikrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** tactile-ui-engineering: every tap counts exactly once, no debounce, accessible as a button. */
@RunWith(AndroidJUnit4::class)
class BeadPadTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun fiftyRapidTapsCountFifty() {
        compose.setContent {
            DhikrTheme {
                var count by remember { mutableIntStateOf(0) }
                BeadPad(onTap = { count++ }, accessibilityLabel = "Count SubhanAllah") {
                    Text("count=$count")
                }
            }
        }
        val pad = compose.onNodeWithContentDescription("Count SubhanAllah")
        repeat(50) { pad.performClick() }
        compose.onNodeWithText("count=50").assertIsDisplayed()
    }
}
