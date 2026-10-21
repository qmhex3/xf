package com.frameout.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.frameout.designsystem.R

val FrameFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold)
)
val FrameMonoFamily: FontFamily = FontFamily.Monospace
data class FrameTypography(
    val display: TextStyle, val title: TextStyle, val titleSmall: TextStyle, val body: TextStyle,
    val bodySmall: TextStyle, val label: TextStyle, val mono: TextStyle, val monoSmall: TextStyle
)
fun frameTypography(): FrameTypography {
    val f = FrameFontFamily
    val m = FrameMonoFamily
    return FrameTypography(
        TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
        TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
        TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
        TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        TextStyle(fontFamily = m, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp),
        TextStyle(fontFamily = m, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp)
    )
}
