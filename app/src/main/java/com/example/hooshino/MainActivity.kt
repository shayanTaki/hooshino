package com.example.hooshino

import android.annotation.SuppressLint
import android.graphics.Bitmap
// import android.graphics.Color as AndroidGraphicsColor
// import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
// import androidx.activity.enableEdgeToEdge
// import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.hooshino.ui.theme.HooshinoTheme

// تعریف enum AppLoadingState در سطح بالای فایل
enum class AppLoadingState {
    LOADING_WEBVIEW,
    SHOWING_WEBVIEW
}

class MainActivity : ComponentActivity() {

    private val websiteUrl = "https://shayanm007.pythonanywhere.com/"

    private val appState = mutableStateOf(AppLoadingState.LOADING_WEBVIEW) // استفاده از enum تعریف شده
    private var webViewInstance: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        setContent {
            HooshinoTheme {
                val currentAppState = appState.value
                SystemUiController(window = window, showFullscreen = (currentAppState == AppLoadingState.SHOWING_WEBVIEW))

                MainContent(
                    appState = currentAppState,
                    websiteUrl = websiteUrl,
                    onWebViewCreated = { wv -> webViewInstance = wv },
                    onPageLoadFinished = {
                        appState.value = AppLoadingState.SHOWING_WEBVIEW
                    },
                    onPageLoadError = {
                        Toast.makeText(this, "خطا در بارگذاری صفحه", Toast.LENGTH_LONG).show()
                        appState.value = AppLoadingState.SHOWING_WEBVIEW
                    },
                    onBackPressedInWebView = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                            true
                        } else {
                            finish()
                            true
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun SystemUiController(window: Window, showFullscreen: Boolean) {
    val view = LocalView.current
    val windowInsetsController = remember(window, view) {
        WindowCompat.getInsetsController(window, view)
    }
    LaunchedEffect(showFullscreen, windowInsetsController) {
        if (showFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    appState: AppLoadingState, // استفاده از enum تعریف شده
    websiteUrl: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageLoadFinished: () -> Unit,
    onPageLoadError: () -> Unit,
    onBackPressedInWebView: () -> Boolean
) {
    var internalWebViewInstance: WebView? by remember { mutableStateOf(null) }
    var pageLoadProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = pageLoadProgress / 100f, label = "progressAnimation")
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (appState == AppLoadingState.SHOWING_WEBVIEW) Modifier else Modifier.padding(innerPadding))
        ) {
            if (appState == AppLoadingState.LOADING_WEBVIEW) {
                LoadingScreen(progress = animatedProgress)
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        internalWebViewInstance = this
                        onWebViewCreated(this)

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.userAgentString = System.getProperty("http.agent")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                pageLoadProgress = 0f
                                Log.d("WebViewM", "Page started loading: $url")
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("WebViewM", "Page finished loading: $url")
                                CookieManager.getInstance().flush()
                                onPageLoadFinished()
                            }
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                val eMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.description else "خطای نامشخص"
                                val reqUrl = request?.url
                                Log.e("WebViewM", "Error: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode else ""} ($eMsg) for URL: $reqUrl")
                                Toast.makeText(context, "خطا در بارگذاری: $eMsg", Toast.LENGTH_LONG).show()
                                onPageLoadError()
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url.toString()
                                Log.d("WebViewM", "Loading URL in WebView: $url")
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                pageLoadProgress = newProgress.toFloat()
                            }
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let { Log.d("WebViewJSConsole", "${it.message()} -- Line ${it.lineNumber()} of ${it.sourceId()}") }
                                return true
                            }
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    Log.d("WebViewM", "Permission request for: ${request?.resources?.joinToString()}");
                                    // request?.grant(request.resources);
                                }
                            }
                        }
                        val cookieManager = CookieManager.getInstance(); cookieManager.setAcceptCookie(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) cookieManager.setAcceptThirdPartyCookies(this, true)
                        cookieManager.flush()

                        if (this.url == null || this.url == "about:blank") {
                            loadUrl(websiteUrl)
                        }
                    }
                },
                modifier = if (appState == AppLoadingState.SHOWING_WEBVIEW) Modifier.fillMaxSize() else Modifier.size(0.dp)
            )

            if (appState == AppLoadingState.SHOWING_WEBVIEW) {
                BackHandler {
                    onBackPressedInWebView()
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(progress: Float) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(80.dp), strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
        Spacer(modifier = Modifier.height(24.dp))
        Text("در حال بارگذاری...", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${(progress * 100).toInt()}%", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    HooshinoTheme {
        LoadingScreen(progress = 0.75f)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultAppPreviewWithLoadingState() {
    HooshinoTheme {
        MainContent(
            appState = AppLoadingState.LOADING_WEBVIEW,
            websiteUrl = "https://example.com",
            onWebViewCreated = {},
            onPageLoadFinished = {},
            onPageLoadError = {},
            onBackPressedInWebView = { false }
        )
    }
}