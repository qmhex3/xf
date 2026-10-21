package com.frameout.core.util

import java.util.concurrent.CancellationException

fun <T> Result<T>.logged(tag: String): Result<T> {
    exceptionOrNull()?.let { if (it !is CancellationException) DiagLog.append(tag, describeFailure(it)) }
    return this
}

fun logFailure(tag: String, error: Throwable) {
    if (error !is CancellationException) DiagLog.append(tag, describeFailure(error))
}

internal fun describeFailure(error: Throwable): String {
    val base = "${error.javaClass.simpleName}: ${error.message}"
    val root = generateSequence(error) { it.cause?.takeIf { cause -> cause !== it } }.take(8).last()
    val frames = root.stackTrace.take(6).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
    val head = if (root !== error) " | cause ${root.javaClass.simpleName}: ${root.message}" else ""
    return if (frames.isEmpty()) base + head else "$base$head | at $frames"
}
