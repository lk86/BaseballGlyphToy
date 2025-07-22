package com.mayeoinbread.mayeosglyphtoys

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.nothing.ketchum.GlyphMatrixManager

class NowPlayingGlyphService : GlyphMatrixService("NowPlaying-Glyph") {

    private val handler = Handler(Looper.getMainLooper())
    private var scrollIndex = 0
    private var frameCounter = 0
    private val scrollSpeed = 2
    private val scrollDelay = 100L  // milliseconds

    private var currentTitle: String = ""
    private var currentArtist: String = ""

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaControllerCallback: MediaController.Callback? = null

    private val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, NowPlayingGlyphService::class.java)

        val activeSessions = mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()

        for (controller in activeSessions) {
            mediaControllerCallback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    metadata ?: return
                    val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: return
                    val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return

                    currentArtist = artist
                    currentTitle = title
                    startScrolling()
                }
            }
            controller.registerCallback(mediaControllerCallback!!)
            // Immediately update if metadata already exists
            controller.metadata?.let { meta ->
                currentArtist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                currentTitle = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                startScrolling()
            }
            break  // Just bind to the first valid session
        }
    }

    override fun onAodUpdate() {
        super.onAodUpdate()
        DrawUtils.drawNormalText(matrix, "AOD_ACTIVE", 0, DrawUtils.SCREEN_CENTER.toInt(), 1024)
    }

    override fun onDestroy() {
        super.onDestroy()
        val componentName = ComponentName(this, NowPlayingGlyphService::class.java)
        val sessions = mediaSessionManager?.getActiveSessions(componentName) ?: return

        // Unregister the callback
        for (controller in sessions) {
            mediaControllerCallback?.let { controller.unregisterCallback(it) }
        }
        mediaControllerCallback = null
    }

    private val scrollRunnable = object : Runnable {
        override fun run() {
            drawNowPlaying()
            handler.postDelayed(this, scrollDelay)
        }
    }

    private fun startScrolling() {
        handler.removeCallbacks(scrollRunnable)
        scrollIndex = 0
        handler.post(scrollRunnable)
    }

    private fun drawNowPlaying() {
        matrix.fill(0)
        val displayText = "$currentTitle - $currentArtist  |".toUpperCase(Locale.current)
        DrawUtils.drawScrollingTextCharacterWise(matrix, displayText, scrollIndex, 0, DrawUtils.SCREEN_CENTER.toInt() - 1, 1024)
        glyphMatrixManager?.setMatrixFrame(matrix)

        frameCounter++
        if(frameCounter >= scrollSpeed) {
            scrollIndex = (scrollIndex + 1) % (displayText.length + 2)
            frameCounter = 0
        }
    }
}