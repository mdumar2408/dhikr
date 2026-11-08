package com.dhikr.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Phase 0: the single entry point of the app.
 *
 * `ComponentActivity` is the minimal AndroidX Activity that can host Jetpack Compose.
 * The OS calls [onCreate] when the activity is created (first launch, or re-creation
 * after a configuration change such as rotation or a theme switch).
 *
 * For now it only shows device facts so we can confirm, on the real Nothing Phone (2a),
 * which Android API level and build we are actually running on.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars (the Android 15+ default for apps targeting 35+).
        enableEdgeToEdge()
        // setContent replaces XML layouts: the UI is a tree of @Composable functions.
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                DeviceInfoScreen(DeviceInfo.current())
            }
        }
    }
}

/** Plain data describing the device. Read once from [Build]; no business logic in the UI. */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val buildDisplay: String,
) {
    companion object {
        fun current() = DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            // On Nothing phones this string contains the Nothing OS build identifier.
            buildDisplay = Build.DISPLAY,
        )
    }
}

@Composable
fun DeviceInfoScreen(info: DeviceInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("DHIKR", color = Color.White, fontSize = 40.sp, fontFamily = FontFamily.Monospace)
        Text("Phase 0 · environment check", color = Color.Gray, fontSize = 14.sp)
        InfoLine("Device", "${info.manufacturer} ${info.model}")
        InfoLine("Android", "${info.androidRelease} (API ${info.sdkInt})")
        InfoLine("Build", info.buildDisplay)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 18.sp)
    }
}

/** Previews render in Android Studio's design pane without a device. */
@Preview
@Composable
private fun DeviceInfoScreenPreview() {
    DeviceInfoScreen(
        DeviceInfo("Nothing", "A142", "16", 36, "Pacman-B4.1-preview"),
    )
}
