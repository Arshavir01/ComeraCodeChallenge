package com.example.comeracodechallenge.model.repository

import com.example.comeracodechallenge.model.entities.Folder
import com.example.comeracodechallenge.model.entities.LocalMedia
import com.example.comeracodechallenge.utils.Status
import kotlinx.coroutines.flow.SharedFlow

interface MediaRepository {
    val loadingStatus: SharedFlow<Status>

    suspend fun getAllMedia(): List<LocalMedia>

    fun getAllMediaWithFolders(): List<Folder>

    suspend fun getAllFolders()

    fun getAllMediaFromFolders(): List<LocalMedia>

    fun getMediaForFolderId(folderId: Int): List<LocalMedia>
}
