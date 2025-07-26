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
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager

class NowPlayingGlyphService : GlyphMatrixService("NowPlaying-Glyph") {

    private val handler = Handler(Looper.getMainLooper())
    private var scrollIndex = 0
    private val scrollDelay = 200L  // milliseconds

    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentSource: String = ""

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaControllerCallback: MediaController.Callback? = null

    private val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(context, NowPlayingListenerService::class.java)

        val activeSessions = mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()

        for (controller in activeSessions) {
            mediaControllerCallback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    val source = controller.packageName.split(".")[1].first()
                    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""

                    currentArtist = artist
                    currentTitle = title
                    currentSource = source.uppercase()
                    startScrolling()
                }
            }
            controller.registerCallback(mediaControllerCallback!!)

            // Immediately update if metadata already exists
            controller.metadata?.let { meta ->
                currentArtist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                currentTitle = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                currentSource = controller.packageName.split(".")[1].first().uppercase()
            }
            // We need to call this so it still draws to the screen even if no metadata is present
            startScrolling()
            break  // Just bind to the first valid session
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacks(scrollRunnable)

        val componentName = ComponentName(this, NowPlayingListenerService::class.java)
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

    override fun onAodUpdate() {
        startScrolling()
    }

    private fun drawNowPlaying() {
        matrix.fill(0)
        // If there's no media playing, draw a notice on the screen
        if (currentTitle == "" && currentArtist == "") {
            DrawUtils.drawNormalText(
                "NO",
                DrawUtils.TextAlign.H_CENTER,
                DrawUtils.TextAlign.BOTTOM_C,
                1024,
                1,
                matrix
            )
            DrawUtils.drawNormalText(
                "MUSIC",
                DrawUtils.TextAlign.H_CENTER,
                DrawUtils.TextAlign.TOP_C,
                1024,
                1,
                matrix
            )
        } else {
            if (currentSource != "") {
                DrawUtils.drawNormalText(
                    currentSource,
                    DrawUtils.TextAlign.H_CENTER,
                    DrawUtils.TextAlign.TOP,
                    512,
                    2,
                    matrix)
            }
            val displayText = "$currentTitle - $currentArtist  |".toUpperCase(Locale.current)
            DrawUtils.drawScrollingTextCharacterWise(
                displayText,
                scrollIndex,
                DrawUtils.TextAlign.LEFT,
                DrawUtils.TextAlign.V_CENTER,
                1024,
                matrix = matrix
            )

            scrollIndex = (scrollIndex + 1) % (displayText.length + 2)
        }

        // User FrameBuilder so we can try adding Music App icon to background
        val frame = GlyphMatrixFrame.Builder()
            .addMid(matrix)
            .build(applicationContext)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }
}