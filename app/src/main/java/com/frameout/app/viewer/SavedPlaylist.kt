package com.frameout.app.viewer

internal object SavedPlaylist {
    @Volatile
    var ids: List<Long> = emptyList()
}
