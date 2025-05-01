package com.example.harada

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class BoardManager(private val context: Context) {

    // Board state variables
    var isAlternateBoard = false

    // SharedPreferences for the main board (day mode) and tracking
    private val prefs: SharedPreferences = context.getSharedPreferences("GridPrefs", Context.MODE_PRIVATE)
    private val timePrefs: SharedPreferences = context.getSharedPreferences("TimePrefs", Context.MODE_PRIVATE)

    // New SharedPreferences for daily history tracking
    private val dailyHistoryPrefs: SharedPreferences = context.getSharedPreferences("DailyHistoryPrefs", Context.MODE_PRIVATE)

    // Constants
    val numRows = 9
    val numCols = 9

    // Constants for the running average feature
    private val runningAverageDays = 10 // Track 10 days for running average

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

    init {
        // Check and update the daily records on initialization
        checkAndUpdateDailyRecords()
    }

    // Helper method to get the current color scheme based on which board is showing
    fun getCurrentHues(): Map<Pair<Int, Int>, Float> {
        return if (isAlternateBoard) altSubgridHues else subgridHues
    }

    // Get cell value (only day mode values exist now)
    fun getCellValue(row: Int, col: Int): Int {
        return prefs.getInt("cell_${row}_${col}", 0)
    }

    // Get cell value for display - in night mode, return last 10 days usage count
    fun getCellDisplayValue(row: Int, col: Int): Int {
        return if (isAlternateBoard) {
            // In night mode, return the 10-day usage count (0-10)
            getLastTenDaysCount(row, col)
        } else {
            // In day mode, return the normal value
            getCellValue(row, col)
        }
    }

    // Save cell value (only updates day mode values)
    fun saveCell(row: Int, col: Int, value: Int) {
        val currentValue = getCellValue(row, col)

        // Only process if the value has actually changed (to avoid redundant saves)
        if (value != currentValue) {
            prefs.edit().putInt("cell_${row}_${col}", value).apply()

            // Save the timestamp when the cell was updated
            if (value > currentValue) {  // Only save timestamp for increments, not resets or decrements
                val key = "last_pressed_${row}_${col}"
                timePrefs.edit().putLong(key, System.currentTimeMillis()).apply()

                // Update daily record for cells when they are incremented
                updateDailyRecord(row, col)
            }
        }
    }

    // Check if a cell was pressed today
    fun isCellPressedRecently(row: Int, col: Int): Boolean {
        val key = "last_pressed_${row}_${col}"
        val lastPressed = timePrefs.getLong(key, 0)

        if (lastPressed == 0L) {
            return false
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfDay = calendar.timeInMillis
        return lastPressed > startOfDay
    }

    // Check if a cell was incremented yesterday
    fun wasIncrementedInPreviousPeriod(row: Int, col: Int): Boolean {
        val key = "last_pressed_${row}_${col}"
        val lastPressed = timePrefs.getLong(key, 0)

        if (lastPressed == 0L) {
            return false
        }

        val calendar = Calendar.getInstance()
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

    // Check when a cell was last incremented (for day mode coloring)
    fun getLastIncrementedPeriod(row: Int, col: Int): IncrementStatus {
        val key = "last_pressed_${row}_${col}"
        val lastPressed = timePrefs.getLong(key, 0)

        if (lastPressed == 0L) {
            return IncrementStatus.NEVER
        }

        // Check if it was pressed today
        if (isCellPressedRecently(row, col)) {
            return IncrementStatus.CURRENT_PERIOD
        }

        // Check if it was pressed yesterday
        if (wasIncrementedInPreviousPeriod(row, col)) {
            return IncrementStatus.PREVIOUS_PERIOD
        }

        // If it was pressed but not today or yesterday
        return IncrementStatus.LONG_AGO
    }

    // Enum to represent cell increment status
    enum class IncrementStatus {
        NEVER,              // Cell has never been incremented
        CURRENT_PERIOD,     // Cell was incremented today
        PREVIOUS_PERIOD,    // Cell was incremented yesterday
        LONG_AGO            // Cell was incremented more than one day ago
    }

    // Function to calculate total sum from all buttons
    fun calculateTotalSum(): Int {
        // If in alternate mode (night mode), we want to sum the 10-day usage counts
        if (isAlternateBoard) {
            // For night mode, sum up all cells' last 10 days usage (total possible: numActiveCells * 10)
            var totalUsageDays = 0
            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    // Skip cells that are not buttons (emojis, center, or summary block)
                    if (row in 3..5 && col in 3..5) continue
                    if (blockCenterEmojis.containsKey(Pair(row, col))) continue

                    // Get 10-day usage count
                    val cellUsage = getLastTenDaysCount(row, col)
                    totalUsageDays += cellUsage
                }
            }
            return totalUsageDays
        } else {
            // Day mode - regular value calculation
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

                if (isAlternateBoard) {
                    // Night mode: Get 10-day usage count
                    blockSum += getLastTenDaysCount(row, col)
                } else {
                    // Day mode: Add normal value to sum
                    blockSum += getCellValue(row, col)
                }
            }
        }

        return blockSum
    }

    // LAST 10 DAYS TRACKING IMPLEMENTATION

    // Generate key for storing daily history
    private fun getDailyHistoryKey(row: Int, col: Int, dayOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -dayOffset) // Go back by dayOffset days

        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        return "history_${row}_${col}_${year}_${dayOfYear}"
    }

    // Check if we've recorded activity for today
    private fun hasTodayRecord(): Boolean {
        val lastRecordDateKey = "last_record_date"
        val lastRecordDate = dailyHistoryPrefs.getLong(lastRecordDateKey, 0)

        if (lastRecordDate == 0L) return false

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastRecordDate
        val lastRecordYear = calendar.get(Calendar.YEAR)
        val lastRecordDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = System.currentTimeMillis()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        return (lastRecordYear == todayYear && lastRecordDay == todayDay)
    }

    // Check and update daily records
    private fun checkAndUpdateDailyRecords() {
        // Skip if we already recorded today
        if (hasTodayRecord()) return

        // Update the last record date
        val lastRecordDateKey = "last_record_date"
        dailyHistoryPrefs.edit().putLong(lastRecordDateKey, System.currentTimeMillis()).apply()

        // For each cell, record values for today
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                // Skip cells that are not buttons
                if (row in 3..5 && col in 3..5) continue
                if (blockCenterEmojis.containsKey(Pair(row, col))) continue

                updateDailyRecord(row, col)
            }
        }
    }

    // Update the daily record for a specific cell
    private fun updateDailyRecord(row: Int, col: Int) {
        val todayKey = getDailyHistoryKey(row, col, 0)

        // Calculate value for today - binary (used = 1, not used = 0)
        val currentValue = getCellValue(row, col)

        // A cell is either used today (1) or not (0)
        // If the value is greater than 0, we count it as used (1)
        val used = if (currentValue > 0) 1 else 0

        // Store the binary value
        dailyHistoryPrefs.edit().putInt(todayKey, used).apply()
    }

    // Get daily value for a specific day (1 if used, 0 if not)
    private fun getDailyValue(row: Int, col: Int, dayOffset: Int): Int {
        val key = getDailyHistoryKey(row, col, dayOffset)

        // FIXED: Safely handle both Float and Integer values that might be stored
        return try {
            // First try to get as int (new format)
            dailyHistoryPrefs.getInt(key, 0)
        } catch (e: ClassCastException) {
            try {
                // If that fails, try to get as float (old format) and convert to int
                val floatVal = dailyHistoryPrefs.getFloat(key, 0f)
                if (floatVal > 0.5f) 1 else 0 // Convert float to binary int
            } catch (e2: Exception) {
                // If all fails, return 0
                0
            }
        }
    }

    // Count how many of the last 10 days this cell was used
    fun getLastTenDaysCount(row: Int, col: Int): Int {
        var count = 0

        for (day in 0 until runningAverageDays) {
            // Add 1 for each day the cell was used
            count += getDailyValue(row, col, day)
        }

        return count
    }
}