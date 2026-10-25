package com.frameout.app.picker

import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState
import com.frameout.core.media.FolderMediaIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.LocalContext
import com.frameout.app.thumb.PrefetchAhead
import com.frameout.core.media.MediaThumbLoader
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import com.frameout.core.media.MediaItem
import androidx.compose.foundation.layout.padding
import com.frameout.core.media.PermissionHelper
import com.frameout.core.util.FrameStrings
import com.frameout.app.PreferredFrameTheme
import com.frameout.designsystem.motion.frameSpring
import com.frameout.designsystem.theme.FrameTheme
import com.frameout.designsystem.theme.frameAmbientGlow

private val PICK_MIME_TYPES = arrayOf(
    "video/*", "image/*", "application/x-matroska", "application/mxf",
    "application/vnd.rn-realmedia", "application/vnd.rn-realmedia-vbr", "application/octet-stream"
)

private class PickMediaChooser : ActivityResultContract<Array<String>, List<Uri>>() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        val get = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(get, null)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val out = LinkedHashSet<Uri>()
        intent.clipData?.let { clip -> for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(out::add) }
        intent.data?.let(out::add)
        return out.toList()
    }
}

private class PermissionFlow(
    val showRationale: Boolean,
    val permanentDenied: Boolean,
    val requestOrSettings: () -> Unit,
    val requestAccess: () -> Unit
)

private fun openAppSettings(context: Context) {
    try {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        i.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(i)
    } catch (_: Exception) {
    }
}

@Composable
private fun rememberPermissionFlow(vm: PickerViewModel): PermissionFlow {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var showRationale by remember { mutableStateOf(false) }
    var permanentDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        vm.refreshPermission(force = true)
        if (!PermissionHelper.hasMediaAccess(context)) {
            val anyRationale = activity != null && PermissionHelper.mediaPermissions().any { activity.shouldShowRequestPermissionRationale(it) }
            permanentDenied = !anyRationale
        }
    }
    LaunchedEffect(Unit) {
        if (PermissionHelper.hasMediaAccess(context)) vm.refreshPermission() else showRationale = true
    }
    val request = { launcher.launch(PermissionHelper.mediaPermissions()) }
    val requestOrSettings = {
        if (permanentDenied) openAppSettings(context) else {
            showRationale = false
            request()
        }
    }
    return PermissionFlow(showRationale, permanentDenied, requestOrSettings, { request() })
}

@Composable
private fun rememberSafLauncher(vm: PickerViewModel, onContinue: (List<Uri>) -> Unit): androidx.activity.result.ActivityResultLauncher<Array<String>> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(PickMediaChooser()) { uris ->
        uris.forEach { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        if (uris.isNotEmpty()) {
            vm.clearSelection()
            onContinue(uris)
        }
    }
}

@Composable
private fun PickerSelectionLifecycle(vm: PickerViewModel, active: Boolean) {
    val activity = LocalActivity.current
    androidx.activity.compose.BackHandler(enabled = active) { vm.clearSelection() }
    DisposableEffect(vm) {
        onDispose { if (activity?.isChangingConfigurations != true) vm.clearSelection() }
    }
}

@Composable
fun PickerScreen(onContinue: (List<Uri>) -> Unit, onClose: () -> Unit, vm: PickerViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val pages = vm.paging.collectAsLazyPagingItems()
    val context = LocalContext.current
    val permission = rememberPermissionFlow(vm)
    val safLauncher = rememberSafLauncher(vm, onContinue)
    PickerSelectionLifecycle(vm, state.selected.isNotEmpty())
    val partial = PermissionHelper.isPartialMediaAccess(context)
    val selected = state.selected.values.toList()
    val flight = remember { PickerFlight() }
    PreferredFrameTheme {
        CompositionLocalProvider(LocalPickerFlight provides flight) {
            Box(Modifier.fillMaxSize().background(FrameTheme.colors.background).frameAmbientGlow()) {
                var headerPx by remember { mutableIntStateOf(0) }
                val headerDp = with(LocalDensity.current) { headerPx.toDp() }
                CompositionLocalProvider(LocalPickerTopInset provides headerDp) {
                    PickerContent(state, pages, selected, vm, permission, Modifier.fillMaxSize())
                }
                PickerHeader(
                    state, vm, { headerPx = it }, { vm.clearSelection(); onClose() },
                    { safLauncher.launch(PICK_MIME_TYPES) }, if (partial) permission.requestAccess else null,
                    Modifier.align(Alignment.TopCenter)
                )
                PickerTray(selected, vm, {
                    val picked = state.selected.keys.map(Uri::parse)
                    vm.clearSelection()
                    onContinue(picked)
                }, Modifier.align(Alignment.BottomCenter))
                PickerFlightLayer(flight, state.selected.keys)
            }
        }
    }
}

