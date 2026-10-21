package com.frameout.app.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.frameout.codecpack.CodecPackBuildInfo
import com.frameout.codecpack.CodecPackLicense
import com.frameout.codecpack.CodecPackLicenses
import com.frameout.core.util.ModeStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.glass.GlassSurface
import com.frameout.designsystem.theme.FrameShapes
import com.frameout.designsystem.theme.FrameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CodecPackLicenseSection() {
    val context = LocalContext.current
    val info by produceState<CodecPackBuildInfo?>(null, context) {
        value = withContext(Dispatchers.IO) { CodecPackLicenses.read(context) }
    }
    val current = info ?: return
    AboutCard(ModeStrings.aboutCodecTitle(), ModeStrings.aboutCodecBody())
    if (!com.frameout.codecpack.CodecPack.isBundled(context)) BuildLine(current)
    current.licenses.forEach { LicenseCard(it) }
}

@Composable
private fun BuildLine(info: CodecPackBuildInfo) {
    val versions = info.licenses.mapNotNull { license -> license.version?.let { "${license.name} $it" } }
    val text = when {
        !info.installed -> ModeStrings.aboutCodecMissing()
        versions.isEmpty() || info.serial == null -> ModeStrings.aboutCodecLegacy()
        else -> ModeStrings.aboutCodecBuild(versions.joinToString(" · "), info.serial.toString())
    }
    FrameText(
        text,
        style = FrameTheme.typography.monoSmall.copy(textDirection = TextDirection.Content),
        color = FrameTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun LicenseCard(license: CodecPackLicense) {
    var expanded by remember { mutableStateOf(false) }
    val text = license.text
    val label = if (expanded) ModeStrings.aboutLicenseHide() else ModeStrings.aboutLicenseShow()
    GlassSurface(shape = FrameShapes.md, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            FrameText("${license.name} · ${license.spdx}", style = FrameTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr))
            license.version?.let {
                FrameText(ModeStrings.aboutVersion(it), style = FrameTheme.typography.monoSmall, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            if (text != null) {
                FrameText(label, style = FrameTheme.typography.label, color = FrameTheme.colors.primary, modifier = Modifier.frameClickable(onClickLabel = label) { expanded = !expanded }.padding(vertical = 10.dp))
                if (expanded) LicenseText(text)
            }
        }
    }
}

@Composable
private fun LicenseText(text: String) {
    val style = FrameTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr)
    Column {
        CodecPackLicenses.paragraphs(text).forEach { paragraph ->
            FrameText(paragraph, style = style, color = FrameTheme.colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
