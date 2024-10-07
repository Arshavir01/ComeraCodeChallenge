package com.example.comeracodechallenge.utils

import android.content.res.Resources

val Int.dpAsPx
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.pxAsDp
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()