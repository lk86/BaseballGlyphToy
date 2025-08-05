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

class BaseballGlyphService : GlyphMatrixService("Baseball-Glyph") {

    private lateinit var appContext: Context

    private val handler = Handler(Looper.getMainLooper())
    private var gameData: List<Map<String, String>> = emptyList()
    private var scrollIndex = 0
    private var keyIndex = "currentKeyIndex"
    private var currentKeyIndex = 0

    private var gameInning = ""
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
        apiTeam = prefs.getString("mlb_team_id", "") ?: "" // Default value here is being used when it shouldn't be
    }

    override fun onTouchPointPressed() {
        if (gameData.isEmpty()) return
        updateValues()
        scrollIndex = 0
        drawFrame()
    }

    override fun onTouchPointLongPress() {
        if (gameData.isEmpty()) return
        currentKeyIndex = (currentKeyIndex + 1) % gameData.size

        appContext.getSharedPreferences(prefsName, MODE_PRIVATE)?.edit()?.apply {
            putInt(keyIndex, currentKeyIndex)
            apply()
        }

        updateValues()
        scrollIndex = 0
        drawFrame()
    }

    override fun onAodUpdate() {
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
        DrawUtils.drawNormalText(
            gameInning,
            DrawUtils.TextAlign.H_CENTER,
            DrawUtils.TextAlign.TOP,
            1024,
            2,
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
                    "https://statsapi.mlb.com/api/v1/schedule/games/?sportId=1&date=$today&teamId=$apiTeam"
                val json = URL(url).readText()
                val flat = flattenJson(JSONObject(json))

                val gameCount = flat.getOrDefault("dates[0].totalGames", "0")
                if (gameCount == "0") {
                    gameData = emptyList()
                    gameData += (mapOf("name" to "NO GAME", "inning" to "-", "score" to "0-0"))
                } else {
                    gameData = emptyList()
                    for (gameIndex in 0..<gameCount.toInt()) {
                        val status = flat.getOrDefault("dates[0].games[$gameIndex].status.abstractGameState", "Final")
                        val statusCode = flat.getOrDefault("dates[0].games[$gameIndex].status.statusCode", "E")
                        val away =
                            flat.getOrDefault("dates[0].games[$gameIndex].teams.away.team.name", "NYY")
                                .filter { it.isUpperCase() }
                        val home =
                            flat.getOrDefault("dates[0].games[$gameIndex].teams.home.team.name", "NYM")
                                .filter { it.isUpperCase() }
                        val away2Digits = away.take(2)
                        val home2Digits = home.take(2)
                        val gameName: String = if (away2Digits == home2Digits) {
                            "$away@$home"
                        } else {
                            "$away2Digits@$home2Digits"
                        }
                        when (status) {
                            "Preview" -> {
                                val gameTime =
                                    Instant.parse(flat.getValue("dates[0].games[$gameIndex].gameDate"))
                                        .atZone(ZoneId.systemDefault())
                                val gameHour = gameTime.hour.toString()
                                val gameMinute = gameTime.minute.toString()
                                gameData += mapOf(
                                    "name" to gameName,
                                    "inning" to statusCode,
                                    "score" to "$gameHour:$gameMinute"
                                )
                            }

                            "Final" -> {
                                val awayScore = flat.getOrDefault("dates[0].games[$gameIndex].teams.away.score", "0")
                                val homeScore = flat.getOrDefault("dates[0].games[$gameIndex].teams.home.score", "10")
                                val homeTeamID =
                                    flat.getOrDefault("dates[0].games[$gameIndex].teams.home.team.id", "121")
                                val awayTeamID = flat.getOrDefault("dates[0].games[$gameIndex].teams.away.team.id", "0")
                                val inning =
                                    if (apiTeam == homeTeamID) { // Favorite Team is Home
                                        if (homeScore.toInt() > awayScore.toInt()) {
                                            "W"
                                        } else if (awayScore.toInt() > homeScore.toInt()) {
                                            "L"
                                        } else {
                                            "T"
                                        }
                                    } else if (apiTeam == awayTeamID) { // Favorite Team is Away
                                        if (awayScore.toInt() > homeScore.toInt()) {
                                            "W"
                                        } else if (homeScore.toInt() > awayScore.toInt()) {
                                            "L"
                                        } else {
                                            "T"
                                        }
                                    } else { // Can't figure out which team is favorite
                                        "E"
                                    }
                                gameData += mapOf(
                                    "name" to gameName,
                                    "inning" to inning,
                                    "score" to "$awayScore-$homeScore"
                                )
                            }

                            "Live" -> {
                                val awayRuns = flat.getOrDefault("dates[0].games[$gameIndex].teams.away.score", "-")
                                val homeRuns = flat.getOrDefault("dates[0].games[$gameIndex].teams.home.score", "-")
                                val gamePk = flat.getValue("dates[0].games[$gameIndex].gamePk")
                                val linescoreUrl =
                                    "https://statsapi.mlb.com/api/v1/game/$gamePk/linescore"
                                val linescoreJSON = URL(linescoreUrl).readText()
                                val linescoreFlat = flattenJson(JSONObject(linescoreJSON))

                                val period = linescoreFlat.getValue("currentInning")
                                val tOrB = linescoreFlat.getValue("inningState").filter { it.isUpperCase() }
                                val vOrCaret = when (tOrB) {
                                    "T" -> "^"
                                    "B" -> "v"
                                    "M" -> "@"
                                    "E" -> "."
                                    else -> "E"
                                }
                                val first = linescoreFlat.getOrDefault("offense.first.id", "")
                                val second = linescoreFlat.getOrDefault("offense.second.id", "")
                                val third = linescoreFlat.getOrDefault("offense.third.id", "")
                                val glyph = convertOutsToGlyph(
                                    linescoreFlat.getValue("outs"),
                                    !first.isEmpty(),
                                    !second.isEmpty(),
                                    !third.isEmpty()
                                )
                                gameData +=
                                    mapOf(
                                        "name" to gameName,
                                        "inning" to "${vOrCaret}${period}${glyph}",
                                        "score" to "$awayRuns-$homeRuns"
                                    )
                            }

                            else -> {
                                gameData += mapOf("name" to gameName, "inning" to statusCode, "score" to status)
                            }
                        }
                    }
                    updateValues()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // gameData = emptyList()
                // gameData += mapOf("name" to "EXCEPTION", "inning" to "-", "score" to "ERR")
                // updateValues()
            }
        }
    }

    private fun updateValues() {
        gameName = gameData[currentKeyIndex].getOrDefault("name", "ERROR")
        gameInning = gameData[currentKeyIndex].getOrDefault("inning", "ERROR")
        gameScore = gameData[currentKeyIndex].getOrDefault("score", "ERROR")
    }
}