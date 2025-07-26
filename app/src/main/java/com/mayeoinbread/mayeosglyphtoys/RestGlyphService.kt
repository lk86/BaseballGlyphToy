package com.mayeoinbread.mayeosglyphtoys

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mayeoinbread.mayeosglyphtoys.DataUtils.flattenJson
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import org.json.JSONObject
import java.net.URL

class RestGlyphService : GlyphMatrixService("Rest-Glyph") {

    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())
    private var kvData: List<Pair<String, String>> = emptyList()
    private var scrollIndex = 0
    private var currentKeyIndex = 0
    private var keyName = "LOADING..."
    private var keyValue = "---"
    private val scrollDelay = 200L  // milliseconds
    private val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

    private var apiUrl = ""
    private var selectedKeys = emptyList<String>()

    private val prefsName = "rest_glyph_prefs"
    private val keyIndex = "currentKeyIndex"

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        appContext = context
        loadUserPreferences(context)

        fetchData()
        startScrolling()
    }

    private fun loadUserPreferences(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        apiUrl = prefs.getString("api_url", "") ?: ""
        selectedKeys = prefs.getStringSet("selected_fields", emptySet())?.toList() ?: return
        currentKeyIndex = prefs.getInt(keyIndex, 0)
    }

    override fun onTouchPointLongPress() {
        if (kvData.isEmpty()) return
        currentKeyIndex = (currentKeyIndex + 1) % kvData.size

        appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)?.edit()?.apply {
            putInt(keyIndex, currentKeyIndex)
            apply()
        }

        updateValues()
        scrollIndex = 0
        drawFrame()
    }

    private fun startScrolling() {
        handler.post(object : Runnable {
            override fun run() {
                drawFrame()
                handler.postDelayed(this, scrollDelay)
            }
        })
    }

    private fun drawFrame() {
        matrix.fill(0)
        DrawUtils.drawScrollingTextCharacterWise(
            keyName,
            scrollIndex,
            DrawUtils.TextAlign.LEFT,
            DrawUtils.TextAlign.V_CENTER,
            512,
            matrix = matrix
        )

        DrawUtils.drawNormalText(
            keyValue,
            DrawUtils.TextAlign.H_CENTER,
            DrawUtils.TextAlign.BOTTOM,
            1024,
            2,
            matrix
        )

        val frame = GlyphMatrixFrame.Builder()
            .addTop(matrix)

        glyphMatrixManager?.setMatrixFrame(frame.build(applicationContext).render())

        scrollIndex = (scrollIndex + 1) % (keyName.length + 2)
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(apiUrl).readText()
                val flat = flattenJson(JSONObject(json))
                val valuesToShow = selectedKeys.mapNotNull { key -> flat[key]?.let { key to it } }

                kvData = valuesToShow
                updateValues()
            } catch (e: Exception) {
                e.printStackTrace()
                kvData = listOf("ERROR" to "---")
            }
        }
    }

    private fun updateValues() {
        val (name, value) = kvData.getOrElse(currentKeyIndex) { "ERROR" to "---" }
        keyName = "${name.uppercase()}  |"
        keyValue = value
    }
}