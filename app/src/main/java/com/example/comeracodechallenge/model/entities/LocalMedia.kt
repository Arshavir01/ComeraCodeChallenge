package com.example.comeracodechallenge.model.entities

import android.net.Uri
import com.example.comeracodechallenge.utils.AppConstants.NO_ID
import com.example.comeracodechallenge.utils.MediaType

data class LocalMedia(
    val id: Long = NO_ID.toLong(),
    val uri: Uri?,
    val name: String,
    val duration: Int?,
    val size: Int,
    val folderName: String = "",
    val mediaType: MediaType,
    val isFavorite: Boolean,
    val hasFavorite: Boolean = false,
    val dateAdded: Long = 0,
    val path: String = "",
    val videoThumbnail: String = "",
)
