package com.example.harada

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private val numRows = 9
    private val numCols = 9
    private val maxCount = 60

    // UI Components
    private lateinit var gridLayout: GridLayout
    private lateinit var rootLayout: ConstraintLayout
    private val buttonGrid = Array(numRows) { arrayOfNulls<Button>(numCols) }
    private lateinit var centerSumView: TextView
    private val summaryTextViews = mutableMapOf<Pair<Int, Int>, TextView>()

    // Board state manager
    private lateinit var boardManager: BoardManager

    // Animation components
    private lateinit var flipOutAnimation: AnimatorSet
    private lateinit var flipInAnimation: AnimatorSet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize components
        gridLayout = findViewById(R.id.gridLayout)
        rootLayout = findViewById(R.id.rootLayout)

        // Initialize the board manager
        boardManager = BoardManager(this)

        // Set up the 3D flip animation resources
        setupFlipAnimations()

        // Create border lines for 3x3 blocks
        rootLayout.post {
            addBlockBorders()
        }

        // Initialize the board
        setupBoard()
    }

    private fun setupFlipAnimations() {
        // Camera distance for 3D effect
        val distance = 8000
        gridLayout.cameraDistance = distance.toFloat()

        // Create custom flip animations
        flipOutAnimation = AnimatorInflater.loadAnimator(
            this,
            R.animator.card_flip_out
        ) as AnimatorSet

        flipOutAnimation.apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        flipInAnimation = AnimatorInflater.loadAnimator(
            this,
            R.animator.card_flip_in
        ) as AnimatorSet

        flipInAnimation.apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun setupBoard() {
        val displayMetrics = resources.displayMetrics
        val cellSize = (minOf(displayMetrics.widthPixels, displayMetrics.heightPixels) - 20) / 9

        // Clear the grid before setting up
        gridLayout.removeAllViews()
        buttonGrid.forEach { it.fill(null) }
        summaryTextViews.clear()

        // Update background color based on current board
        if (boardManager.isAlternateBoard) {
            rootLayout.setBackgroundColor(Color.parseColor("#202020")) // Dark background for night mode
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF")) // Light background for day mode
        }

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val isInSummaryBlock = row in 3..5 && col in 3..5
                val isCenter = row == 4 && col == 4  // Flag for center cell
                val isCenterEmoji = boardManager.blockCenterEmojis.containsKey(Pair(row, col))

                if (isCenter) {
                    createCenterSumCell(cellSize)
                } else if (isInSummaryBlock) {
                    createSummaryCell(row, col, cellSize)
                } else if (isCenterEmoji) {
                    createEmojiCell(row, col, cellSize)
                } else {
                    createButtonCell(row, col, cellSize)
                }
            }
        }
    }

    private fun createCenterSumCell(cellSize: Int) {
        // Create the center sum display (total of all values)
        centerSumView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(0, 0, 0, 0)
            }
            gravity = Gravity.CENTER
            textSize = 20f
            text = boardManager.calculateTotalSum().toString()

            // Set text color based on day/night mode
            if (boardManager.isAlternateBoard) {
                setTextColor(Color.WHITE)
            } else {
                setTextColor(Color.BLACK)
            }

            setBackgroundColor(Color.TRANSPARENT)
            // Make the text bold
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        // Set up long press listener on the center cell
        var flipped = false
        val handler = Handler(Looper.getMainLooper())
        val flipRunnable = Runnable {
            // Flip the board after holding for 1 second, without requiring finger lift
            if (!flipped) {
                flipBoard()
                flipped = true
            }
        }

        centerSumView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flipped = false
                    handler.postDelayed(flipRunnable, 1000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(flipRunnable)
                    true
                }
                else -> false
            }
        }

        gridLayout.addView(centerSumView)
    }

    private fun createSummaryCell(row: Int, col: Int, cellSize: Int) {
        // Create a TextView for each position in the summary block (middle 3x3)
        val summaryTextView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(0, 0, 0, 0)
            }
            gravity = Gravity.CENTER
            textSize = 18f

            // Calculate which 3x3 block this summary cell represents
            val blockRow = (row - 3)  // 0, 1, 2
            val blockCol = (col - 3)  // 0, 1, 2

            // Calculate initial sum for this block
            val blockSum = boardManager.calculateBlockSum(blockRow, blockCol)
            text = blockSum.toString()

            // Set text color based on the block's hue, but with maximum saturation
            val hue = boardManager.getCurrentHues()[Pair(blockRow, blockCol)] ?: 0f
            val hsv = floatArrayOf(hue, 0.8f, 0.5f) // Higher saturation and deeper color
            setTextColor(Color.HSVToColor(hsv))

            // Make the text bold
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundColor(Color.TRANSPARENT)

            // Set up long press listener
            var flipped = false
            val handler = Handler(Looper.getMainLooper())
            val flipRunnable = Runnable {
                // Flip the board after holding for 1 second, without requiring finger lift
                if (!flipped) {
                    flipBoard()
                    flipped = true
                }
            }

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        flipped = false
                        handler.postDelayed(flipRunnable, 1000)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(flipRunnable)
                        true
                    }
                    else -> false
                }
            }
        }

        // Store reference to this TextView for updating later
        summaryTextViews[Pair(row - 3, col - 3)] = summaryTextView
        gridLayout.addView(summaryTextView)
    }

    private fun createEmojiCell(row: Int, col: Int, cellSize: Int) {
        // Create emoji view for centers of non-summary 3x3 blocks
        val emoji = boardManager.blockCenterEmojis[Pair(row, col)]
        val emojiView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
                setMargins(0, 0, 0, 0)
            }
            gravity = Gravity.CENTER
            textSize = 18f
            text = emoji

            // Set text color based on day/night mode
            if (boardManager.isAlternateBoard) {
                setTextColor(Color.WHITE)
            } else {
                setTextColor(Color.BLACK)
            }

            setBackgroundColor(Color.TRANSPARENT)
        }
        gridLayout.addView(emojiView)
    }

    private fun createButtonCell(row: Int, col: Int, cellSize: Int) {
        val savedValue = boardManager.getCellValue(row, col)
        val button = Button(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize + 4 // Add 1 pixel overlap
                height = cellSize + 4 // Add 1 pixel overlap
                // Remove any spacing or margins
                setMargins(-2, -2, 0, 0) // Negative margin creates overlap
            }
            gravity = Gravity.CENTER
            // Remove default padding for flat appearance
            setPadding(0, 0, 0, 0)
            textSize = 16f
            text = savedValue.toString()

            // Enhanced text color logic based on increment status
            when (boardManager.getLastIncrementedPeriod(row, col)) {
                BoardManager.IncrementStatus.CURRENT_PERIOD -> {
                    setTextColor(boardManager.pressedTextColor) // Green
                }
                BoardManager.IncrementStatus.PREVIOUS_PERIOD -> {
                    // Default color (Black/White)
                    if (boardManager.isAlternateBoard) {
                        setTextColor(Color.WHITE)
                    } else {
                        setTextColor(Color.BLACK)
                    }
                }
                BoardManager.IncrementStatus.LONG_AGO,
                BoardManager.IncrementStatus.NEVER -> { // Combining these cases
                    setTextColor(boardManager.notIncrementedTextColor) // Red
                }
            }

            // Use flat style on both day and night mode
            background = ColorUtils.makeColoredBackground(savedValue, row, col, boardManager)

            // Remove any default button styling
            isAllCaps = false
            stateListAnimator = null
        }

        var didReset = false
        val handler = Handler(Looper.getMainLooper())
        val resetRunnable = Runnable {
            button.text = "0"
            boardManager.saveCell(row, col, 0)
            button.background = ColorUtils.makeColoredBackground(0, row, col, boardManager)
            AnimationUtils.playSparkleAnimation(button)
            button.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            didReset = true

            // Reset to default text color on reset
            if (boardManager.isAlternateBoard) {
                button.setTextColor(Color.WHITE)
            } else {
                button.setTextColor(Color.BLACK)
            }

            // Get block coordinates for this cell
            val blockRow = row / 3
            val blockCol = col / 3

            // Update only the summary for this specific block
            updateBlockSum(blockRow, blockCol)

            // Also update the total sum
            updateTotalSum()
        }

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    didReset = false
                    handler.postDelayed(resetRunnable, 1000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(resetRunnable)
                    if (!didReset && event.action == MotionEvent.ACTION_UP) {
                        button.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        val current = button.text.toString().toInt()
                        val updated = (current + 1).coerceAtMost(maxCount)
                        button.text = updated.toString()
                        boardManager.saveCell(row, col, updated)
                        button.background = ColorUtils.makeColoredBackground(updated, row, col, boardManager)

                        // Update text color to green to indicate pressed today/this week
                        if (updated > 0) {
                            button.setTextColor(boardManager.pressedTextColor)
                        }

                        // Get block coordinates for this cell
                        val blockRow = row / 3
                        val blockCol = col / 3

                        // Update only the summary for this specific block
                        updateBlockSum(blockRow, blockCol)

                        // Also update the total sum
                        updateTotalSum()
                    }
                    true
                }
                else -> false
            }
        }

        buttonGrid[row][col] = button
        gridLayout.addView(button)
    }

    // Function to update only a specific block sum
    private fun updateBlockSum(blockRow: Int, blockCol: Int) {
        // Calculate the sum for this specific block
        val blockSum = boardManager.calculateBlockSum(blockRow, blockCol)

        // Find the correct summary cell in the middle 3x3
        val summaryView = summaryTextViews[Pair(blockRow, blockCol)]

        // Update the text and play animation only on this specific summary cell
        summaryView?.let {
            it.text = blockSum.toString()
            AnimationUtils.playSparkleAnimation(it)
        }
    }

    // Function to update only the total sum
    private fun updateTotalSum() {
        // Calculate the new total sum
        val newTotalSum = boardManager.calculateTotalSum()

        // Update the center cell
        centerSumView.text = newTotalSum.toString()
        AnimationUtils.playSparkleAnimation(centerSumView)
    }

    // Function to add subtle borders around each 3x3 block
    private fun addBlockBorders() {
        val displayMetrics = resources.displayMetrics
        val cellSize = (minOf(displayMetrics.widthPixels, displayMetrics.heightPixels) - 20) / 9

        // Calculate grid dimensions
        val gridWidth = cellSize * 9
        val gridHeight = cellSize * 9

        // Border color based on board mode
        val borderColor = if (boardManager.isAlternateBoard) {
            Color.parseColor("#505050") // Lighter gray for night mode
        } else {
            Color.GRAY // Standard gray for day mode
        }

        // Add vertical lines between 3x3 blocks (at columns 3 and 6)
        for (i in 1..2) {
            val dividerPosition = cellSize * i * 3

            val verticalDivider = View(this).apply {
                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    2, // width - slightly thicker for visibility
                    gridHeight
                ).apply {
                    // Position relative to gridLayout
                    topToTop = gridLayout.id
                    bottomToBottom = gridLayout.id
                    startToStart = gridLayout.id
                    marginStart = dividerPosition - 1 // Center on the boundary
                }
                setBackgroundColor(borderColor)
                alpha = 0.3f // Make it subtle
            }

            rootLayout.addView(verticalDivider)
        }

        // Add horizontal lines between 3x3 blocks (at rows 3 and 6)
        for (i in 1..2) {
            val dividerPosition = cellSize * i * 3

            val horizontalDivider = View(this).apply {
                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    gridWidth,
                    2 // height - slightly thicker for visibility
                ).apply {
                    // Position relative to gridLayout
                    startToStart = gridLayout.id
                    endToEnd = gridLayout.id
                    topToTop = gridLayout.id
                    topMargin = dividerPosition - 1 // Center on the boundary
                }
                setBackgroundColor(borderColor)
                alpha = 0.3f // Make it subtle
            }

            rootLayout.addView(horizontalDivider)
        }
    }

    // Flip the board when center is held
    private fun flipBoard() {
        // Apply haptic feedback
        gridLayout.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

        // Create a background transition view to smooth the change
        val transitionView = View(this).apply {
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            )

            // Set initial background color based on current state
            val targetColor = if (boardManager.isAlternateBoard) {
                Color.parseColor("#FFFFFF") // Transitioning to light
            } else {
                Color.parseColor("#202020") // Transitioning to dark
            }

            // Start fully transparent
            setBackgroundColor(targetColor)
            alpha = 0f

            // Place it on top of everything but below the grid
            elevation = 5f
        }

        // Add the transition view
        rootLayout.addView(transitionView)

        // Start the background fade AND the flip animation simultaneously
        transitionView.animate().alpha(0.5f).setDuration(300)

        // Start the flip out animation immediately
        flipOutAnimation.setTarget(gridLayout)
        flipOutAnimation.start()

        // When flip out animation is half-way done (at 90 degrees), switch the board data
        Handler(Looper.getMainLooper()).postDelayed({
            // Toggle which board we're showing
            boardManager.isAlternateBoard = !boardManager.isAlternateBoard

            // Remove existing dividers (they will be recreated with correct colors)
            for (i in rootLayout.childCount - 1 downTo 0) {
                val child = rootLayout.getChildAt(i)
                if (child is View && child !== gridLayout && child !== transitionView) {
                    rootLayout.removeView(child)
                }
            }

            // Setup board again with new data
            setupBoard()

            // Add borders again with correct colors
            addBlockBorders()

            // Continue with the flip in animation
            flipInAnimation.setTarget(gridLayout)
            flipInAnimation.start()

            // Begin fading out the transition view gradually as the board flips back in
            transitionView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    // Remove the transition view when animation is complete
                    rootLayout.removeView(transitionView)
                }

        }, 200) // Exactly at the mid-point of the flip animation
    }
}