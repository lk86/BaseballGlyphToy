package com.mayeoinbread.mayeosglyphtoys

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RestGlyphService : GlyphMatrixService("Rest-Glyph") {

    companion object {
        private const val GLYPH_LENGTH = 25

        private val font3x5 = mapOf(
            '0' to listOf("111", "101", "101", "101", "111"),
            '1' to listOf("010", "110", "010", "010", "111"),
            '2' to listOf("111", "001", "111", "100", "111"),
            '3' to listOf("111", "001", "111", "001", "111"),
            '4' to listOf("101", "101", "111", "001", "001"),
            '5' to listOf("111", "100", "111", "001", "111"),
            '6' to listOf("111", "100", "111", "101", "111"),
            '7' to listOf("111", "001", "010", "010", "010"),
            '8' to listOf("111", "101", "111", "101", "111"),
            '9' to listOf("111", "101", "111", "001", "111"),
            '-' to listOf("000", "000", "111", "000", "000"),
            ':' to listOf("0", "1", "0", "1", "0"),
            ' ' to listOf("000", "000", "000", "000", "000"),
            'L' to listOf("100", "100", "100", "100", "111"),
            'B' to listOf("110", "101", "110", "101", "110"),
            'E' to listOf("111", "100", "111", "100", "111"),
            'r' to listOf("000", "000", "110", "100", "100"),
            'o' to listOf("000", "000", "111", "101", "111"),
            'S' to listOf("011", "100", "010", "001", "110"),
            'K' to listOf("101", "101", "110", "101", "101")
        )
    }
}