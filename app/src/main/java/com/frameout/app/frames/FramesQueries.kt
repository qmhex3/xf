package com.frameout.app.frames

import androidx.paging.PagingSource
import com.frameout.core.db.FrameEntity
import com.frameout.core.util.AppContainer

internal class FramesQueries(private val container: AppContainer) {
    suspend fun ids(projectId: Long, state: FramesState): List<Long> = when (state.filter) {
        FrameFilter.All, FrameFilter.Albums -> container.frames.listActiveIds(projectId)
        FrameFilter.Favorites -> container.frames.listFavoriteIds(projectId)
        FrameFilter.Deleted -> container.frames.listDeletedIds(projectId)
        FrameFilter.Tagged -> container.frameTags.listTaggedIds(projectId, state.tagFilter.toList(), state.tagFilter.size)
        FrameFilter.Sharpest -> container.sharpest.listIds(projectId)
    }

    suspend fun count(projectId: Long, state: FramesState): Int = when (state.filter) {
        FrameFilter.All, FrameFilter.Albums -> container.frames.countActive(projectId)
        FrameFilter.Favorites -> container.frames.countFavorites(projectId)
        FrameFilter.Deleted -> container.frames.countDeleted(projectId)
        FrameFilter.Tagged -> container.frameTags.countTagged(projectId, state.tagFilter.toList(), state.tagFilter.size)
        FrameFilter.Sharpest -> container.sharpest.count(projectId)
    }

    fun paging(projectId: Long, state: FramesState): PagingSource<Int, FrameEntity> = when (state.filter) {
        FrameFilter.All, FrameFilter.Albums -> container.frames.pagingActive(projectId)
        FrameFilter.Favorites -> container.frames.pagingFavorites(projectId)
        FrameFilter.Deleted -> container.frames.pagingDeleted(projectId)
        FrameFilter.Tagged -> container.frameTags.pagingTagged(projectId, state.tagFilter.toList(), state.tagFilter.size)
        FrameFilter.Sharpest -> container.sharpest.paging(projectId)
    }
}
