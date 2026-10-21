package com.frameout.app.batch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.frameout.app.trim.TrimScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frameout.app.PreferredFrameTheme
import com.frameout.core.util.FrameStrings
import com.frameout.designsystem.components.FrameConfirmDialog
import com.frameout.designsystem.components.FrameTopBar
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow

@Composable
fun BatchScreen(
    uris: List<String>,
    session: Long,
    onDone: () -> Unit,
    onOpenProject: (Long) -> Unit,
    vm: BatchViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val gate = com.frameout.app.billing.rememberProGate()
    val prep by vm.prep.collectAsState()
    val starting by vm.starting.collectAsState()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }
    LaunchedEffect(uris, session) { vm.prepare(uris, session) }
    val editItem = prep.firstOrNull { it.uri == editing }
    BackHandler(enabled = editItem != null) { editing = null }
    PreferredFrameTheme {
        when {
            editItem != null -> key(editItem.uri) {
                TrimScreen(
                    uri = Uri.parse(editItem.uri),
                    onBack = { editing = null },
                    onExtract = { _, range, _ ->
                        vm.setRange(editItem.uri, range.takeIf { !it.useFull })
                        editing = null
                    },
                    batchEdit = true,
                    initialRange = editItem.range
                )
            }
            state.items.isNotEmpty() -> Column(Modifier.fillMaxSize().background(FrameTheme.colors.background).frameAmbientGlow()) {
                FrameTopBar(title = FrameStrings.batchQueue(), onBack = onDone, backContentDescription = FrameStrings.back())
                if (confirmCancel) CancelDialog({ confirmCancel = false; vm.cancelAll() }, { confirmCancel = false })
                BatchList(state, onOpenProject, Modifier.weight(1f))
                BatchBottomBar(state.done, onDone) { confirmCancel = true }
            }
            else -> BatchPrepare(
                items = prep,
                starting = starting,
                prefs = vm.prefs,
                onBack = onDone,
                onEdit = { editing = it.uri },
                onRemove = vm::remove,
                onStart = { gate.run(com.frameout.core.pro.ProFeature.Batch) { vm.start() } },
                hasSpace = vm::hasSpace
            )
        }
    }
}

@Composable
private fun BatchList(state: BatchState, onOpen: (Long) -> Unit, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "summary") { BatchSummary(state) }
        items(state.items, key = { it.projectId }) { item ->
            BatchRow(item, onOpen = { onOpen(item.projectId) })
        }
    }
}

@Composable
private fun CancelDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    FrameConfirmDialog(
        title = FrameStrings.cancelExtract(),
        body = FrameStrings.confirmCancelExtractBody(),
        confirmLabel = FrameStrings.cancelExtract(),
        dismissLabel = FrameStrings.keepExtracting(),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
        icon = com.frameout.designsystem.icons.FrameIcons.Close
    )
}
