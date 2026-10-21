package com.frameout.codecpack

enum class FormatKind {
    System,
    Gif,
    Apng,
    AnimatedWebp,
    AnimatedImage,
    NeedsFfmpeg,
    Unsupported
}

data class FormatInfo(
    val mime: String,
    val container: String,
    val kind: FormatKind,
    val message: String = "",
    val failure: MediaFailure = MediaFailure.None
)
