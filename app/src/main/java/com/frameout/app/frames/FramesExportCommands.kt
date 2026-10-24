package com.frameout.app.frames

import com.frameout.core.util.FrameStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

private suspend fun FramesViewModel.completeExport(block: suspend () -> ExportOutcome) {
    exportStarted(currentCoroutineContext()[Job])
    val outcome = try {
        block()
    } catch (e: CancellationException) {
        clearBusy()
        throw e
    } catch (e: Exception) {
        com.frameout.core.util.logFailure("FramesExport", e)
        ExportOutcome(FrameStrings.saveFailed())
    } finally {
        exportEnded()
    }
    postExport(outcome)
}

internal fun exportSelected(vm: FramesViewModel, exporter: FramesExportAction, scope: CoroutineScope, gif: Boolean) {
    if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.AdvancedExport)) return

    vm.framesForExport { frames ->
        if (frames.isEmpty()) return@framesForExport
        if (vm.exportRunning) return@framesForExport
        scope.launch {
            vm.postBusy(FrameStrings.saving())
            val title = vm.state.value.project?.name ?: FrameStrings.frames()
            vm.completeExport { if (gif) exporter.gif(frames) else exporter.pdf(frames, title) }
        }
    }
}

internal fun exportZip(vm: FramesViewModel, exporter: FramesExportAction, scope: CoroutineScope) {
    if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.AdvancedExport)) return

    vm.framesForExport { frames ->
        if (frames.isEmpty()) return@framesForExport
        if (vm.exportRunning) return@framesForExport
        scope.launch {
            vm.postBusy(FrameStrings.saving())
            val title = vm.state.value.project?.name ?: FrameStrings.frames()
            vm.completeExport { exporter.zip(frames, title) }
        }
    }
}

internal fun exportContactSheet(vm: FramesViewModel, exporter: FramesExportAction, scope: CoroutineScope, options: com.frameout.core.export.ContactSheetLayout.Options) {
    vm.framesForExport { frames ->
        if (frames.isEmpty()) {
            vm.postMessage(FrameStrings.contactNeedsOne())
            return@framesForExport
        }
        if (vm.exportRunning) return@framesForExport
        scope.launch {
            vm.postBusy(FrameStrings.saving())
            val title = vm.state.value.project?.name ?: FrameStrings.frames()
            vm.completeExport { exporter.contactSheet(frames, title, options) }
        }
    }
}

internal fun exportCsv(vm: FramesViewModel, exporter: FramesExportAction, scope: CoroutineScope) {
    if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.AdvancedExport)) return

    vm.framesForExport { frames ->
        if (frames.isEmpty()) return@framesForExport
        if (vm.exportRunning) return@framesForExport
        scope.launch {
            vm.postBusy(FrameStrings.saving())
            vm.completeExport { exporter.csv(frames) }
        }
    }
}

internal fun exportEvidence(vm: FramesViewModel, exporter: FramesExportAction, scope: CoroutineScope) {
    if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.AdvancedExport)) return

    vm.framesForExport { frames ->
        if (frames.isEmpty()) return@framesForExport
        if (vm.exportRunning) return@framesForExport
        scope.launch {
            vm.postBusy(FrameStrings.saving())
            val title = vm.state.value.project?.name ?: FrameStrings.frames()
            vm.completeExport { exporter.evidence(frames, title) }
        }
    }
}

internal fun exportSlides(vm: FramesViewModel, exporter: FramesExportAction, scope: CoroutineScope, projectId: Long) {
    if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.AdvancedExport)) return

    if (vm.exportRunning) return
    scope.launch {
        vm.postBusy(FrameStrings.saving())
        vm.completeExport { exporter.slidesPdf(projectId) }
    }
}

internal fun compareSelected(vm: FramesViewModel, onCompare: (List<Long>) -> Unit) {
    if (!com.frameout.app.billing.ProCheck.pass(com.frameout.core.pro.ProFeature.Compare)) return

    vm.framesForSelection { frames ->
        if (frames.size == 2) onCompare(frames.map { it.id }) else vm.postMessage(FrameStrings.compareNeedsTwo())
    }
}

internal fun exportDataset(vm: FramesViewModel, action: DatasetExportAction, scope: CoroutineScope, projectId: Long, format: com.frameout.core.export.DatasetFormat, options: com.frameout.core.quality.DatasetOptions) {
    if (vm.exportRunning) return
    scope.launch {
        vm.postBusy(com.frameout.core.util.QualityStrings.analyzing())
        val title = vm.state.value.project?.name ?: FrameStrings.frames()
        vm.completeExport { action.export(projectId, title, format, options) }
    }
}
