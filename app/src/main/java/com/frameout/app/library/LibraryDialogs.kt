package com.frameout.app.library

import androidx.compose.runtime.Composable
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameConfirmDialog

internal sealed interface DeleteRequest {
    data class Project(val id: Long) : DeleteRequest
    data class Projects(val count: Int) : DeleteRequest
    data class Saved(val count: Int) : DeleteRequest
}

@Composable
internal fun ConfirmDeleteDialog(request: DeleteRequest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title: String
    val body: String
    when (request) {
        is DeleteRequest.Project -> {
            title = FrameStrings.confirmDeleteProject()
            body = FrameStrings.confirmDeleteProjectsBody()
        }
        is DeleteRequest.Projects -> {
            title = FrameStrings.confirmDeleteProjects(request.count)
            body = FrameStrings.confirmDeleteProjectsBody()
        }
        is DeleteRequest.Saved -> {
            title = FrameStrings.confirmDeleteSaved(request.count)
            body = FrameStrings.confirmDeleteSavedBody()
        }
    }
    FrameConfirmDialog(title, body, FrameStrings.delete(), FrameStrings.cancel(), onConfirm, onDismiss, destructive = true)
}
