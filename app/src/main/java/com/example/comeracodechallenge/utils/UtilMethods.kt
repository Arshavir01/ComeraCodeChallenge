package com.example.comeracodechallenge.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object UtilMethods {
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

    fun isVideoFile(filePath: String): Boolean {
        return getMimeType(filePath)?.startsWith("video/") == true
    }

    fun isImageFile(filePath: String): Boolean {
        return getMimeType(filePath)?.startsWith("image/") == true
    }

    private fun getMimeType(filePath: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun hasMediaPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imagePermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                        PackageManager.PERMISSION_GRANTED
            val videoPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
                        PackageManager.PERMISSION_GRANTED
            return imagePermission && videoPermission
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }
}