package com.frameout.engine
import android.media.MediaFormat
import com.frameout.engine.decode.*
import java.io.File
fun main(args: Array<String>) {
    val raw = File(args[1]).readBytes(); val n = 512 * 256
    val m = HdrToneMapper(ColorParams(true, true, MediaFormat.COLOR_TRANSFER_ST2084, 1000f))
    val out = File(args[0]).outputStream().buffered()
    for (i in 0 until n) {
        fun s(c: Int) = (((raw[(3 * i + c) * 2].toInt() and 255) or ((raw[(3 * i + c) * 2 + 1].toInt() and 255) shl 8))) / 65535f
        val p = m.map(s(0), s(1), s(2))
        out.write((p shr 16) and 255); out.write((p shr 8) and 255); out.write(p and 255)
    }
    out.close()
}
