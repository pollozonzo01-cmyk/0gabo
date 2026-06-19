package com.timetvpro

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView

class PlayerActivity : Activity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: StyledPlayerView
    private lateinit var progressBar: ProgressBar

    private var streamUrl: String? = null
    private var referer: String? = null
    private var userAgent: String? = null

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_REFERER = "extra_referer"
        const val EXTRA_USER_AGENT = "extra_user_agent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla encendida + pantalla completa
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setImmersiveMode()

        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.player_progress_bar)

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 5000

        // Obtener datos del Intent
        streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        referer = intent.getStringExtra(EXTRA_REFERER)
        userAgent = intent.getStringExtra(EXTRA_USER_AGENT)

        if (streamUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Error: URL de stream no válida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializePlayer()
    }

    private fun initializePlayer() {
        if (player != null) return

        val url = streamUrl ?: return

        try {
            // 1. Configurar los headers HTTP (Referer y User-Agent) necesarios para saltar bloqueos
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            userAgent?.let {
                if (it.isNotEmpty()) {
                    httpDataSourceFactory.setUserAgent(it)
                }
            }

            // Agregar cabeceras por defecto
            val headers = mutableMapOf<String, String>()
            referer?.let {
                if (it.isNotEmpty()) {
                    headers["Referer"] = it
                    try {
                        val uri = Uri.parse(it)
                        headers["Origin"] = "${uri.scheme}://${uri.host}"
                    } catch (e: Exception) {}
                }
            }
            userAgent?.let {
                if (it.isNotEmpty()) {
                    headers["User-Agent"] = it
                }
            }

            // Inyectar cookies del WebView para burlar Cloudflare/sesión
            try {
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    headers["Cookie"] = cookies
                }
            } catch (e: Exception) {}

            if (headers.isNotEmpty()) {
                httpDataSourceFactory.setDefaultRequestProperties(headers)
            }

            val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

            // 2. Crear ExoPlayer
            player = ExoPlayer.Builder(this).build()
            playerView.player = player

            // 3. Forzar decodificación HLS para m3u8
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)

            player?.apply {
                setMediaSource(mediaSource)
                playWhenReady = true
                prepare()

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                progressBar.visibility = View.VISIBLE
                            }
                            Player.STATE_READY -> {
                                progressBar.visibility = View.GONE
                            }
                            Player.STATE_ENDED -> {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@PlayerActivity, "El stream ha terminado", Toast.LENGTH_SHORT).show()
                            }
                            Player.STATE_IDLE -> {
                                progressBar.visibility = View.GONE
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@PlayerActivity,
                            "Error de conexión. Reintentando...",
                            Toast.LENGTH_LONG
                        ).show()

                        // Reintentar automáticamente después de 3 segundos
                        playerView.postDelayed({
                            if (!isFinishing && player != null) {
                                player?.prepare()
                                player?.play()
                            }
                        }, 3000)
                    }
                })
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Fallo al inicializar ExoPlayer: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun releasePlayer() {
        player?.let {
            it.release()
            player = null
        }
        playerView.player = null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    player?.let {
                        if (it.isPlaying) {
                            it.pause()
                            Toast.makeText(this, "Pausa", Toast.LENGTH_SHORT).show()
                        } else {
                            it.play()
                            Toast.makeText(this, "Reproducir", Toast.LENGTH_SHORT).show()
                        }
                        playerView.showController()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    player?.let {
                        if (it.isPlaying) it.pause() else it.play()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    player?.play()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    player?.pause()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        setImmersiveMode()
        player?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
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
}
