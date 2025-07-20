package com.mayeoinbread.mayeosglyphtoys

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nothing.ketchum.GlyphMatrixManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class AngleGlyphService : GlyphMatrixService("Angle-Glyph"), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var currentOrientation = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private val angleHistory = ArrayDeque<Float>(TRAIL_LENGTH)

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, currentOrientation)

            val pitch = Math.toDegrees(currentOrientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(currentOrientation[2].toDouble()).toFloat()

            drawAngleFrame(pitch, roll)
        }
    }

    fun drawRotatedDigitText(matrix: IntArray, text: String, brightness: Int) {
        val glyphWidth = 3
        val glyphHeight = 5
        val spacing = 1
        val totalWidth = text.length * (glyphWidth + spacing) - spacing

        val baseX = GLYPH_LENGTH - glyphHeight - 2
        val baseY = (GLYPH_LENGTH - totalWidth) / 2

        for ((i, c) in text.reversed().withIndex()) {
            val fontChar = font3x5[c] ?: continue
            val rotated: List<String> = rotateFont90(fontChar)
            for (y in rotated.indices) {
                for (x in rotated[y].indices) {
                    if(rotated[y][x] == '1') {
                        val mx = baseX + x
                        val my = baseY + i * (glyphWidth + spacing) + y
                        if (mx in 0..<GLYPH_LENGTH && my in 0..<GLYPH_LENGTH) {
                            matrix[my * GLYPH_LENGTH + mx] = brightness
                        }
                    }
                }
            }
        }
    }

    fun rotateFont90(font: List<String>): List<String> {
        val width = font[0].length
        val height = font.size
        val rotated = MutableList(width) {""}
        for (x in 0 until width) {
            for (y in 0 until height) {
                rotated[x] = rotated[x] + font[y][x]
            }
        }
        return rotated.reversed()
    }

    fun drawAngleFrame(pitch: Float, roll: Float) {
        val matrix = IntArray(GLYPH_LENGTH * GLYPH_LENGTH)

        val rollDeviation = abs(abs(roll) - 90f)
        val brightness = ((1f - (rollDeviation / 90f)) * 2047).toInt().coerceIn(0, 2047)

        val centerX = GLYPH_LENGTH / 2f
        val centerY = GLYPH_LENGTH / 2f
        val length = 11

        angleHistory.addLast(pitch)
        if (angleHistory.size > TRAIL_LENGTH) angleHistory.removeFirst()

        angleHistory.forEachIndexed { index, angle ->
            val fadeBrightness = (brightness * (1f - index / TRAIL_LENGTH)).toInt().coerceIn(0, 2047)
            val angleRad = Math.toRadians(-angle.toDouble() - 90)

            val dx = length * cos(angleRad)
            val dy = length * sin(angleRad)

            val x0 = (centerX - dx).toInt()
            val y0 = (centerY - dy).toInt()
            val x1 = (centerX + dx).toInt()
            val y1 = (centerY + dy).toInt()

            drawLineOnMatrix(matrix, x0, y0, x1, y1, fadeBrightness)
        }

        drawLineOnMatrix(matrix, 12, 0, 12, 1, 256)
        drawLineOnMatrix(matrix, 12, 23, 12, 24, 256)
        drawLineOnMatrix(matrix, 12, 11, 12, 13, 256)
        drawLineOnMatrix(matrix, 11, 12, 13, 12, 256)

        val intAngle = pitch.toInt().coerceIn(-180, 180)
        val angleStr = "${if (intAngle < 0) "-" else ""}${abs(intAngle)}°"

        drawRotatedDigitText(matrix, angleStr, 1024)

        glyphMatrixManager?.setMatrixFrame(matrix)
    }

    private fun drawLineOnMatrix(matrix: IntArray, x0: Int, y0: Int, x1: Int, y1: Int, value: Int) {
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var x = 0
        var y = 0

        while (true) {
            if (x in 0..<GLYPH_LENGTH && y in 0..<GLYPH_LENGTH) {
                matrix[y * GLYPH_LENGTH + x] = value
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

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private companion object {
        private const val GLYPH_LENGTH = 25
        private const val TRAIL_LENGTH = 3
    }

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
        '°' to listOf("010", "101", "010", "000", "000"),
    )
}