package com.frameout.core.export

object CsvText {
    const val BOM = "﻿"
    const val EOL = "\r\n"

    fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
