package com.frameout.core.quality

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class SkipReason { Blurry, Duplicate, NoPeople, NoMatch }

data class SkipCounts(val blurry: Int = 0, val duplicate: Int = 0, val noPeople: Int = 0, val noMatch: Int = 0) {
    val total: Int get() = blurry + duplicate + noPeople + noMatch

    operator fun plus(reason: SkipReason): SkipCounts = when (reason) {
        SkipReason.Blurry -> copy(blurry = blurry + 1)
        SkipReason.Duplicate -> copy(duplicate = duplicate + 1)
        SkipReason.NoPeople -> copy(noPeople = noPeople + 1)
        SkipReason.NoMatch -> copy(noMatch = noMatch + 1)
    }
}

object SkipStats {
    private val state = MutableStateFlow<Map<Long, SkipCounts>>(emptyMap())

    fun flow(projectId: Long): Flow<SkipCounts> = state.map { it[projectId] ?: SkipCounts() }.distinctUntilChanged()

    fun record(projectId: Long, reason: SkipReason) = state.update { it + (projectId to ((it[projectId] ?: SkipCounts()) + reason)) }

    fun reset(projectId: Long) = state.update { it - projectId }
}
