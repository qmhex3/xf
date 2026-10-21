package com.frameout.engine.extract

import android.graphics.Bitmap
import com.frameout.core.quality.LumaGrid
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal object SceneDetector {
    const val GRID = 16
    const val CELLS = GRID * GRID
    const val MIN_SCENE_CANDIDATES = 6
    private const val SUB = 3
    private const val RECENT = 4
    private const val EXTRA = 9
    private const val STATE_SIZE = CELLS * 2 + EXTRA
    private const val I_SINCE = CELLS * 2
    private const val I_PEND = I_SINCE + 1
    private const val I_AGE = I_SINCE + 2
    private const val I_DRIFT = I_SINCE + 3
    private const val I_RECENT_N = I_SINCE + 4
    private const val I_RECENT = I_SINCE + 5
    private const val FLAT_VAR = 1e-4f
    private const val FLAT_MEAN_DELTA = 0.12f
    private const val STRUCT_CUT = 0.35f
    private const val ZMAD_CUT = 0.10f
    private const val SPIKE_ABS = 1.0f
    private const val SPIKE_LOW = 0.45f
    private const val SPIKE_RATIO = 3.0f
    private const val DRIFT_HIGH = 1.6f
    private const val DRIFT_LOW = 0.35f
    private const val SETTLE = 0.30f
    private const val GROWTH = 0.04f
    private const val CONFIRM = 2
    private const val MAX_WAIT = 24

    fun signature(bitmap: Bitmap): FloatArray = signature(bitmap.width, bitmap.height) { x, y ->
        val p = bitmap.getPixel(x, y)
        (77 * ((p shr 16) and 0xFF) + 150 * ((p shr 8) and 0xFF) + 29 * (p and 0xFF)) shr 8
    }

    fun signature(w: Int, h: Int, luma: LumaGrid): FloatArray {
        val out = FloatArray(CELLS)
        if (w <= 0 || h <= 0) return out
        val span = GRID * SUB * 2
        for (row in 0 until GRID) {
            for (col in 0 until GRID) {
                var sum = 0
                for (sy in 0 until SUB) {
                    val y = (((row * SUB + sy) * 2 + 1) * h / span).coerceIn(0, h - 1)
                    for (sx in 0 until SUB) {
                        val x = (((col * SUB + sx) * 2 + 1) * w / span).coerceIn(0, w - 1)
                        sum += luma.at(x, y).coerceIn(0, 255)
                    }
                }
                out[row * GRID + col] = sum / (SUB * SUB * 255f)
            }
        }
        return out
    }

    fun changed(state: FloatArray?, current: FloatArray?): Boolean {
        if (state == null || current == null) return true
        if (state.size < CELLS || current.size < CELLS) return true
        return evaluate(state, current, null)
    }

    fun carry(state: FloatArray?, current: FloatArray?): FloatArray? {
        if (current == null) return state
        if (state == null || state.size < CELLS || current.size < CELLS) return current
        val next = FloatArray(STATE_SIZE)
        evaluate(state, current, next)
        return next
    }

    private fun evaluate(state: FloatArray, cur: FloatArray, out: FloatArray?): Boolean {
        val tracked = state.size >= STATE_SIZE
        val prevOffset = if (tracked) CELLS else 0
        val since = (if (tracked) state[I_SINCE].toInt() else 0) + 1
        var pend = if (tracked) state[I_PEND].toInt() else 0
        var age = if (tracked) state[I_AGE].toInt() else 0
        var lastDrift = if (tracked) state[I_DRIFT] else 0f
        val recentN = if (tracked) state[I_RECENT_N].toInt().coerceIn(0, RECENT) else 0
        val inst = score(state, prevOffset, cur)
        val drift = if (tracked) score(state, 0, cur) else inst
        val avg = recentAverage(state, recentN)
        var accept = false
        if (pend == 0) {
            if (since >= MIN_SCENE_CANDIDATES) {
                val spike = inst >= SPIKE_ABS || (inst >= SPIKE_LOW && inst >= SPIKE_RATIO * avg)
                if (spike || drift >= DRIFT_HIGH) {
                    pend = CONFIRM
                    age = 0
                    lastDrift = drift
                }
            }
        } else {
            age++
            if (drift < DRIFT_LOW) {
                pend = 0
                age = 0
            } else if (age >= MAX_WAIT) {
                accept = true
            } else if (inst >= SETTLE || drift - lastDrift > GROWTH) {
                pend = CONFIRM
                lastDrift = drift
            } else {
                pend--
                lastDrift = drift
                if (pend <= 0) accept = true
            }
        }
        if (out != null) writeState(state, cur, out, since, if (accept) max(pend, 1) else pend, age, lastDrift, recentN, inst)
        return accept
    }

    private fun recentAverage(state: FloatArray, recentN: Int): Float {
        if (recentN <= 0) return 0f
        var sum = 0f
        for (i in 0 until recentN) sum += state[I_RECENT + i]
        return sum / recentN
    }

    private fun writeState(state: FloatArray, cur: FloatArray, out: FloatArray, since: Int, pend: Int, age: Int, lastDrift: Float, recentN: Int, inst: Float) {
        System.arraycopy(state, 0, out, 0, CELLS)
        System.arraycopy(cur, 0, out, CELLS, CELLS)
        out[I_SINCE] = since.toFloat()
        out[I_PEND] = pend.toFloat()
        out[I_AGE] = age.toFloat()
        out[I_DRIFT] = lastDrift
        val keep = min(recentN, RECENT - 1)
        for (i in 0 until keep) out[I_RECENT + i] = state[I_RECENT + (recentN - keep) + i]
        out[I_RECENT + keep] = inst
        out[I_RECENT_N] = (keep + 1).toFloat()
    }

    private fun score(a: FloatArray, aOffset: Int, b: FloatArray): Float {
        var sumA = 0f
        var sumB = 0f
        for (i in 0 until CELLS) {
            sumA += a[aOffset + i]
            sumB += b[i]
        }
        val meanA = sumA / CELLS
        val meanB = sumB / CELLS
        var varA = 0f
        var varB = 0f
        var cov = 0f
        var zmad = 0f
        for (i in 0 until CELLS) {
            val da = a[aOffset + i] - meanA
            val db = b[i] - meanB
            varA += da * da
            varB += db * db
            cov += da * db
            zmad += abs(da - db)
        }
        varA /= CELLS
        varB /= CELLS
        cov /= CELLS
        zmad /= CELLS
        val structure = if (varA < FLAT_VAR && varB < FLAT_VAR) {
            if (abs(meanA - meanB) > FLAT_MEAN_DELTA) 1f else 0f
        } else {
            1f - cov / sqrt((varA + FLAT_VAR) * (varB + FLAT_VAR))
        }
        return max(structure / STRUCT_CUT, zmad / ZMAD_CUT)
    }
}
