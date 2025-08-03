package com.lhk.sportsglyphtoy

import kotlin.math.abs

object DrawUtils {

    enum class TextAlign {
        // LEFT_C is left of text at h center, RIGHT_C is right of text at h center
        LEFT, LEFT_C, H_CENTER, RIGHT, RIGHT_C,
        // TOP_C is top of text at v center, BOTTOM_C is bottom of text at v center
        TOP, TOP_C, V_CENTER, BOTTOM, BOTTOM_C
    }

    const val SCREEN_LENGTH = 25
    const val SCREEN_CENTER = 12.5f
    const val CHAR_WIDTH = 3
    const val CHAR_HEIGHT = 5
    const val SPACING = 1

    fun drawLineOnMatrix(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        value: Int,
        matrix: IntArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    ) {
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

    private fun rotate90(font: List<String>): List<String> {
        val rotated = MutableList(CHAR_WIDTH) {""}
        for (x in 0 until CHAR_WIDTH) {
            for (y in 0 until CHAR_HEIGHT) {
                rotated[x] = rotated[x] + font[y][x]
            }
        }
        return rotated.reversed()
    }

    private fun getTLForText(
        text: String,
        hAlign: TextAlign,
        vAlign: TextAlign,
        padding: Int = 0  // If we're in the center, add an offset
    ): Pair<Int, Int> {
        val bounds = getBoundsOfText(text, withBorder = false)
        val xVal: Int = when (hAlign) {
            TextAlign.H_CENTER -> {
                (SCREEN_CENTER - (bounds.first / 2f)).toInt()
            }
            TextAlign.RIGHT -> {
                SCREEN_LENGTH - bounds.first - padding
            }
            TextAlign.LEFT_C -> {
                SCREEN_CENTER.toInt() + padding
            }
            TextAlign.RIGHT_C -> {
                (SCREEN_CENTER - bounds.first).toInt() - padding
            }
            else -> {
                padding
            }
        }

        val yVal: Int = when (vAlign) {
            TextAlign.V_CENTER -> {
                (SCREEN_CENTER - (bounds.second / 2f)).toInt()
            }
            TextAlign.BOTTOM -> {
                SCREEN_LENGTH - bounds.second - padding
            }
            TextAlign.TOP_C -> {
                SCREEN_CENTER.toInt() + padding
            }
            TextAlign.BOTTOM_C -> {
                (SCREEN_CENTER - bounds.second).toInt() - padding
            }
            else -> {
                padding
            }
        }

        return Pair(xVal, yVal)
    }

    // Draw text using custom 3x5 font
    fun drawNormalText(
        text: String,
        hAlign: TextAlign,
        vAlign: TextAlign,
        brightness: Int,
        padding: Int = 0,
        matrix: IntArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    ) {
        val (baseX, baseY) = getTLForText(text, hAlign, vAlign, padding)

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

    fun drawFilledRect(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        brightness: Int,
        matrix: IntArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    ): IntArray {

        for (x in x0 until x1) {
            for (y in y0 until y1) {
                matrix[y * SCREEN_LENGTH + x] = brightness
            }
        }

        return matrix
    }

    fun drawLineRect(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        brightness: Int,
        matrix: IntArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    ): IntArray {

        drawLineOnMatrix(x0, y0, x1, y0, brightness, matrix)
        drawLineOnMatrix(x1, y0, x1, y1, brightness, matrix)
        drawLineOnMatrix(x1, y1, x0, y1, brightness, matrix)
        drawLineOnMatrix(x0, y1, x0, y0, brightness, matrix)

        return matrix
    }

    fun getBoundsOfText(
        text: String,
        rotated: Boolean = false,
        withBorder: Boolean = true
    ): Pair<Int, Int> {
        // width x height
        var textWidth = text.length * (CHAR_WIDTH + SPACING)
        var textHeight = CHAR_HEIGHT + 2 * SPACING
        if (!withBorder) {
            textWidth -= 2 * SPACING
            textHeight -= 2 * SPACING
        }
        return if (rotated) {
            Pair(textHeight, textWidth)
        } else {
            Pair(textWidth, textHeight)
        }
    }

    fun drawRotatedText(
        text: String,
        baseX: Int,
        baseY: Int,
        brightness: Int,
        matrix: IntArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH)
    ): IntArray {
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
        return matrix
    }

    fun drawScrollingTextCharacterWise(
        text: String,
        scrollIndex: Int,
        hAlign: TextAlign,
        vAlign: TextAlign,
        brightness: Int,
        padding: Int = 0,
        matrix: IntArray = IntArray(SCREEN_LENGTH * SCREEN_LENGTH),
        rotate: Boolean = false
    ) {
        val fullText = "$text  $text  "
        val displayableChars = SCREEN_LENGTH / (3 + SPACING)

        val (baseX, baseY) = getTLForText(fullText, hAlign, vAlign, padding)

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

    private val font: Map<Char, List<String>> = mapOf(
        '0' to listOf("010", "101", "101", "101", "010"),
        '1' to listOf("010", "110", "010", "010", "010"),
        '2' to listOf("110", "001", "010", "100", "111"),
        '3' to listOf("110", "001", "110", "001", "110"),
        '4' to listOf("101", "101", "011", "001", "001"),
        '5' to listOf("111", "100", "110", "001", "110"),
        '6' to listOf("010", "100", "110", "101", "010"),
        '7' to listOf("111", "001", "010", "010", "010"),
        '8' to listOf("010", "101", "010", "101", "010"),
        '9' to listOf("010", "101", "011", "001", "010"),
        '-' to listOf("000", "000", "111", "000", "000"),
        '°' to listOf("010", "101", "010", "000", "000"),
        '^' to listOf("000", "010", "111", "000", "000"),
        'v' to listOf("000", "000", "000", "111", "010"),
        ':' to listOf("0", "1", "0", "1", "0"),
        '@' to listOf("000", "000", "010", "000", "000"),
        ' ' to listOf("000", "000", "000", "000", "000"),
        '.' to listOf("000", "000", "000", "000", "010"),
        '|' to listOf("010", "010", "010", "010", "010"),
        '\'' to listOf("010", "010", "000", "000", "000"),
        '&' to listOf("010", "101", "010", "101", "011"),
        '(' to listOf("010", "100", "100", "100", "010"),
        ')' to listOf("010", "001", "001", "001", "010"),
        '[' to listOf("011", "010", "010", "010", "011"),
        ']' to listOf("110", "010", "010", "010", "110"),
        '#' to listOf("101", "111", "101", "111", "101"),
        'A' to listOf("010", "101", "111", "101", "101"),
        'B' to listOf("110", "101", "110", "101", "110"),
        'C' to listOf("010", "101", "100", "101", "010"),
        'D' to listOf("110", "101", "101", "101", "110"),
        'E' to listOf("111", "100", "110", "100", "111"),
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
        'Z' to listOf("111", "001", "010", "100", "111"),
        'a' to listOf("000", "000", "000", "000", "000"), // 0, 0 out
        'b' to listOf("000", "000", "001", "000", "000"), // 1B, 0 out
        'c' to listOf("000", "010", "000", "000", "000"), // 2B, 0 out
        'd' to listOf("000", "000", "100", "000", "000"), // 3B, 0 out
        'e' to listOf("000", "010", "001", "000", "000"), // 1B+2B, 0 out
        'f' to listOf("000", "010", "100", "000", "000"), // 2B+3B, 0 out
        'g' to listOf("000", "010", "101", "000", "000"), // 1B+2B+3B, 0 out
        'h' to listOf("000", "000", "000", "000", "100"), // 0, 1 out
        'i' to listOf("000", "000", "001", "000", "100"), // 1B, 1 out
        'j' to listOf("000", "010", "000", "000", "100"), // 2B, 1 out
        'k' to listOf("000", "000", "100", "000", "100"), // 3B, 1 out
        'l' to listOf("000", "010", "001", "000", "100"), // 1B+2B, 1 out
        'm' to listOf("000", "010", "100", "000", "100"), // 2B+3B, 1 out
        'n' to listOf("000", "010", "101", "000", "100"), // 1B+2B+3B, 1 out
        'o' to listOf("000", "000", "000", "000", "110"), // 0, 2 out
        'p' to listOf("000", "000", "001", "000", "110"), // 1B, 2 out
        'q' to listOf("000", "010", "000", "000", "110"), // 2B, 2 out
        'r' to listOf("000", "000", "100", "000", "110"), // 3B, 2 out
        's' to listOf("000", "010", "001", "000", "110"), // 1B+2B, 2 out
        't' to listOf("000", "010", "100", "000", "110"), // 2B+3B, 2 out
        'u' to listOf("000", "010", "101", "000", "110"), // 1B+2B+3B, 2 out
        '_' to listOf("000", "000", "000", "000", "111") // 0 on, 3 out
    )
}