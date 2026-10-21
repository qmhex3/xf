package com.frameout.app.full

class FormatCase(val ext: String, val fixture: String, val codec: String, val width: Int, val height: Int, val displayWidth: Int = width, val displayHeight: Int = height)

object FormatCases {
    val required = listOf(
        FormatCase("mkv", "fmt_mkv.mkv", "h264", 320, 240),
        FormatCase("webm", "fmt_webm.webm", "vp9", 320, 240),
        FormatCase("avi", "fmt_avi.avi", "mpeg4", 320, 240),
        FormatCase("wmv", "fmt_wmv.wmv", "wmv2", 320, 240),
        FormatCase("flv", "fmt_flv.flv", "flv1", 320, 240),
        FormatCase("ts", "fmt_ts.ts", "h264", 320, 240),
        FormatCase("m2ts", "fmt_m2ts.m2ts", "h264", 320, 240),
        FormatCase("mts", "fmt_mts.mts", "h264", 320, 240),
        FormatCase("vob", "fmt_vob.vob", "mpeg2video", 720, 576),
        FormatCase("3gp", "fmt_3gp.3gp", "h263", 176, 144),
        FormatCase("rmvb", "fmt_rmvb.rmvb", "rv20", 320, 240),
        FormatCase("ogv", "fmt_ogv.ogv", "theora", 320, 240),
        FormatCase("prores", "fmt_prores.mov", "prores", 320, 240),
        FormatCase("dnxhr", "fmt_dnxhr.mov", "dnxhd", 320, 240),
        FormatCase("j2k", "fmt_j2k.mov", "jpeg2000", 320, 240),
        FormatCase("j2kgray", "fmt_j2k_gray.mov", "jpeg2000", 320, 240),
        FormatCase("dv", "fmt_dv.dv", "dvvideo", 720, 576, 768, 576),
        FormatCase("mxf", "fmt_mxf_dnxhd.mxf", "dnxhd", 1920, 1080)
    )

    fun byExt(ext: String): FormatCase = required.first { it.ext == ext }
}
