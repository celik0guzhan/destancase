package com.destancase.feature.web

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * Çoklu dosya seçimi — ACTION_OPEN_DOCUMENT. Birden fazla MIME destekler.
 */
class PickMultipleWithMimeTypes : ActivityResultContract<Array<String>, List<Uri>>() {
    override fun createIntent(context: android.content.Context, input: Array<String>) =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = if (input.size == 1) input[0] else "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val out = mutableListOf<Uri>()
        intent.data?.let(out::add)
        intent.clipData?.let { cd -> repeat(cd.itemCount) { out += cd.getItemAt(it).uri } }
        return out
    }
}

/**
 * Minimal WebView ekranı: ikon/toolbar yok, sadece içerik + pull‑to‑refresh.
 * Viewport problemleri için kritik ayarlar:
 *  - useWideViewPort = true
 *  - loadWithOverviewMode = true
 */
@Composable
fun WebViewScreen(
    url: String,
    allowedHosts: Set<String>,
    maxCount: Int = 10,
    onCloseRequested: () -> Unit = {}
) {
    val context = LocalContext.current

    var pending by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isPickerOpen by remember { mutableStateOf(false) }

    val refreshState = rememberSwipeRefreshState(isRefreshing)

    // Dosya seçici
    val picker = rememberLauncherForActivityResult(PickMultipleWithMimeTypes()) { uris ->
        isPickerOpen = false
        pending?.onReceiveValue(
            if (uris.isNullOrEmpty()) null else uris.take(maxCount).toTypedArray()
        )
        pending = null
    }

    // İçerik
    SwipeRefresh(
        state = refreshState,
        onRefresh = {
            isRefreshing = true
            webView?.reload()
        },
        // Picker açıkken veya sayfa yüklenirken refresh devre dışı
        swipeEnabled = !isPickerOpen && !isLoading,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        webView = this

                        // ——— WebSettings: mobil viewport davranışı ———
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true

                            // *** Viewport düzeltmeleri ***
                            useWideViewPort = true          // <meta name="viewport">'u uygula
                            loadWithOverviewMode = true     // İçeriği başlangıçta ekrana sığdır
                            setSupportZoom(false)            // Manuel ölçek verme; meta ile çakışmasın
                            builtInZoomControls = false
                            displayZoomControls = false

                            // Ağ
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = "$userAgentString AppWebView"
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        // Scroll bar stili;
                        setBackgroundColor(0xFF000000.toInt())


                        webViewClient = object : WebViewClient() {

                            //  izinli hostlar WebView içinde; diğeri dışarı yönlendirilebilir (şimdilik blokla)
                            override fun shouldOverrideUrlLoading(
                                view: WebView?, request: WebResourceRequest?
                            ): Boolean {
                                val u = request?.url ?: return false
                                val ok = (u.scheme == "https") && (u.host in allowedHosts)
                                return !ok
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                isRefreshing = false
                            }

                            // SSL hataları: prod'da blokla (güvenlik). Gerekirse telemetry eklenebilir.
                            override fun onReceivedSslError(
                                view: WebView?, handler: SslErrorHandler?, error: SslError?
                            ) {
                                handler?.cancel()
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                isPickerOpen = true
                                pending?.onReceiveValue(null) // Eski callback'ı iptal
                                pending = filePathCallback

                                //Kabul edilen MIME türleri
                                val accepts = fileChooserParams?.acceptTypes
                                    ?.map { it.trim() }
                                    ?.filter { it.isNotEmpty() }
                                    ?.toTypedArray()
                                    ?: arrayOf("*/*")

                                picker.launch(accepts)
                                return true
                            }
                        }
                        //Sayfayı yükle
                        loadUrl(url)
                    }
                }
            )

            // Üstte yüklenme çubuğu
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
        }
    }

    // Donanım geri
    BackHandler(enabled = true) {
        val wv = webView
        if (wv?.canGoBack() == true) wv.goBack() else onCloseRequested()
    }

    // Compose yaşam döngüsü  WebView temizliği
    DisposableEffect(Unit) { onDispose { webView?.destroy() } }
}
