package com.frameout.app.update

import androidx.annotation.DrawableRes
import com.frameout.core.util.UpdateStrings
import com.frameout.designsystem.icons.FrameIconsExtra

data class UpdateFeature(@DrawableRes val icon: Int, val title: String, val body: String)

data class UpdateFixes(val count: Int, val body: String)

data class UpdateInfo(
    val versionName: String,
    val sizeLabel: String,
    val features: List<UpdateFeature>,
    val fixes: UpdateFixes? = null
) {
    companion object {
        fun releaseNotes(versionName: String, sizeLabel: String) = UpdateInfo(
            versionName = versionName,
            sizeLabel = sizeLabel,
            features = listOf(
                UpdateFeature(FrameIconsExtra.Tag, UpdateStrings.feature1Title(), UpdateStrings.feature1Body()),
                UpdateFeature(FrameIconsExtra.Package, UpdateStrings.feature2Title(), UpdateStrings.feature2Body()),
                UpdateFeature(FrameIconsExtra.Bolt, UpdateStrings.feature3Title(), UpdateStrings.feature3Body())
            ),
            fixes = UpdateFixes(3, UpdateStrings.fixesBody())
        )
    }
}
