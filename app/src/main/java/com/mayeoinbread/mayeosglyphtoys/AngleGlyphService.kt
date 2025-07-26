package com.mayeoinbread.mayeosglyphtoys

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class AngleGlyphService : GlyphMatrixService("Angle-Glyph"), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var currentOrientation = FloatArray(3)
    private var rotationMatrix = FloatArray(9)

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

    private fun drawAngleFrame(pitch: Float, roll: Float) {
        val matrix = IntArray(DrawUtils.SCREEN_LENGTH * DrawUtils.SCREEN_LENGTH)

        // Lower the brightness of the line if the phone goes off axis
        val rollDeviation = abs(abs(roll) - 90f)
        val brightness = ((1f - (rollDeviation / 90f)) * 2047).toInt().coerceIn(0, 2047)

        val length = 11

        val angleRad = Math.toRadians(-pitch.toDouble() - 90)

        val dx = length * cos(angleRad)
        val dy = length * sin(angleRad)

        val x0 = (DrawUtils.SCREEN_CENTER - dx).toInt()
        val y0 = (DrawUtils.SCREEN_CENTER - dy).toInt()
        val x1 = (DrawUtils.SCREEN_CENTER + dx).toInt()
        val y1 = (DrawUtils.SCREEN_CENTER + dy).toInt()

        DrawUtils.drawLineOnMatrix(x0, y0, x1, y1, brightness, matrix)

        // Draw a centered "+" on the screen, plus a marker at each edge for reference to horizontal
        DrawUtils.drawLineOnMatrix(12, 0, 12, 1, 256, matrix)
        DrawUtils.drawLineOnMatrix(12, 23, 12, 24, 256, matrix)
        DrawUtils.drawLineOnMatrix(12, 11, 12, 13, 256, matrix)
        DrawUtils.drawLineOnMatrix(11, 12, 13, 12, 256, matrix)

        val intAngle = pitch.toInt().coerceIn(-180, 180)
        val angleStr = "${if (intAngle < 0) "-" else ""}${abs(intAngle)}°"

        val totalWidth = angleStr.length * (DrawUtils.CHAR_WIDTH + DrawUtils.SPACING) - DrawUtils.SPACING
        val baseX = DrawUtils.SCREEN_LENGTH - DrawUtils.CHAR_HEIGHT - 1
        val baseY = (DrawUtils.SCREEN_LENGTH - totalWidth) / 2

        // Get a rectangle shape that bounds the text
        val (width, height) = DrawUtils.getBoundsOfText(angleStr, true)
        val yOffset = (DrawUtils.SCREEN_LENGTH - height) / 2

        val frame = GlyphMatrixFrame.Builder()
            .addLow(matrix)
            .addMid(
                // Draw "black" rectangle "behind" text to stop line showing through, makes things easy to read
                DrawUtils.drawFilledRect(
                    DrawUtils.SCREEN_LENGTH - width,
                    yOffset,
                    DrawUtils.SCREEN_LENGTH,
                    DrawUtils.SCREEN_LENGTH - yOffset,
                    0,
                    matrix
                )
            )
            .addTop(
                DrawUtils.drawRotatedText(angleStr, baseX, baseY, 1024)
            ).build(applicationContext)

        glyphMatrixManager?.setMatrixFrame(frame.render())
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}