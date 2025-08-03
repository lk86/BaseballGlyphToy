package com.lhk.sportsglyphtoy

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lhk.sportsglyphtoy.DataUtils.flattenJson
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import org.json.JSONObject
import java.net.URL

class SportsGlyphService : GlyphMatrixService("Sports-Glyph") {

    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())
    private var gameData : Map<String, String> = emptyMap()
    private var scrollIndex = 0

    private var gameInning = ""
    private var gameName = "LOADING"
    private var gameScore = "..."
    private val scrollDelay = 200L  // milliseconds
    private val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

    private var apiKey = ""
    private var apiTeam = ""
    private var apiSport = ""

    private val prefsName = "sports_glyph_prefs"

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
        val prefs = context.getSharedPreferences(prefsName, MODE_PRIVATE)
        apiKey = prefs.getString("api_key", "sb11te7geey36bayvx7540d") ?: "" // Default value here is being used when it shouldn't be
        apiTeam = prefs.getString("api_team", "New York Mets") ?: "" // Default value here is being used when it shouldn't be
        apiSport = prefs.getString("api_sport", "mlb") ?: "" // Default value here is being used when it shouldn't be
    }

    override fun onTouchPointPressed() {
        fetchData()
        startScrolling()
    }

    override fun onTouchPointLongPress() {
        fetchData()
        startScrolling()
    }

    override fun onAodUpdate() {
        fetchData()
        startScrolling()
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
        DrawUtils.drawNormalText(
            gameInning,
            DrawUtils.TextAlign.H_CENTER,
            DrawUtils.TextAlign.TOP,
            1024,
            1,
            matrix
        )

        if (gameName.length > 5) {
        DrawUtils.drawScrollingTextCharacterWise(
            gameName,
            scrollIndex,
            DrawUtils.TextAlign.LEFT,
            DrawUtils.TextAlign.V_CENTER,
            512,
            matrix = matrix
        )
        } else {
            DrawUtils.drawNormalText(
                gameName,
                DrawUtils.TextAlign.H_CENTER,
                DrawUtils.TextAlign.V_CENTER,
                512,
                2,
                matrix = matrix
            )
        }

        DrawUtils.drawNormalText(
            gameScore,
            DrawUtils.TextAlign.H_CENTER,
            DrawUtils.TextAlign.BOTTOM,
            1024,
            2,
            matrix
        )

        val frame = GlyphMatrixFrame.Builder()
            .addTop(matrix)

        glyphMatrixManager?.setMatrixFrame(frame.build(applicationContext).render())

        scrollIndex = (scrollIndex + 1) % (gameName.length + 2)
    }

    private fun convertOutsToGlyph(outs: String, first: Boolean, second: Boolean, third: Boolean): Char {
        if (outs == "3") {
            return '_'
        } else if (outs == "0") {
            if (!first && !second && !third) {
                return 'a'
            } else if (first && !second && !third) {
                return 'b'
            } else if (!first && second && !third) {
                return 'c'
            } else if (!first && !second && third) {
                return 'd'
            } else if (first && second && !third) {
                return 'e'
            } else if (!first && second && third) {
                return 'f'
            } else if (first && second && third) {
                return 'g'
            }
        } else if (outs == "1") {
            if (!first && !second && !third) {
                return 'h'
            } else if (first && !second && !third) {
                return 'i'
            } else if (!first && second && !third) {
                return 'j'
            } else if (!first && !second && third) {
                return 'k'
            } else if (first && second && !third) {
                return 'l'
            } else if (!first && second && third) {
                return 'm'
            } else if (first && second && third) {
                return 'n'
            }
        } else if (outs == "2") {
            if (!first && !second && !third) {
                return 'o'
            } else if (first && !second && !third) {
                return 'p'
            } else if (!first && second && !third) {
                return 'q'
            } else if (!first && !second && third) {
                return 'r'
            } else if (first && second && !third) {
                return 's'
            } else if (!first && second && third) {
                return 't'
            } else if (first && second && third) {
                return 'u'
            }
        }
        return 'E'
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now(ZoneId.of("HST"))
                val url =
                    "https://api.sportsblaze.com/$apiSport/v1/boxscores/daily/$today.json?key=$apiKey&team=$apiTeam"
                val json = URL(url).readText()
                val flat = flattenJson(JSONObject(json))

                val statusTest = flat.getOrDefault("games[0].status", "")
                if (statusTest.isEmpty()) {
                    gameData = mapOf("name" to "NO GAME", "inning" to "-", "score" to "0-0")
                    updateValues()
                } else {
                    val gameIndex = 0
                    val status = flat.getValue("games[$gameIndex].status")
                    val away = flat.getValue("games[$gameIndex].teams.away.name").filter { it.isUpperCase() }
                    val home = flat.getValue("games[$gameIndex].teams.home.name").filter { it.isUpperCase() }
                    val away2Digits = away.take(2)
                    val home2Digits = home.take(2)
                    val gameName:String = if (away2Digits == home2Digits) {
                        "$away@$home"
                    } else {
                        "$away2Digits@$home2Digits"
                    }
                    when (status) {
                        "Scheduled" -> {
                            val gameTime =
                                Instant.parse(flat.getValue("games[$gameIndex].date")).atZone(ZoneId.systemDefault())
                            val gameHour = gameTime.hour.toString()
                            val gameMinute = gameTime.minute.toString()
                            gameData = mapOf("name" to gameName, "inning" to "NS", "score" to "$gameHour:$gameMinute")
                        }
                        "Final" -> {
                            val awayRuns = flat.getValue("games[$gameIndex].scores.total.away.runs")
                            val homeRuns = flat.getValue("games[$gameIndex].scores.total.home.runs")
                            val inning = if (apiTeam.filter{it.isUpperCase()} == home) { // Favorite Team is Home
                                if (homeRuns > awayRuns) {
                                    "W"
                                } else {
                                    "L"
                                }
                            } else if (apiTeam.filter{it.isUpperCase()} == away) { // Favorite Team is Away
                                if (awayRuns > homeRuns) {
                                    "W"
                                } else {
                                    "L"
                                }
                            } else { // Can't figure out which team is favorite
                                "E"
                            }
                            gameData = mapOf("name" to gameName, "inning" to inning, "score" to "$awayRuns-$homeRuns")
                        }
                        "Delayed" -> {
                            val gameTime =
                                Instant.parse(flat.getValue("games[$gameIndex].date")).atZone(ZoneId.systemDefault())
                            val gameHour = gameTime.hour.toString()
                            val gameMinute = gameTime.minute.toString()
                            gameData = mapOf("name" to gameName, "inning" to "D", "score" to "$gameHour:$gameMinute")
                        }
                        "In Progress" -> {
                            val awayRuns = flat.getValue("games[$gameIndex].scores.total.away.runs")
                            val homeRuns = flat.getValue("games[$gameIndex].scores.total.home.runs")
                            val period = flat.getValue("games[$gameIndex].live.period")
                            val tOrB = flat.getValue("games[$gameIndex].live.inning").filter { it.isUpperCase() }
                            val vOrCaret = when (tOrB) {
                                "T" -> "^"
                                "B" -> "v"
                                "M" -> "@"
                                "E" -> "."
                                else -> "E"
                            }
                            val glyph = convertOutsToGlyph(flat.getValue("games[$gameIndex].live.count.outs"), false, false, false)
                            gameData =
                                mapOf("name" to gameName, "inning" to "${vOrCaret}${period}${glyph}", "score" to "$awayRuns-$homeRuns")
                        }
                        else -> {
                            gameData = mapOf("name" to gameName, "inning" to "E", "score" to "ERR")
                        }
                    }
                    updateValues()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                gameData = mapOf("name" to "EXCEPTION", "inning" to "-", "score" to "ERR")

            }
        }
    }

    private fun updateValues() {
        gameName = gameData.getOrDefault("name", "ERROR")
        gameInning = gameData.getOrDefault("inning", "ERROR")
        gameScore = gameData.getOrDefault("score", "ERROR")
    }
}