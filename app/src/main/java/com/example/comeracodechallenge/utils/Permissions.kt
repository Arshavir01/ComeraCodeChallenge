package com.example.comeracodechallenge.utils

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.core.app.ActivityCompat

fun shouldShowMediaPermissionRationale(activity: Activity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shouldShowRationaleImages =
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        val shouldShowRationaleVideos =
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        return shouldShowRationaleImages && shouldShowRationaleVideos
    } else {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }
}