package com.example.harada

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object ColorUtils {

    // Create colored background for buttons based on value and position (day mode)
    fun makeColoredBackground(value: Int, row: Int, col: Int, boardManager: BoardManager): GradientDrawable {
        val blockRow = row / 3
        val blockCol = col / 3
        val hue = boardManager.getCurrentHues()[Pair(blockRow, blockCol)] ?: 0f

        val step = (value / 5).coerceAtMost(12)

        // Day mode - lighter background, lower saturation
        val saturation = (0.02f + step * 0.025f).coerceAtMost(0.3f)
        val brightness = (1.0f - step * 0.003f).coerceIn(0.94f, 1.0f)

        val hsv = floatArrayOf(hue, saturation, brightness)
        val color = Color.HSVToColor(hsv)

        return GradientDrawable().apply {
            cornerRadius = 0f
            setColor(color)
            // No border/stroke on buttons - completely flat design
            setStroke(0, Color.TRANSPARENT)
        }
    }

    // Create colored background for usage count cells (night mode)
    // Usage is an integer from 0-10 representing days used
    fun makeUsageBackground(daysUsed: Int, row: Int, col: Int, boardManager: BoardManager): GradientDrawable {
        val blockRow = row / 3
        val blockCol = col / 3
        val hue = boardManager.getCurrentHues()[Pair(blockRow, blockCol)] ?: 0f

        // Normalize days used (0-10) to determine intensity
        val intensity = daysUsed / 10f

        // For night mode usage count, we want deeper colors with higher saturation
        // as the days used increases
        val saturation = (0.2f + intensity * 0.6f).coerceIn(0.2f, 0.8f)
        val brightness = (0.5f - intensity * 0.2f).coerceIn(0.3f, 0.5f)

        val hsv = floatArrayOf(hue, saturation, brightness)
        val color = Color.HSVToColor(hsv)

        return GradientDrawable().apply {
            cornerRadius = 0f
            setColor(color)
            // No border - flat design
            setStroke(0, Color.TRANSPARENT)
        }
    }

    // Get color for a specific block
    fun getBlockColor(blockRow: Int, blockCol: Int, boardManager: BoardManager, maxSaturation: Boolean = false): Int {
        val hue = boardManager.getCurrentHues()[Pair(blockRow, blockCol)] ?: 0f

        // Adjust saturation and brightness based on night mode
        val saturation: Float
        val brightness: Float

        if (boardManager.isAlternateBoard) {
            // Night mode
            saturation = if (maxSaturation) 0.9f else 0.3f
            brightness = if (maxSaturation) 0.7f else 0.4f
        } else {
            // Day mode
            saturation = if (maxSaturation) 0.8f else 0.2f
            brightness = if (maxSaturation) 0.5f else 0.95f
        }

        val hsv = floatArrayOf(hue, saturation, brightness)
        return Color.HSVToColor(hsv)
    }
}