package com.lhk.sportsglyphtoys

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lhk.sportsglyphtoys.DataUtils.flattenJson
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import org.json.JSONObject
import java.net.URL

class FootballGlyphService : GlyphMatrixService("Football-Glyph") {

    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())
    private var gameData: Map<String, String> = emptyMap()
    private var scrollIndex = 0

    private var gamePeriod = ""
    private var gameName = "LOADING"
    private var gameScore = "..."
    private val scrollDelay = 200L  // milliseconds
    private val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

    private var apiTeam = ""

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
        apiTeam = prefs.getString("nfl_team", "137") ?: "" // Default value here is being used when it shouldn't be
    }

    override fun onTouchPointPressed() {
        fetchData()
        drawFrame()
    }

    override fun onTouchPointLongPress() {
        fetchData()
        drawFrame()
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
            gamePeriod,
            DrawUtils.TextAlign.H_CENTER,
            DrawUtils.TextAlign.TOP,
            1024,
            1,
            matrix
        )

        if (gameName.length > 6) {
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
                1,
                matrix = matrix
            )
        }

        DrawUtils.drawNormalText(
            gameScore,
            DrawUtils.TextAlign.H_CENTER,
            DrawUtils.TextAlign.BOTTOM,
            1024,
            1,
            matrix
        )

        val frame = GlyphMatrixFrame.Builder()
            .addTop(matrix)

        glyphMatrixManager?.setMatrixFrame(frame.build(applicationContext).render())

        scrollIndex = (scrollIndex + 1) % (gameName.length + 2)
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now(ZoneId.of("HST"))
                val url =
                    "https://statsapi.mlb.com/api/v1/schedule/games/?sportId=1&date=$today&teamId=$apiTeam"
                val json = URL(url).readText()
                val flat = flattenJson(JSONObject(json))

                val gameCount = flat.getValue("dates[0].totalGames")
                if (gameCount == "0") {
                    gameData = mapOf("name" to "NO GAME", "period" to "-", "score" to "0-0")
                } else if (gameCount == "1") {
                    val gameIndex = 0
                    val status = flat.getValue("dates[0].games[$gameIndex].status.detailedState")
                    val statusCode = flat.getOrDefault("dates[0].games[0].status.statusCode", "E")
                    val away =
                        flat.getValue("dates[0].games[$gameIndex].teams.away.team.name").filter { it.isUpperCase() }
                    val home =
                        flat.getValue("dates[0].games[$gameIndex].teams.home.team.name").filter { it.isUpperCase() }
                    val away2Digits = away.take(2)
                    val home2Digits = home.take(2)
                    val gameName: String = if (away2Digits == home2Digits) {
                        "$away@$home"
                    } else {
                        "$away2Digits@$home2Digits"
                    }
                    when (status) {
                        "Scheduled" -> {
                            val gameTime =
                                Instant.parse(flat.getValue("dates[0].games[$gameIndex].gameDate"))
                                    .atZone(ZoneId.systemDefault())
                            val gameHour = gameTime.hour.toString()
                            val gameMinute = gameTime.minute.toString()
                            gameData =
                                mapOf("name" to gameName, "period" to statusCode, "score" to "$gameHour:$gameMinute")
                        }

                        "Final" -> {
                            val awayScore = flat.getValue("dates[0].games[$gameIndex].teams.away.score")
                            val homeScore = flat.getValue("dates[0].games[$gameIndex].teams.home.score")
                            val homeTeamID = flat.getValue("dates[0].games[$gameIndex].teams.home.team.id")
                            val awayTeamID = flat.getValue("dates[0].games[$gameIndex].teams.away.team.id")
                            val period =
                                if (apiTeam == homeTeamID) { // Favorite Team is Home
                                    if (homeScore > awayScore) {
                                        "W"
                                    } else if (awayScore > homeScore) {
                                        "L"
                                    } else {
                                        "T"
                                    }
                                } else if (apiTeam == awayTeamID) { // Favorite Team is Away
                                    if (awayScore > homeScore) {
                                        "W"
                                    } else if (homeScore > awayScore) {
                                        "L"
                                    } else {
                                        "T"
                                    }
                                } else { // Can't figure out which team is favorite
                                    "E"
                                }
                            gameData = mapOf("name" to gameName, "period" to period, "score" to "$awayScore-$homeScore")
                        }

                        "Delayed" -> {
                            val timeText =
                                if (flat.getValue("dates[0].games[$gameIndex].status.startTimeTBD") == "true") {
                                    "TBD"
                                } else {
                                    val gameTime =
                                        Instant.parse(flat.getValue("dates[0].games[$gameIndex].gameDate"))
                                            .atZone(ZoneId.systemDefault())
                                    val gameHour = gameTime.hour.toString()
                                    val gameMinute = gameTime.minute.toString()
                                    "$gameHour:$gameMinute"
                                }
                            gameData = mapOf("name" to gameName, "period" to statusCode, "score" to timeText)
                        }

                        "In Progress" -> {
                            val awayScore = flat.getValue("dates[0].games[$gameIndex].teams.away.score")
                            val homeScore = flat.getValue("dates[0].games[$gameIndex].teams.home.score")
                            val gamePk = flat.getValue("dates[0].games[$gameIndex].gamePk")
                            val linescoreUrl =
                                "https://statsapi.mlb.com/api/v1/game/$gamePk/linescore"
                            val linescoreJSON = URL(linescoreUrl).readText()
                            val linescoreFlat = flattenJson(JSONObject(linescoreJSON))

                            val period = linescoreFlat.getValue("currentInning")

                            gameData =
                                mapOf(
                                    "name" to gameName,
                                    "period" to period,
                                    "score" to "$awayScore-$homeScore"
                                )
                        }

                        else -> {
                            gameData = mapOf("name" to gameName, "period" to statusCode, "score" to "ERR")
                        }
                    }
                    updateValues()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                gameData = mapOf("name" to "EXCEPTION", "period" to "-", "score" to "ERR")
                updateValues()
            }
        }
    }

    private fun updateValues() {
        gameName = gameData.getOrDefault("name", "ERROR")
        gamePeriod = gameData.getOrDefault("period", "ERROR")
        gameScore = gameData.getOrDefault("score", "ERROR")
    }
}