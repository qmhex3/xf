package com.frameout.app.oem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frameout.app.PreferredFrameTheme
import com.frameout.app.onboarding.OnboardingBrand
import com.frameout.app.onboarding.OnboardingCta
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.a11y.frameClickable
import com.frameout.designsystem.a11y.screenHeading
import com.frameout.designsystem.components.FrameText
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow

@Composable
fun OemGuideScreen(
    oem: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    PreferredFrameTheme {
        val colors = FrameTheme.colors
        Box(Modifier.fillMaxSize().background(colors.background).frameAmbientGlow()) {
            BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    OnboardingBrand(Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(32.dp))
                    FrameText(
                        FrameStrings.backgroundSetup(),
                        style = FrameTheme.typography.display.copy(fontSize = 26.sp, lineHeight = 34.sp),
                        modifier = Modifier.screenHeading()
                    )
                    Spacer(Modifier.height(8.dp))
                    FrameText(
                        FrameStrings.oemHint(),
                        style = FrameTheme.typography.body.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    OemStepsCard(oem, onOpenBattery = { OemSettings.openBatteryList(context) })
                    Spacer(Modifier.height(24.dp))
                    OnboardingCta(
                        label = FrameStrings.openAppSettings(),
                        last = false,
                        onClick = { OemSettings.openAutostartOrBattery(context, oem) }
                    )
                    FrameText(
                        FrameStrings.continueLabel(),
                        style = FrameTheme.typography.titleSmall.copy(fontSize = 14.sp),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .frameClickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
