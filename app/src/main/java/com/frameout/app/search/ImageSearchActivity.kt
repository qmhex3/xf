package com.frameout.app.search

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import com.frameout.app.PreferredFrameTheme
import com.frameout.designsystem.components.FrameSpinner
import com.frameout.core.search.ImageSearchEngines
import com.frameout.core.search.SearchEngine
import com.frameout.core.util.SearchStrings

class ImageSearchActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var spinner: ComposeView
    private lateinit var image: Uri
    private lateinit var engine: SearchEngine
    private var mime = "image/jpeg"
    private var uploadHost = ""
    private var autoMode = false
    private var finished = 0
    private lateinit var titleView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = Runnable { if (autoMode) fallBack() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        val found = ImageSearchEngines.byId(intent.getStringExtra(EXTRA_ENGINE).orEmpty())
        if (uri == null || found == null || uri.authority != "$packageName.fileprovider") { finish(); return }
        image = uri
        engine = found
        mime = contentResolver.getType(uri) ?: "image/jpeg"

        web = WebView(this)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            addView(topBar(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(progress, ViewGroup.LayoutParams.MATCH_PARENT, (3 * resources.displayMetrics.density).toInt())
            addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(buildHost(root))
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        configureWeb()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val parent = web.parent as? ViewGroup
                val top = parent?.getChildAt(parent.childCount - 1) as? WebView
                if (top != null && top !== web) closePopup(top)
                else if (web.canGoBack()) web.goBack() else finish()
            }
        })
        startAuto()
    }

    private fun buildHost(root: View): FrameLayout {
        spinner = ComposeView(this).apply {
            setContent { PreferredFrameTheme { FrameSpinner(size = 44.dp) } }
        }
        return FrameLayout(this).apply {
            addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(spinner, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
    }

    private fun topBar(): View {
        val dp = resources.displayMetrics.density
        fun button(text: String, onClick: () -> Unit) = Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { onClick() }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF121212.toInt())
            setPadding((8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
            addView(button("✕") { finish() })
            addView(TextView(context).apply {
                titleView = this
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                text = engine.name
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(button(SearchStrings.webRetry()) { startAuto() })
            addView(button(SearchStrings.webSite()) { startManual() })
        }
    }

    private fun showProgress(value: Int) {
        progress.progress = value
        progress.visibility = if (value in 1..99) View.VISIBLE else View.GONE
        if (value >= 100 && (popupOpen() || !autoMode)) setLoading(false)
    }

    private fun setLoading(on: Boolean) {
        if (::spinner.isInitialized) spinner.visibility = if (on) View.VISIBLE else View.GONE
    }

    private fun showTitle(title: String?) {
        if (!title.isNullOrBlank() && ::titleView.isInitialized) titleView.text = title
    }

    private fun popupOpen(): Boolean {
        val parent = web.parent as? ViewGroup ?: return false
        return (0 until parent.childCount).any { parent.getChildAt(it).let { c -> c is WebView && c !== web } }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWeb() {
        applyBrowserSettings(web)
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) = showProgress(newProgress)

            override fun onReceivedTitle(view: WebView, title: String?) = showTitle(title)

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                if (!isUserGesture || popupOpen()) return false
                val popup = newPopup()
                web.visibility = View.GONE
                (web.parent as? ViewGroup)?.addView(popup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                (resultMsg.obj as WebView.WebViewTransport).webView = popup
                resultMsg.sendToTarget()
                return true
            }

            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                callback.onReceiveValue(arrayOf(image))
                return true
            }
        }
        web.webViewClient = mainClient()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun newPopup(): WebView {
        val popup = WebView(this)
        with(popup.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            userAgentString = web.settings.userAgentString
        }
        popup.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse? = serveUpload(request)

            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean = request.url.scheme != "https"
        }
        popup.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, newProgress: Int) = showProgress(newProgress)

            override fun onReceivedTitle(v: WebView, title: String?) = showTitle(title)

            override fun onShowFileChooser(v: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                callback.onReceiveValue(arrayOf(image))
                return true
            }

            override fun onCloseWindow(window: WebView) = closePopup(window)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)
        return popup
    }

    private fun mainClient() = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = serveUpload(request)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = request.url.scheme != "https"

        override fun onPageFinished(view: WebView, url: String?) {
            if (!autoMode) {
                setLoading(false)
            } else if (++finished >= 2) {
                handler.removeCallbacks(watchdog)
                setLoading(false)
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (request.isForMainFrame && errorResponse.statusCode >= 400 && autoMode) fallBack()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
            if (request.isForMainFrame && autoMode) fallBack()
        }
    }

    private fun serveUpload(request: WebResourceRequest): WebResourceResponse? {
        if (request.url.host != uploadHost || request.url.path != IMAGE_PATH) return null
        val stream = runCatching { contentResolver.openInputStream(image) }.getOrNull() ?: return null
        return WebResourceResponse(mime, null, 200, "OK", mapOf("Cache-Control" to "no-store"), stream)
    }

    private fun closePopup(popup: WebView) {
        (popup.parent as? ViewGroup)?.removeView(popup)
        popup.destroy()
        if (!popupOpen()) web.visibility = View.VISIBLE
    }

    private fun startAuto() {
        val spec = UploadSpecs.of(engine)
        if (spec == null) { startManual(); return }
        autoMode = true
        finished = 0
        setLoading(true)
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, AUTO_TIMEOUT_MS)
        uploadHost = Uri.parse(spec.origin).host.orEmpty()
        web.loadDataWithBaseURL(spec.origin + "/", spec.html(), "text/html", "utf-8", null)
    }

    private fun startManual() {
        handler.removeCallbacks(watchdog)
        autoMode = false
        uploadHost = ""
        setLoading(true)
        web.loadUrl(engine.url)
    }

    private fun fallBack() {
        autoMode = false
        startManual()
        titleView.text = SearchStrings.webFallback()
    }

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        if (::web.isInitialized) {
            (web.parent as? ViewGroup)?.let { p ->
                (0 until p.childCount).map { p.getChildAt(it) }.filter { it is WebView && it !== web }.forEach { p.removeView(it); (it as WebView).destroy() }
            }
            web.stopLoading()
            web.clearCache(true)
            web.clearHistory()
            (web.parent as? ViewGroup)?.removeView(web)
            web.destroy()
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URI = "image_uri"
        const val EXTRA_ENGINE = "engine_id"
        internal const val IMAGE_PATH = "/__frameout_image"
        private const val AUTO_TIMEOUT_MS = 15_000L
    }
}
