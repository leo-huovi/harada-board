package com.example.harada

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class BoardManager(private val context: Context) {

    // Board state variables
    var isAlternateBoard = false

    // SharedPreferences for both boards
    private val prefs: SharedPreferences = context.getSharedPreferences("GridPrefs", Context.MODE_PRIVATE)
    private val altPrefs: SharedPreferences = context.getSharedPreferences("AltGridPrefs", Context.MODE_PRIVATE)
    private val timePrefs: SharedPreferences = context.getSharedPreferences("TimePrefs", Context.MODE_PRIVATE)
    private val lastCheckPrefs: SharedPreferences = context.getSharedPreferences("LastCheckPrefs", Context.MODE_PRIVATE)

    // Constants
    val numRows = 9
    val numCols = 9

    // Emojis for the centers of the non-summary 3x3 blocks
    val blockCenterEmojis = mapOf(
        Pair(1, 1) to "㊙\uFE0F", // top-left
        Pair(1, 4) to "\uD83C\uDF6E", // top-center
        Pair(1, 7) to "〽\uFE0F", // top-right
        Pair(4, 1) to "🌸", // middle-left
        Pair(4, 7) to "🍀", // middle-right
        Pair(7, 1) to "\uD83D\uDC7E", // bottom-left
        Pair(7, 4) to "\uD83D\uDCB7", // bottom-center
        Pair(7, 7) to "🍵"  // bottom-right
    )

    // Color schemes for the boards
    val subgridHues = mapOf(
        Pair(0, 0) to 0f,     // Red
        Pair(0, 1) to 30f,    // Orange
        Pair(0, 2) to 60f,    // Yellow
        Pair(1, 2) to 120f,   // Green
        Pair(2, 2) to 180f,   // Cyan
        Pair(2, 1) to 240f,   // Blue
        Pair(2, 0) to 270f,   // Violet
        Pair(1, 0) to 300f,   // Pink
        Pair(1, 1) to 330f    // Center: near neutral
    )

    // Alternate color scheme for the second board (same hues, night mode visuals)
    val altSubgridHues = subgridHues.toMap()

    // Color for pressed cells
    val pressedTextColor = android.graphics.Color.parseColor("#99e699") // Forest Green

    // New color for cells that have not been incremented for a while
    val notIncrementedTextColor = android.graphics.Color.parseColor("#cc6666") // Grayish Red

    // Helper method to get the current SharedPreferences based on which board is showing
    fun getCurrentPrefs(): SharedPreferences {
        return if (isAlternateBoard) altPrefs else prefs
    }

    // Helper method to get the current color scheme based on which board is showing
    fun getCurrentHues(): Map<Pair<Int, Int>, Float> {
        return if (isAlternateBoard) altSubgridHues else subgridHues
    }

    // Get cell value from the current board
    fun getCellValue(row: Int, col: Int): Int {
        return getCurrentPrefs().getInt("cell_${row}_${col}", 0)
    }

    // Save cell value to the current board
    fun saveCell(row: Int, col: Int, value: Int) {
        getCurrentPrefs().edit().putInt("cell_${row}_${col}", value).apply()

        // Save the timestamp when the cell was updated
        if (value > 0) {  // Only save timestamp for increments, not resets
            val key = if (isAlternateBoard) "alt_last_pressed_${row}_${col}" else "last_pressed_${row}_${col}"
            timePrefs.edit().putLong(key, System.currentTimeMillis()).apply()

            // Also update the last check timestamp to track when we last checked this cell
            val checkKey = if (isAlternateBoard) "alt_last_check_${row}_${col}" else "last_check_${row}_${col}"
            lastCheckPrefs.edit().putLong(checkKey, System.currentTimeMillis()).apply()
        }
    }

    // Check if a cell was pressed today (day mode) or this week (night mode)
    fun isCellPressedRecently(row: Int, col: Int): Boolean {
        val key = if (isAlternateBoard) "alt_last_pressed_${row}_${col}" else "last_pressed_${row}_${col}"
        val lastPressed = timePrefs.getLong(key, 0)

        if (lastPressed == 0L) {
            return false
        }

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        if (isAlternateBoard) {
            // Night mode: Check if pressed this week
            // Get start of current week (Sunday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val startOfWeek = calendar.timeInMillis
            return lastPressed > startOfWeek
        } else {
            // Day mode: Check if pressed today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val startOfDay = calendar.timeInMillis
            return lastPressed > startOfDay
        }
    }

    // Check if a cell was incremented in the previous period (yesterday in day mode, last week in night mode)
    fun wasIncrementedInPreviousPeriod(row: Int, col: Int): Boolean {
        val key = if (isAlternateBoard) "alt_last_pressed_${row}_${col}" else "last_pressed_${row}_${col}"
        val lastPressed = timePrefs.getLong(key, 0)

        if (lastPressed == 0L) {
            return false
        }

        val calendar = Calendar.getInstance()

        if (isAlternateBoard) {
            // Night mode: Check if pressed last week
            // Get start of last week
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfThisWeek = calendar.timeInMillis

            // Go back one week
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val startOfLastWeek = calendar.timeInMillis

            return lastPressed >= startOfLastWeek && lastPressed < startOfThisWeek
        } else {
            // Day mode: Check if pressed yesterday
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis

            // Go back one day
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val startOfYesterday = calendar.timeInMillis

            return lastPressed >= startOfYesterday && lastPressed < startOfToday
        }
    }

    // Check when a cell was last incremented
    fun getLastIncrementedPeriod(row: Int, col: Int): IncrementStatus {
        val key = if (isAlternateBoard) "alt_last_pressed_${row}_${col}" else "last_pressed_${row}_${col}"
        val lastPressed = timePrefs.getLong(key, 0)

        if (lastPressed == 0L) {
            return IncrementStatus.NEVER
        }

        // Check if it was pressed in the current period (today or this week)
        if (isCellPressedRecently(row, col)) {
            return IncrementStatus.CURRENT_PERIOD
        }

        // Check if it was pressed in the previous period (yesterday or last week)
        if (wasIncrementedInPreviousPeriod(row, col)) {
            return IncrementStatus.PREVIOUS_PERIOD
        }

        // If it was pressed but not in the current or previous period
        return IncrementStatus.LONG_AGO
    }

    // Enum to represent cell increment status
    enum class IncrementStatus {
        NEVER,              // Cell has never been incremented
        CURRENT_PERIOD,     // Cell was incremented in current period (today/this week)
        PREVIOUS_PERIOD,    // Cell was incremented in previous period (yesterday/last week)
        LONG_AGO            // Cell was incremented more than one period ago
    }

    // Function to calculate total sum from all buttons
    fun calculateTotalSum(): Int {
        var totalSum = 0
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                // Skip cells that are not buttons (emojis, center, or summary block)
                if (row in 3..5 && col in 3..5) continue
                if (blockCenterEmojis.containsKey(Pair(row, col))) continue

                // Get value from shared preferences
                val cellValue = getCellValue(row, col)
                totalSum += cellValue
            }
        }
        return totalSum
    }

    // Function to calculate sum for a specific 3x3 block
    fun calculateBlockSum(blockRow: Int, blockCol: Int): Int {
        var blockSum = 0

        // Calculate the starting row and column for this block
        val startRow = blockRow * 3
        val startCol = blockCol * 3

        // Sum all values in this block
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val row = startRow + r
                val col = startCol + c

                // Skip center emoji cell
                if (blockCenterEmojis.containsKey(Pair(row, col))) continue

                // Skip cells that are in the middle block
                if (row in 3..5 && col in 3..5) continue

                // Add value to sum
                val cellValue = getCellValue(row, col)
                blockSum += cellValue
            }
        }

        return blockSum
    }
}