package com.frameout.app.security

internal object Mask {
    private val KEY = intArrayOf(0x5b, 0x2e, 0x71, 0x3c, 0x6a)

    val frida = decode(61, 92, 24, 88, 11)
    val gum = decode(60, 91, 28, 17, 0, 40)
    val substrate = decode(55, 71, 19, 79, 31, 57, 93, 5, 78, 11, 47, 75)
    val lspd = decode(55, 93, 1, 88)
    val xposed = decode(35, 94, 30, 79, 15, 63)
    val bridge = decode(63, 75, 95, 78, 5, 57, 88, 95, 93, 4, 63, 92, 30, 85, 14, 117, 86, 1, 83, 25, 62, 74, 95, 100, 26, 52, 93, 20, 88, 40, 41, 71, 21, 91, 15)
    val tracer = decode(15, 92, 16, 95, 15, 41, 126, 24, 88, 80)

    private fun decode(vararg data: Int): String =
        String(CharArray(data.size) { (data[it] xor KEY[it % KEY.size]).toChar() })
}
