package com.example.harada

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation

object AnimationUtils {

    // Animation for when a value changes
    fun playSparkleAnimation(view: View) {
        val scale = ScaleAnimation(
            1f, 1.3f, 1f, 1.3f,
            AnimationSet.RELATIVE_TO_SELF, 0.5f,
            AnimationSet.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 150
            repeatMode = ScaleAnimation.REVERSE
            repeatCount = 1
        }

        val fade = AlphaAnimation(1f, 0.3f).apply {
            duration = 150
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = 1
        }

        val set = AnimationSet(true).apply {
            addAnimation(scale)
            addAnimation(fade)
        }

        view.startAnimation(set)
    }
}