package com.frameout.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.frameout.app.capture.CaptureShortcut
import com.frameout.core.backup.BackupFormat
import com.frameout.core.backup.BackupImporter
import kotlinx.coroutines.launch
import com.frameout.engine.work.ExtractNotifications
import com.frameout.engine.work.FgsBudget

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && Build.VERSION.SDK_INT >= 33 && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) openNotificationSettings()
    }
    private var incoming by mutableStateOf<List<Uri>>(emptyList())
    private var captureRequested by mutableStateOf(false)
    private var pendingBackup by mutableStateOf<Uri?>(null)
    private var notifRationale by mutableStateOf(false)
    private var internalLaunch = false

    override fun onStart() {
        super.onStart()
        internalLaunch = false
        FgsBudget.reset(this)
    }

    override fun onStop() {
        super.onStop()
        if (userLeftApp()) com.frameout.app.icon.AppIconSwitcher.sync(this, (application as FrameOutApp).container.prefs)
    }

    private fun userLeftApp(): Boolean {
        val screenOn = (getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isInteractive
        return screenOn && !internalLaunch && !isChangingConfigurations
    }

    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        internalLaunch = true
        super.startActivityForResult(intent, requestCode, options)
    }

    @Suppress("DEPRECATION")
    override fun startIntentSenderForResult(
        intent: android.content.IntentSender, requestCode: Int, fillInIntent: Intent?,
        flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        internalLaunch = true
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as FrameOutApp
        applyStartupTheme(app)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) com.frameout.app.icon.AppIconSwitcher.healDuplicates(this, app.container.prefs)
        if (Build.VERSION.SDK_INT >= 26) window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        enableEdgeToEdge()
        notifRationale = shouldAskNotifications(app)
        com.frameout.core.util.LocaleHelper.apply(this, app.container.prefs.language.value)
        if (savedInstanceState == null && !launchedFromHistory(intent)) readIntent(intent)
        setContent {
            MainContent(this, app, incoming, captureRequested, { captureRequested = false }, pendingBackup, { uri -> pendingBackup = null; importBackup(uri) }, { pendingBackup = null }, notifRationale, { notifRationale = false; requestNotifications(app) }, { notifRationale = false; app.container.prefs.setNotificationAsked() }) { incoming = emptyList() }
            PreferredFrameTheme { com.frameout.app.billing.PaywallHost(this@MainActivity, app) }
        }
    }

    private fun applyStartupTheme(app: FrameOutApp) {
        val systemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val dark = when (app.container.prefs.themeMode.value) {
            "Light" -> false
            "System" -> systemDark
            else -> true
        }
        setTheme(if (dark) R.style.Theme_FrameOut_Dark else R.style.Theme_FrameOut_Light)
    }

    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = VolumeKeys.dispatch(event) || super.dispatchKeyEvent(event)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun launchedFromHistory(intent: Intent): Boolean =
        intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0

    private fun readIntent(intent: Intent) {
        val (backups, media) = IncomingMedia.urisFrom(intent).partition { intent.type == BackupFormat.MIME || IncomingMedia.isBackup(this, it) }
        incoming = media
        val open = intent.getLongExtra(ExtractNotifications.EXTRA_OPEN_PROJECT, -1L)
        val trusted = com.frameout.core.util.IntentToken.matches(this, intent.getStringExtra(ExtractNotifications.EXTRA_OPEN_TOKEN))
        if (open >= 0L && trusted) PendingOpen.projectId = open
        intent.removeExtra(ExtractNotifications.EXTRA_OPEN_PROJECT)
        intent.removeExtra(ExtractNotifications.EXTRA_OPEN_TOKEN)
        if (backups.isNotEmpty()) pendingBackup = backups.first()
        if (backups.isNotEmpty() || media.isNotEmpty()) consume(intent)
        if (intent.action == CaptureShortcut.ACTION) {
            captureRequested = true
            intent.action = null
        }
    }

    private fun consume(intent: Intent) {
        intent.action = null
        intent.data = null
        intent.replaceExtras(null as Bundle?)
        intent.clipData = null
    }

    private fun importBackup(uri: Uri) {
        val app = application as FrameOutApp
        app.appScope.launch { app.container.notice.value = BackupImporter(app, app.container).import(uri).message() }
    }

    private fun shouldAskNotifications(app: FrameOutApp): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
        return !app.container.prefs.notificationAsked()
    }

    private fun openNotificationSettings() {
        runCatching {
            startActivity(
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    private fun requestNotifications(app: FrameOutApp) {
        app.container.prefs.setNotificationAsked()
        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
