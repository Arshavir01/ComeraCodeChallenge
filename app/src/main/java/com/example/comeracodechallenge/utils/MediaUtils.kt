package com.example.comeracodechallenge.utils

import android.content.res.Resources
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object MediaUtils {

    val Int.dpAsPx
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Int.pxAsDp
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()

    fun convertSecondsToHour(millis: Int): String {
        val duration = millis.toDuration(DurationUnit.MILLISECONDS)

        val timeString =
            duration.toComponents { hours, minutes, seconds, _ ->
                if (hours > 0) {
                    "%02d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    "%02d:%02d".format(minutes, seconds)
                }
            }
        return timeString
    }
}