internal val LocalPickerTopInset = androidx.compose.runtime.compositionLocalOf { 0.dp }

@Composable
private fun PickerContent(
    state: PickerState,
    pages: LazyPagingItems<MediaItem>,
    selected: List<MediaItem>,
    vm: PickerViewModel,
    permission: PermissionFlow,
    modifier: Modifier
) {
    val denied = !state.permissionGranted
    val inset = LocalPickerTopInset.current
    val padded = modifier.padding(top = inset)
    val refresh = pages.loadState.refresh
    val switching = rememberSwitching(state.generation, pages)
    val empty = pages.itemCount == 0
    when {
        permission.showRationale && denied -> PickerMessage(FrameStrings.permissionRationale(), padded, FrameStrings.manageAccess()) { permission.requestOrSettings() }
        denied && !state.loading -> PickerMessage(
            if (permission.permanentDenied) FrameStrings.permissionDeniedPermanent() else FrameStrings.mediaPermissionRequired(),
            padded,
            if (permission.permanentDenied) FrameStrings.openAppSettings() else FrameStrings.manageAccess()
        ) { permission.requestOrSettings() }
        switching || (empty && (state.loading || refresh is LoadState.Loading)) -> Box(padded) {
            PickerSkeleton(Modifier.fillMaxSize())
            PickerLoadingBar(Modifier.align(Alignment.TopCenter))
        }
        empty && state.error != null -> PickerMessage(state.error, padded)
        empty && refresh is LoadState.Error -> PickerMessage(refresh.error.message ?: FrameStrings.scanFailed(), padded)
        empty -> PickerMessage(FrameStrings.noMediaFound(), padded)
        else -> Box(modifier) {
            MediaGrid(pages, selected, vm, Modifier.fillMaxSize())
            if (refresh is LoadState.Loading) PickerLoadingBar(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun rememberSwitching(generation: Int, pages: LazyPagingItems<MediaItem>): Boolean {
    var settled by remember { mutableStateOf(generation) }
    LaunchedEffect(generation) {
        if (settled != generation) {
            val started = withTimeoutOrNull(SWITCH_GRACE_MS) {
                snapshotFlow { pages.loadState.refresh is LoadState.Loading }.first { it }
            } != null
            if (started) snapshotFlow { pages.loadState.refresh is LoadState.Loading }.first { !it }
            settled = generation
        }
    }
    return settled != generation
}

private const val SWITCH_GRACE_MS = 400L

@Composable
private fun MediaGrid(pages: LazyPagingItems<MediaItem>, selected: List<MediaItem>, vm: PickerViewModel, modifier: Modifier) {
    val order = remember(selected) { selected.withIndex().associate { (index, media) -> media.uri to index } }
    val bottom = 104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    PrefetchAhead(gridState, 24) { index ->
        if (index < pages.itemCount) pages.peek(index)?.let { MediaThumbLoader.load(context, it) }
    }
    LaunchedEffect(pages) {
        FolderMediaIndex.changes.collect {
            if (gridState.firstVisibleItemIndex == 0 && !gridState.isScrollInProgress) pages.refresh()
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(PickerGrid.MinCell),
        modifier = modifier,
        contentPadding = PickerGrid.padding(bottom, LocalPickerTopInset.current),
        horizontalArrangement = Arrangement.spacedBy(PickerGrid.Gap),
        verticalArrangement = Arrangement.spacedBy(PickerGrid.Gap)
    ) {
        items(pages.itemCount, key = { index -> pages.peek(index)?.uri ?: "placeholder-$index" }, contentType = { "media" }) { index ->
            pages[index]?.let { item ->
                MediaGridItem(item, order[item.uri], selected.isNotEmpty()) { vm.toggle(item) }
            }
        }
    }
}
