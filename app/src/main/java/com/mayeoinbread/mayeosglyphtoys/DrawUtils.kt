package com.mayeoinbread.mayeosglyphtoys

import kotlin.math.abs

object DrawUtils {

    const val SCREEN_LENGTH = 25
    const val SCREEN_CENTER = 12.5f
    const val CHAR_WIDTH = 3
    const val CHAR_HEIGHT = 5
    const val SPACING = 1

    fun drawLineOnMatrix(matrix: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int) {
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var x = x0
        var y = y0

        while (true) {
            if (x in 0..<SCREEN_LENGTH && y in 0..<SCREEN_LENGTH) {
                matrix[y * SCREEN_LENGTH + x] = value
            }
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
    }

    fun rotate90(font: List<String>): List<String> {
        val rotated = MutableList(CHAR_WIDTH) {""}
        for (x in 0 until CHAR_WIDTH) {
            for (y in 0 until CHAR_HEIGHT) {
                rotated[x] = rotated[x] + font[y][x]
            }
        }
        return rotated.reversed()
    }

    fun drawNormalText(
        matrix: IntArray,
        text: String,
        baseX: Int,
        baseY: Int,
        brightness: Int
    ) {
        for ((i, c) in text.withIndex()) {
            val char = font[c] ?: continue
            for (y in char.indices) {
                for (x in char[y].indices) {
                    if (char[y][x] == '1') {
                        val mx = baseX + i * (CHAR_WIDTH + SPACING) + x
                        val my = baseY + y
                        if (mx in 0 until SCREEN_LENGTH && my in 0 until SCREEN_LENGTH) {
                            matrix[my * SCREEN_LENGTH + mx] = brightness
                        }
                    }
                }
            }
        }
    }

    fun drawRotatedText(
        matrix: IntArray,
        text: String,
        baseX: Int,
        baseY: Int,
        brightness: Int
    ) {
        for ((i, c) in text.reversed().withIndex()) {
            val fontChar = font[c] ?: continue
            val rotated = rotate90(fontChar)
            for (y in rotated.indices) {
                for (x in rotated[y].indices) {
                    if (rotated[y][x] == '1') {
                        val mx = baseX + x
                        val my = baseY + i * (CHAR_WIDTH + SPACING) + y
                        if (mx in 0 until SCREEN_LENGTH && my in 0 until SCREEN_LENGTH) {
                            matrix[my * SCREEN_LENGTH + mx] = brightness
                        }
                    }
                }
            }
        }
    }

    fun drawScrollingTextCharacterWise(
        matrix: IntArray,
        text: String,
        scrollIndex: Int,
        baseX: Int,
        baseY: Int,
        brightness: Int,
        rotate: Boolean = false
    ) {
        val fullText = "$text  $text  "
        val displayableChars = SCREEN_LENGTH / (3 + SPACING)

        val visibleText = fullText.drop(scrollIndex).take(displayableChars)

        for ((i, c) in visibleText.withIndex()) {
            val glyph = font[c] ?: font[' ']!!
            val rendered = if (rotate) rotate90(glyph) else glyph

            for (y in rendered.indices) {
                for (x in rendered[y].indices) {
                    if (rendered[y][x] == '1') {
                        val mx = baseX + i * (CHAR_WIDTH + SPACING) + x
                        val my = baseY + y
                        if (mx in 0 until SCREEN_LENGTH && my in 0 until SCREEN_LENGTH) {
                            matrix[my * SCREEN_LENGTH + mx] = brightness
                        }
                    }
                }
            }
        }
    }

    val font: Map<Char, List<String>> = mapOf(
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
        '°' to listOf("010", "101", "010", "000", "000"),
        ':' to listOf("0", "1", "0", "1", "0"),
        ' ' to listOf("000", "000", "000", "000", "000"),
        '.' to listOf("000", "000", "000", "000", "010"),
        '|' to listOf("010", "010", "010", "010", "010"),
        '\'' to listOf("010", "010", "000", "000", "000"),
        '&' to listOf("010", "101", "010", "101", "011"),
        '(' to listOf("010", "100", "100", "100", "010"),
        ')' to listOf("010", "001", "001", "001", "010"),
        '#' to listOf("101", "111", "101", "111", "101"),
        'A' to listOf("010", "101", "111", "101", "101"),
        'B' to listOf("110", "101", "110", "101", "110"),
        'C' to listOf("010", "101", "100", "101", "010"),
        'D' to listOf("110", "101", "101", "101", "110"),
        'E' to listOf("111", "100", "111", "100", "111"),
        'F' to listOf("111", "100", "111", "100", "100"),
        'G' to listOf("011", "100", "101", "101", "011"),
        'H' to listOf("101", "101", "111", "101", "101"),
        'I' to listOf("111", "010", "010", "010", "111"),
        'J' to listOf("111", "001", "001", "001", "110"),
        'K' to listOf("101", "101", "110", "101", "101"),
        'L' to listOf("100", "100", "100", "100", "111"),
        'M' to listOf("101", "111", "111", "101", "101"),
        'N' to listOf("110", "101", "101", "101", "101"),
        'O' to listOf("010", "101", "101", "101", "010"),
        'P' to listOf("110", "101", "110", "100", "100"),
        'Q' to listOf("111", "101", "101", "111", "011"),
        'R' to listOf("110", "101", "110", "101", "101"),
        'S' to listOf("011", "100", "010", "001", "110"),
        'T' to listOf("111", "010", "010", "010", "010"),
        'U' to listOf("101", "101", "101", "101", "011"),
        'V' to listOf("101", "101", "101", "101", "010"),
        'W' to listOf("101", "101", "101", "111", "111"),
        'X' to listOf("101", "101", "010", "101", "101"),
        'Y' to listOf("101", "101", "010", "010", "010"),
        'Z' to listOf("111", "001", "010", "100", "111")
    )
}