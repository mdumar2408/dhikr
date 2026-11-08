package com.dhikr.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColor
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.dhikr.app.MainActivity
import com.dhikr.app.appContainer
import com.dhikr.app.design.dotmatrix.DotGrid
import com.dhikr.app.design.dotmatrix.DotMatrixBitmap
import com.dhikr.app.design.dotmatrix.DotMatrixRenderer
import com.dhikr.app.design.theme.DarkColors
import com.dhikr.app.design.theme.LightColors
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Home-screen widget (Phase 6, android-widget-engineering).
 * - Reads the same CounterEngine as the app — no counter of its own.
 * - Per-instance state holds only the pinned dhikr id (absent = follow Flow).
 * - Dot-matrix numbers/Arabic are ALPHA_8 masks tinted per theme (bitmap-memory friendly).
 */
class DhikrWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, EXPANDED))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        // A cold start loads routines and counts first (usually < 100 ms); don't render a false "empty".
        withTimeoutOrNull(3_000) { container.engine.awaitLoaded() }
        provideContent {
            val engineState by container.engine.state.collectAsState()
            val prefs by container.preferences.prefs.collectAsState()
            val pinned = currentState<Preferences>()[KEY_PINNED]
            val model = WidgetModel.project(engineState, prefs.feel.afterTarget, pinned)
            WidgetBody(model)
        }
    }

    companion object {
        val KEY_PINNED = longPreferencesKey("pinned_dhikr_id")
        val COMPACT = DpSize(110.dp, 110.dp)
        val EXPANDED = DpSize(250.dp, 110.dp)
    }
}

class DhikrWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DhikrWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightRefresh.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MidnightRefresh.cancel(context)
    }
}

// ---- Rendering ---------------------------------------------------------------------------------

private val Canvas = ColorProvider(day = LightColors.surface, night = DarkColors.canvas)
private val Ink = ColorProvider(day = LightColors.ink, night = DarkColors.ink)
private val InkMuted = ColorProvider(day = LightColors.inkMuted, night = DarkColors.inkMuted)
private val DotOff = ColorProvider(day = LightColors.dotOff, night = DarkColors.dotOff)
private val Ember = ColorProvider(day = LightColors.ember, night = DarkColors.ember)

private fun label(color: GlanceColor = InkMuted, size: Int = 11) =
    TextStyle(color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace)

@Composable
private fun WidgetBody(model: WidgetModel) {
    val size = LocalSize.current
    val increment = actionRunCallback<IncrementAction>()
    val openApp = actionStartActivity<MainActivity>()
    Box(
        GlanceModifier.fillMaxSize().background(Canvas).cornerRadius(28.dp).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            model.empty -> Column(
                GlanceModifier.fillMaxSize().clickable(openApp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("DHIKR", style = label(Ink, 13))
                Spacer(GlanceModifier.height(6.dp))
                Text("Choose daily dhikr in the app", style = label(InkMuted, 11).copy(textAlign = TextAlign.Center))
            }

            size.width >= DhikrWidget.EXPANDED.width -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // Left: the count — tap to count.
                Column(
                    GlanceModifier.defaultWeight().fillMaxHeight().clickable(increment)
                        .semantics { contentDescription = "Count ${model.title}, ${model.count} of ${model.target}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DotNumber(model.count, model.complete, widthDp = 110, heightDp = 46)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(if (model.complete) "COMPLETE" else "/ ${model.target}", style = label(if (model.complete) Ember else InkMuted))
                    Spacer(GlanceModifier.height(6.dp))
                    BeadRow(model.fraction, model.complete, widthDp = 110)
                }
                Spacer(GlanceModifier.width(8.dp))
                // Right: what to say — tap to open the app.
                Column(
                    GlanceModifier.defaultWeight().fillMaxHeight().clickable(openApp),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArabicDots(model.arabic, widthDp = 120, heightDp = 30)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(model.title, style = TextStyle(color = Ink, fontSize = 13.sp, textAlign = TextAlign.End), maxLines = 2)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(model.modeLabel.uppercase() + if (model.hasAudio) " · ♪" else "", style = label())
                }
            }

            else -> Column(
                GlanceModifier.fillMaxSize().clickable(increment)
                    .semantics { contentDescription = "Count ${model.title}, ${model.count} of ${model.target}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model.title.uppercase(), style = label(Ink, 10), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                DotNumber(model.count, model.complete, widthDp = 84, heightDp = 38)
                Spacer(GlanceModifier.height(4.dp))
                Text(if (model.complete) "COMPLETE" else "/ ${model.target}", style = label(if (model.complete) Ember else InkMuted, 10))
            }
        }
    }
}

/** px for a dp size, capped so masks stay small (RemoteViews bitmap memory). */
@Composable
private fun px(dp: Int): Int {
    val density = LocalContext.current.resources.displayMetrics.density
    return (dp * density).roundToInt().coerceAtMost(dp * 3)
}

@Composable
private fun DotNumber(value: Int, complete: Boolean, widthDp: Int, heightDp: Int) {
    val w = px(widthDp)
    val h = px(heightDp)
    val masks = remember(value, w, h) {
        val grid = DotGrid.text(value.toString())
        DotMatrixBitmap.renderMask(grid, w, h, lit = false) to DotMatrixBitmap.renderMask(grid, w, h, lit = true)
    }
    Dots(masks, if (complete) Ember else Ink, widthDp, heightDp, value.toString())
}

@Composable
private fun BeadRow(fraction: Float, complete: Boolean, widthDp: Int) {
    val beads = 14
    val w = px(widthDp)
    val h = px(8)
    val lit = floor(fraction * beads).toInt()
    val masks = remember(lit, w, h) {
        val grid = DotGrid(beads, 1, BooleanArray(beads) { it < lit })
        DotMatrixBitmap.renderMask(grid, w, h, lit = false, dotRatio = 0.55f) to
            DotMatrixBitmap.renderMask(grid, w, h, lit = true, dotRatio = 0.55f)
    }
    Dots(masks, if (complete) Ember else Ink, widthDp, 8, "${(fraction * 100).toInt()} percent")
}

@Composable
private fun ArabicDots(text: String, widthDp: Int, heightDp: Int) {
    if (text.isBlank()) return
    val w = px(widthDp)
    val h = px(heightDp)
    // Shaped by Android (joined letters), then sampled into dots — same renderer as the app.
    val mask: Bitmap? = remember(text, w, h) {
        runCatching { DotMatrixBitmap.renderMask(DotMatrixRenderer.rasterize(text, rows = 14), w, h, lit = true) }.getOrNull()
    }
    if (mask != null) {
        Image(
            provider = ImageProvider(mask), contentDescription = text,
            modifier = GlanceModifier.size(widthDp.dp, heightDp.dp), colorFilter = ColorFilter.tint(Ink),
        )
    } else {
        Text(text, style = TextStyle(color = Ink, fontSize = 16.sp, textAlign = TextAlign.End), maxLines = 1)
    }
}

@Composable
private fun Dots(masks: Pair<Bitmap, Bitmap>, litColor: GlanceColor, widthDp: Int, heightDp: Int, description: String) {
    Box(GlanceModifier.size(widthDp.dp, heightDp.dp)) {
        Image(ImageProvider(masks.first), contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(DotOff))
        Image(ImageProvider(masks.second), contentDescription = description,
            modifier = GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(litColor))
    }
}

