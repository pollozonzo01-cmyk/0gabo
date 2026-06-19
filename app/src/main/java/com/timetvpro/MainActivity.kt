package com.timetvpro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import java.io.ByteArrayInputStream

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var container: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalSystemUiVisibility = 0

    // Referencias a los botones de favoritos (Solo los declarados en activity_main.xml)
    private lateinit var btnHome: Button
    private lateinit var btnAztecaUno: Button
    private lateinit var btnAzteca7: Button
    private lateinit var btnCanal5: Button
    private lateinit var btnLasEstrellas: Button
    private lateinit var btnTudn: Button
    private lateinit var btnEspn: Button
    private lateinit var btnEspn2: Button
    private lateinit var btnEspn3: Button
    private lateinit var btnFoxSports: Button
    private lateinit var btnFoxSports2: Button
    private lateinit var btnFoxPremium: Button
    private lateinit var btnWinSports: Button
    private lateinit var btnWinSports2: Button
    private lateinit var btnDsports: Button
    private lateinit var btnDsports2: Button
    private lateinit var btnDsportsPlus: Button
    private lateinit var btnDaznLaliga: Button
    private lateinit var btnMovistarCampeones: Button

    companion object {
        // Enlaces rápidos de favoritos de Gabo TV
        const val GABO_TV_HOME = "https://www.gabotv.com/"
        const val AZTECA_UNO_URL = "https://www.gabotv.com/?movies=azteca-uno"
        const val AZTECA_7_URL = "https://www.gabotv.com/?movies=azteca-7"
        const val CANAL_5_URL = "https://www.gabotv.com/?movies=canal-5"
        const val LAS_ESTRELLAS_URL = "https://www.gabotv.com/?movies=las-estrellas"
        const val TUDN_URL = "https://www.gabotv.com/?movies=tudn"
        const val ESPN_URL = "https://www.gabotv.com/?movies=espn"
        const val ESPN2_URL = "https://www.gabotv.com/?movies=espn-2"
        const val ESPN3_URL = "https://www.gabotv.com/?movies=espn-3"
        const val ESPN4_URL = "https://www.gabotv.com/?movies=espn-4"
        const val ESPN5_URL = "https://www.gabotv.com/?movies=espn-5"
        const val ESPN6_URL = "https://www.gabotv.com/?movies=espn-6"
        const val ESPN7_URL = "https://www.gabotv.com/?movies=espn-7"
        const val FOX_SPORTS_URL = "https://www.gabotv.com/?movies=fox-sport"
        const val FOX_SPORTS2_URL = "https://www.gabotv.com/?movies=fox-sport-2"
        const val FOX_PREMIUM_URL = "https://www.gabotv.com/?movies=fox-sport-premium"
        const val WIN_SPORTS_URL = "https://www.gabotv.com/?movies=win-sport"
        const val WIN_SPORTS2_URL = "https://www.gabotv.com/?movies=win-sport-2"
        const val DSPORTS_URL = "https://www.gabotv.com/?movies=directv-sport"
        const val DSPORTS2_URL = "https://www.gabotv.com/?movies=directv-sport-2"
        const val DSPORTS_PLUS_URL = "https://www.gabotv.com/?movies=directv-sport-plus"
        const val DAZN_LALIGA_URL = "https://www.gabotv.com/?movies=dazn-la-liga"
        const val MOVISTAR_CAMPEONES_URL = "https://www.gabotv.com/?movies=m-liga-de-campeones"

        // User-agent de Chrome desktop — mejor compatibilidad con players de streaming
        const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla siempre encendida + fullscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setImmersiveMode()
        setContentView(R.layout.activity_main)

        container    = findViewById(R.id.container)
        progressBar  = findViewById(R.id.progressBar)
        webView      = findViewById(R.id.webView)

        // Enlazar los botones de favoritos
        btnHome = findViewById(R.id.btn_home)
        btnAztecaUno = findViewById(R.id.btn_azteca_uno)
        btnAzteca7 = findViewById(R.id.btn_azteca_7)
        btnCanal5 = findViewById(R.id.btn_canal_5)
        btnLasEstrellas = findViewById(R.id.btn_las_estrellas)
        btnTudn = findViewById(R.id.btn_tudn)
        btnEspn = findViewById(R.id.btn_espn)
        btnEspn2 = findViewById(R.id.btn_espn2)
        btnEspn3 = findViewById(R.id.btn_espn3)
        btnFoxSports = findViewById(R.id.btn_fox_sports)
        btnFoxSports2 = findViewById(R.id.btn_fox_sports2)
        btnFoxPremium = findViewById(R.id.btn_fox_premium)
        btnWinSports = findViewById(R.id.btn_win_sports)
        btnWinSports2 = findViewById(R.id.btn_win_sports2)
        btnDsports = findViewById(R.id.btn_dsports)
        btnDsports2 = findViewById(R.id.btn_dsports2)
        btnDsportsPlus = findViewById(R.id.btn_dsports_plus)
        btnDaznLaliga = findViewById(R.id.btn_dazn_laliga)
        btnMovistarCampeones = findViewById(R.id.btn_movistar_campeones)

        setupWebView()
        setupFavorites()
        clearPrivateData()
        
        // Cargar por defecto la portada de Gabo TV
        btnHome.isSelected = true
        webView.loadUrl(GABO_TV_HOME)
    }

    private fun setupFavorites() {
        val buttons = listOf(
            btnHome, btnAztecaUno, btnAzteca7, btnCanal5, btnLasEstrellas, btnTudn,
            btnEspn, btnEspn2, btnEspn3, btnFoxSports, btnFoxSports2, btnFoxPremium,
            btnWinSports, btnWinSports2, btnDsports, btnDsports2, btnDsportsPlus,
            btnDaznLaliga, btnMovistarCampeones
        )

        fun selectFavorite(selectedButton: Button, url: String) {
            buttons.forEach { it.isSelected = false }
            selectedButton.isSelected = true
            webView.loadUrl(url)
            
            // CRUCIAL: Regresar el foco del D-Pad directamente al WebView
            // Esto permite que el usuario pueda empezar a desplazarse hacia abajo de inmediato
            // con las flechas del control sin quedar atrapado en la barra superior.
            webView.requestFocus()
        }

        btnHome.setOnClickListener { selectFavorite(btnHome, GABO_TV_HOME) }
        btnAztecaUno.setOnClickListener { selectFavorite(btnAztecaUno, AZTECA_UNO_URL) }
        btnAzteca7.setOnClickListener { selectFavorite(btnAzteca7, AZTECA_7_URL) }
        btnCanal5.setOnClickListener { selectFavorite(btnCanal5, CANAL_5_URL) }
        btnLasEstrellas.setOnClickListener { selectFavorite(btnLasEstrellas, LAS_ESTRELLAS_URL) }
        btnTudn.setOnClickListener { selectFavorite(btnTudn, TUDN_URL) }
        btnEspn.setOnClickListener { selectFavorite(btnEspn, ESPN_URL) }
        btnEspn2.setOnClickListener { selectFavorite(btnEspn2, ESPN2_URL) }
        btnEspn3.setOnClickListener { selectFavorite(btnEspn3, ESPN3_URL) }
        btnFoxSports.setOnClickListener { selectFavorite(btnFoxSports, FOX_SPORTS_URL) }
        btnFoxSports2.setOnClickListener { selectFavorite(btnFoxSports2, FOX_SPORTS2_URL) }
        btnFoxPremium.setOnClickListener { selectFavorite(btnFoxPremium, FOX_PREMIUM_URL) }
        btnWinSports.setOnClickListener { selectFavorite(btnWinSports, WIN_SPORTS_URL) }
        btnWinSports2.setOnClickListener { selectFavorite(btnWinSports2, WIN_SPORTS2_URL) }
        btnDsports.setOnClickListener { selectFavorite(btnDsports, DSPORTS_URL) }
        btnDsports2.setOnClickListener { selectFavorite(btnDsports2, DSPORTS2_URL) }
        btnDsportsPlus.setOnClickListener { selectFavorite(btnDsportsPlus, DSPORTS_PLUS_URL) }
        btnDaznLaliga.setOnClickListener { selectFavorite(btnDaznLaliga, DAZN_LALIGA_URL) }
        btnMovistarCampeones.setOnClickListener { selectFavorite(btnMovistarCampeones, MOVISTAR_CAMPEONES_URL) }
    }

    @SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
    private fun setupWebView() {
        val settings = webView.settings

        // ── JavaScript y medios ──────────────────────────────────────────────
        settings.javaScriptEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // ── Rendimiento / hardware ───────────────────────────────────────────
        @Suppress("DEPRECATION")
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // ── Layout y Enfoque de Smart TV ─────────────────────────────────────
        settings.useWideViewPort     = true
        settings.loadWithOverviewMode = true
        settings.domStorageEnabled   = true
        settings.databaseEnabled     = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls  = false
        settings.displayZoomControls  = false

        // ── User-Agent desktop Chrome ────────────────────────────────────────
        settings.userAgentString = USER_AGENT

        // ── Mixed content (HTTP dentro de HTTPS — necesario para streams) ────
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // ── Anti-popup: bloquear ventanas emergentes de páginas externas ─────
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        webView.scrollBarStyle       = View.SCROLLBARS_INSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true

        // Configuración de foco para el control remoto
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                webView.requestFocus()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        //  WebViewClient — Intercepta las URLs m3u8/mpd de Gabo TV
        // ════════════════════════════════════════════════════════════════════
        webView.webViewClient = object : WebViewClient() {

            private var lastLaunchTime = 0L

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                injectAdBlockCSS(view)
                optimizeForTV(view)
            }

            // CRUCIAL: Ignorar errores de certificados SSL viejos/caducados en la TV Box
            // Esto evita que la pantalla se quede en blanco cuando la TV Box tiene el almacén de certificados desactualizado.
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }

            /**
             * NÚCLEO DEL BLOQUEADOR — intercepta CADA petición de red.
             * Si detecta un stream .m3u8 o .mpd al pulsar un botón o cargar la página,
             * lo intercepta y lo abre de forma automática en tu reproductor nativo.
             */
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()
                
                // Interceptar .m3u8 y .mpd, omitiendo los segmentos parciales .ts o llaves .key
                if (url != null && (url.contains(".m3u8") || url.contains(".mpd")) && 
                    !url.contains("/ts") && !url.contains(".ts") && !url.contains(".key")) {
                    
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastLaunchTime > 3000) {
                        lastLaunchTime = currentTime
                        
                        val headers = request.requestHeaders ?: emptyMap()
                        // Priorizar el parent URL como Referer secundario si el original falla
                        val referer = headers["Referer"] ?: headers["referer"] ?: view?.url ?: GABO_TV_HOME
                        val userAgent = headers["User-Agent"] ?: headers["user-agent"] ?: USER_AGENT
                        
                        view?.post {
                            val context = view.context
                            val intent = Intent(context, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_STREAM_URL, url)
                                putExtra(PlayerActivity.EXTRA_REFERER, referer)
                                putExtra(PlayerActivity.EXTRA_USER_AGENT, userAgent)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                    
                    // Retornar una respuesta vacía para detener la carga de video dentro del WebView
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }

                // Intentar bloquear anuncios de redes externas y redirecciones
                AdBlocker.shouldBlock(url)?.let { return it }
                
                return super.shouldInterceptRequest(view, request)
            }

            // Fallback para Android < 5.0 (API 21)
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldInterceptRequest(
                view: WebView?,
                url: String?
            ): WebResourceResponse? {
                AdBlocker.shouldBlock(url)?.let { return it }
                return super.shouldInterceptRequest(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                // Bloquear redirecciones de anuncios
                AdBlocker.shouldBlock(url)?.let { return true }
                return false
            }

            // Fallback para Android < 5.0
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                AdBlocker.shouldBlock(url)?.let { return true }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) { onHideCustomView(); return }
                customView         = view
                customViewCallback = callback
                originalSystemUiVisibility = window.decorView.systemUiVisibility
                container.addView(
                    customView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                webView.visibility = View.GONE
                setImmersiveMode()
            }

            override fun onHideCustomView() {
                customView?.let { container.removeView(it) }
                customView = null
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                window.decorView.systemUiVisibility = originalSystemUiVisibility
                setImmersiveMode()
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean,
                isUserGesture: Boolean, resultMsg: android.os.Message?
            ): Boolean = false

            override fun getVideoLoadingProgressView(): View? = null
        }
    }

    private fun injectAdBlockCSS(view: WebView?) {
        val css = AdBlocker.COSMETIC_CSS
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
            .replace("\r", "")
        val js = """
            (function() {
                var existing = document.getElementById('_mspp_adblock');
                if (existing) return;
                var s = document.createElement('style');
                s.id = '_mspp_adblock';
                s.innerHTML = '$css';
                document.head && document.head.appendChild(s);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun optimizeForTV(view: WebView?) {
        val js = """
            (function() {
                // Ocultar elementos estorbosos de Gabo TV para dejar el player de video limpio a pantalla completa
                var s = document.createElement('style');
                s.innerHTML =
                    'header, footer, .sidebar, .widget-area, #comments, .comments-area, .movie-info, ' +
                    '.entry-info, .related-posts, .dt_related, .top-page, .copy, .fbox, .logo, .nav, ' +
                    '.main-header, #nav-dropdown { display: none !important; }' +
                    'video { width:100%!important; height:auto!important; max-height:100vh!important; }' +
                    'body { overflow-x:hidden!important; background: #000 !important; }' +
                    '.player { width:100vw!important; height:100vh!important; position:fixed!important; top:0!important; left:0!important; z-index:99999!important; }' +
                    'iframe[src*="player"],iframe[src*="embed"],iframe[src*="stream"]' +
                    '{ width:100%!important; min-height:450px!important; }';
                document.head && document.head.appendChild(s);
                
                var overlays = document.querySelectorAll(
                    '[class*="overlay"],[class*="modal"],[class*="interstitial"],[class*="popup"]'
                );
                overlays.forEach(function(el) {
                    var z = parseInt(window.getComputedStyle(el).zIndex);
                    if (z > 100) el.style.display = 'none';
                });
                
                // Desmuteo y reproducción automática robótica
                var attempts = 0;
                var interval = setInterval(function() {
                    attempts++;
                    if (attempts > 40) { clearInterval(interval); return; }
                    
                    var videos = document.querySelectorAll('video');
                    videos.forEach(function(v) {
                        v.muted = false;
                        v.volume = 1.0;
                        if (v.paused) {
                            v.play().catch(function(e){});
                        }
                    });

                    var playBtns = document.querySelectorAll('.play-btn, .doo_player_play, [class*="play"], .jw-icon-volume, .jw-icon-play');
                    playBtns.forEach(function(btn) {
                        if (btn.offsetWidth > 0 && btn.offsetHeight > 0) {
                            btn.click();
                        }
                    });
                }, 500);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun setImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setImmersiveMode()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                when {
                    customView != null -> {
                        webView.webChromeClient?.onHideCustomView(); true
                    }
                    webView.canGoBack() -> { webView.goBack(); true }
                    else -> false
                }
            }
            KeyEvent.KEYCODE_MENU -> { webView.reload(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
        setImmersiveMode()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
