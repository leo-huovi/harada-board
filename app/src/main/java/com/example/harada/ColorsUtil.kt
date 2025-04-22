package com.example.harada

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object ColorUtils {

    // Create colored background for buttons based on value and position
    fun makeColoredBackground(value: Int, row: Int, col: Int, boardManager: BoardManager): GradientDrawable {
        val blockRow = row / 3
        val blockCol = col / 3
        val hue = boardManager.getCurrentHues()[Pair(blockRow, blockCol)] ?: 0f

        val step = (value / 5).coerceAtMost(12)

        // Adjust saturation and brightness based on night mode
        val saturation: Float
        val brightness: Float

        if (boardManager.isAlternateBoard) {
            // Night mode - darker background, higher saturation
            saturation = (0.05f + step * 0.03f).coerceAtMost(0.4f)
            brightness = (0.5f - step * 0.01f).coerceIn(0.4f, 0.5f)
        } else {
            // Day mode - lighter background, lower saturation
            saturation = (0.02f + step * 0.025f).coerceAtMost(0.3f)
            brightness = (1.0f - step * 0.003f).coerceIn(0.94f, 1.0f)
        }

        val hsv = floatArrayOf(hue, saturation, brightness)
        val color = Color.HSVToColor(hsv)

        return GradientDrawable().apply {
            cornerRadius = 0f
            setColor(color)

            // Add a padding that matches the button color instead of transparent
            // This ensures any tiny gaps will be filled with the button's color
            setPadding(1, 1, 1, 1)

            // No border/stroke on buttons - completely flat design
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