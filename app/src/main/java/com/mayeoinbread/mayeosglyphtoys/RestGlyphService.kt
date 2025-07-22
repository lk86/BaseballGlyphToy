package com.mayeoinbread.mayeosglyphtoys

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import org.json.JSONObject
import java.net.URL

class RestGlyphService : GlyphMatrixService("Rest-Glyph") {

    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())
    private var roomData: List<Pair<String, String>> = emptyList()
    private var scrollIndex = 0
    private var frameCounter = 0
    private val scrollSpeed = 3
    private var currentRoomIndex = 0
    private var roomName = "LOADING..."
    private var temperature = "--°C"
    private val scrollDelay = 200L  // milliseconds
    private val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

    private var apiUrl = ""

    private val prefsName = "rest_glyph_prefs"
    private val keyRoomIndex = "currentRoomIndex"

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        appContext = context
        loadUserPreferences(context)

        fetchTemperature()
        startScrolling()
    }

    private fun loadUserPreferences(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        apiUrl = prefs.getString("api_url", "") ?: ""
        currentRoomIndex = prefs.getInt(keyRoomIndex, 0)
    }

    override fun onTouchPointLongPress() {
        if (roomData.isEmpty()) return
        currentRoomIndex = (currentRoomIndex + 1) % roomData.size

        appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)?.edit()?.apply {
            putInt(keyRoomIndex, currentRoomIndex)
            apply()
        }

        updateRoomAndDraw()
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
            matrix,
            roomName,
            scrollIndex,
            5,
            4,
            512
        )
        val totalWidth = temperature.length * (DrawUtils.CHAR_WIDTH + DrawUtils.SPACING) - DrawUtils.SPACING
        val baseX = (DrawUtils.SCREEN_LENGTH - totalWidth) / 2
        DrawUtils.drawNormalText(
            matrix,
            temperature,
            baseX,
            DrawUtils.SCREEN_LENGTH - DrawUtils.CHAR_HEIGHT - 3,
            1024
        )
        glyphMatrixManager?.setMatrixFrame(matrix)

        frameCounter++
        if (frameCounter >= scrollSpeed) {
            scrollIndex = (scrollIndex + 1) % (roomName.length + 3)
            frameCounter = 0
        }
    }

    private fun fetchTemperature() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = URL(apiUrl).readText()
                val json = JSONObject(response)

                val list = mutableListOf<Pair<String, String>>()
                for (key in json.keys()) {
                    val arr = json.getJSONArray(key)
                    val value = arr.getDouble(0)
                    list.add(key to String.format("%.0f°C", value))
                }

                roomData = list
                updateRoomAndDraw()
            } catch (e: Exception) {
                e.printStackTrace()
                roomData = listOf("ERROR" to "--°C")
            }
        }
    }

    private fun updateRoomAndDraw() {
        val (name, value) = roomData.getOrElse(currentRoomIndex) {"ERROR" to "-°C"}
        roomName = name.uppercase()
        temperature = value
    }
}