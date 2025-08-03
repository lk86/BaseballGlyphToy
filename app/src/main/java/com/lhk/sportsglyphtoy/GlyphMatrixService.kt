package com.lhk.sportsglyphtoy

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

abstract class GlyphMatrixService(private val tag: String) : Service() {

    private val buttonPressedHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.let { data ->
                        data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)?.let { value ->
                            when (value) {
                                GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
                                GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
                                GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
                                GlyphToy.EVENT_AOD -> onAodUpdate()
                            }
                        }
                    }
                }
                else -> {
                    Log.d(LOG_TAG, "Message: ${msg.what}")
                    super.handleMessage(msg)
                }
            }
        }
    }

    private val serviceMessenger = Messenger(buttonPressedHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(p0: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                Log.d(LOG_TAG, "$tag: onServiceConnected")
                gmm.register(Glyph.DEVICE_23112)
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {}
    }

    final override fun startService(service: Intent?): ComponentName? {
        Log.d(LOG_TAG, "$tag: startService")
        return super.startService(service)
    }

    final override fun onBind(intent: Intent?): IBinder? {
        Log.d(LOG_TAG, "$tag: onBind")
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
            Log.d(LOG_TAG, "$tag: onBind completed")
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LOG_TAG, "$tag: onUnbind")
        glyphMatrixManager?.let {
            Log.d(LOG_TAG, "$tag: onServiceDisconnected")
            performOnServiceDisconnected(applicationContext)
        }
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }

    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}

    open fun performOnServiceDisconnected(context: Context) {}

    open fun onTouchPointPressed() {}

    open fun onTouchPointLongPress() {}

    open fun onTouchPointReleased() {}

    open fun onAodUpdate() {
        Log.d(LOG_TAG, "$tag: onAodUpdate")
    }

    companion object {
        val LOG_TAG: String = GlyphMatrixService::class.java.simpleName
    }
}