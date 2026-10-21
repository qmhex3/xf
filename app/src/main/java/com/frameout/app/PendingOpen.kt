package com.frameout.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

object PendingOpen {
    var projectId by mutableLongStateOf(-1L)
}
