package com.frameout.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frameout.core.prefs.AppPrefs
import com.frameout.core.quality.content.ContentSpec
import com.frameout.core.util.FrameStrings
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.icons.FrameIconsExtra
import com.frameout.engine.extract.ExtractMode
import com.frameout.engine.extract.ExtractModeCodec

internal val ModeKinds = listOf("every_frame", "every_n", "every_time", "keyframes", "fixed", "scene", "slides", "content")
private const val MAX_FRAMES = 100000L
private const val MAX_SECONDS = 86400L
private const val MAX_MILLIS = 86400000L

internal fun kindOf(mode: ExtractMode) = when (mode) {
    ExtractMode.EveryFrame -> "every_frame"
    is ExtractMode.EveryNFrames -> "every_n"
    is ExtractMode.EveryNMillis -> "every_time"
    ExtractMode.KeyframesOnly -> "keyframes"
    is ExtractMode.FixedCount -> "fixed"
    ExtractMode.SceneChange -> "scene"
    ExtractMode.Slides -> "slides"
    is ExtractMode.Content -> "content"
}

internal fun modeFor(kind: String, previous: ExtractMode) = when (kind) {
    "every_n" -> ExtractMode.EveryNFrames(if (previous is ExtractMode.EveryNFrames) previous.n else 5)
    "every_time" -> ExtractMode.EveryNMillis(if (previous is ExtractMode.EveryNMillis) previous.millis else 1000L)
    "fixed" -> ExtractMode.FixedCount(if (previous is ExtractMode.FixedCount) previous.count else 30)
    "keyframes" -> ExtractMode.KeyframesOnly
    "scene" -> ExtractMode.SceneChange
    "slides" -> ExtractMode.Slides
    "content" -> ExtractMode.Content(if (previous is ExtractMode.Content) previous.spec else ContentSpec())
    else -> ExtractMode.EveryFrame
}

@Composable
fun ExtractModeSettings(prefs: AppPrefs, stored: String, onOpenSheet: () -> Unit) {
    val mode = ExtractModeCodec.decode(stored)
    val kind = kindOf(mode)
    val save = { next: ExtractMode -> prefs.setExtractMode(ExtractModeCodec.encode(next)) }
    NavRow(FrameIconsExtra.Keyframe, FrameStrings.defaultMode(), value = FrameStrings.settingValue(kind), onClick = onOpenSheet)
    SettingsHint(ModeStrings.modeHelp(kind))
    key(kind) {
        when (mode) {
            is ExtractMode.EveryNFrames -> {
                SettingsDivider(0.dp)
                SettingsNumberField(ModeStrings.framesLabel(), mode.n.toLong(), MAX_FRAMES) { save(ExtractMode.EveryNFrames(it.toInt())) }
            }
            is ExtractMode.FixedCount -> {
                SettingsDivider(0.dp)
                SettingsNumberField(ModeStrings.countLabel(), mode.count.toLong(), MAX_FRAMES) { save(ExtractMode.FixedCount(it.toInt())) }
            }
            is ExtractMode.EveryNMillis -> TimeModeFields(mode.millis) { save(ExtractMode.EveryNMillis(it)) }
            is ExtractMode.Content -> ContentSettings(mode.spec) { save(ExtractMode.Content(it)) }
            else -> Unit
        }
    }
}

@Composable
private fun TimeModeFields(millis: Long, onCommit: (Long) -> Unit) {
    var unit by rememberSaveable { mutableStateOf(if (millis % 1000L == 0L) "s" else "ms") }
    SettingsDivider(0.dp)
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
        SettingsSegmented(listOf(SegmentOption("s", ModeStrings.seconds()), SegmentOption("ms", ModeStrings.millis())), unit) { unit = it }
    }
    key(unit) {
        val seconds = unit == "s"
        val shown = if (seconds) (millis / 1000L).coerceAtLeast(1L) else millis
        val max = if (seconds) MAX_SECONDS else MAX_MILLIS
        SettingsNumberField(ModeStrings.timeLabel(), shown, max, if (seconds) 1L else 100L) { onCommit(if (seconds) it * 1000L else it) }
    }
}

internal fun isSmartKind(kind: String): Boolean = kind == "scene" || kind == "slides" || kind == "content"

internal fun gateMode(kind: String, action: () -> Unit) {
    val smart = isSmartKind(kind)
    com.frameout.core.pro.ProAccess.source?.gate(com.frameout.core.pro.ProFeature.SmartExtract, !smart, kind, action) ?: action()
}

internal fun gateSmart(on: Boolean, item: String, action: () -> Unit) {
    com.frameout.core.pro.ProAccess.source?.gate(com.frameout.core.pro.ProFeature.SmartExtract, !on, item, action) ?: action()
}
