package com.example.comeracodechallenge.model.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.database.MergeCursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.annotation.ChecksSdkIntAtLeast
import com.example.comeracodechallenge.model.entities.Folder
import com.example.comeracodechallenge.model.entities.LocalMedia
import com.example.comeracodechallenge.utils.AppConstants.ALL_TITLE
import com.example.comeracodechallenge.utils.AppConstants.FAVORITE_ID
import com.example.comeracodechallenge.utils.AppConstants.FAVORITE_TITLE
import com.example.comeracodechallenge.utils.AppConstants.FOLDER_ID_ALL
import com.example.comeracodechallenge.utils.MediaType
import com.example.comeracodechallenge.utils.Status
import com.example.comeracodechallenge.utils.UtilMethods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly

class MediaRepository(private val context: Context) {
    private val mediaList = mutableListOf<LocalMedia>()
    private var mediaFolders: MutableList<Folder>? = null

    private val _loadingStatus: MutableStateFlow<Status> = MutableStateFlow(Status.Unknown)
    val loadingStatus: SharedFlow<Status> = _loadingStatus

    private val mutex = Mutex(false)

    private suspend fun getAllMedia(): List<LocalMedia> =
        withContext(Dispatchers.IO) {
            val storageMediaList: MutableList<LocalMedia> = ArrayList()

            mutex.withLock {
                if (mediaList.isNotEmpty()) {
                    return@withContext mediaList.distinctBy { it.id }
                }

                try {
                    _loadingStatus.emit(Status.Loading)
                    val columns = getColumns()

                    val cursorImages =
                        context.contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            columns,
                            null,
                            null,
                            MediaStore.Images.Media.DATE_ADDED,
                        )

                    val cursorVideos =
                        context.contentResolver.query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            columns,
                            null,
                            null,
                            MediaStore.Video.Media.DATE_ADDED,
                        )

                    val mergeCursor =
                        MergeCursor(
                            arrayOf(
                                cursorImages,
                                cursorVideos,
                            ),
                        )

                    mergeCursor.moveToLast()
                    val mediaList = getDataFromCursor(mergeCursor)
                    mergeCursor.closeQuietly()

                    storageMediaList.addAll(mediaList)

                    this@MediaRepository.mediaList.addAll(
                        storageMediaList.distinctBy { it.id }.sortedByDescending { it.dateAdded },
                    )
                    _loadingStatus.emit(Status.Success)
                } catch (e: Exception) {
                    _loadingStatus.emit(Status.Error(e.message ?: "Error"))
                }
            }

            return@withContext mediaList.distinctBy { it.id }
        }

    private fun getColumns(): Array<String> {
        val list =
            buildList {
                add(BaseColumns._ID)
                add(MediaStore.MediaColumns.DATA)
                add(MediaStore.MediaColumns.DATE_ADDED)
                add(MediaStore.MediaColumns.BUCKET_ID)
                add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                add(MediaStore.MediaColumns.DISPLAY_NAME)
                add(MediaStore.MediaColumns.SIZE)
                add(MediaStore.MediaColumns.MIME_TYPE)
                add(MediaStore.MediaColumns.DATE_ADDED)
                if (isSdkAtLeastR()) {
                    add(MediaStore.MediaColumns.IS_FAVORITE)
                    add(MediaStore.MediaColumns.DURATION)
                }
            }
        return list.toTypedArray()
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    private fun isSdkAtLeastR(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    fun getAllMediaWithFolders(): List<Folder> {
        return mediaFolders ?: emptyList()
    }

    private suspend fun getDataFromCursor(
        cursor: Cursor,
    ): List<LocalMedia> =
        withContext(Dispatchers.IO) {
            val storageMediaList: MutableList<LocalMedia> = ArrayList()
            val pathColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val idColumnIndex = cursor.getColumnIndex(BaseColumns._ID)
            val folderNameColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val fileNameColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val fileSizeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            var favoriteColumnIndex = -1
            var durationColumnIndex = -1
            if (isSdkAtLeastR()) {
                favoriteColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                durationColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            }
            val mimeTypeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val dateAddedColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

            while (!cursor.isBeforeFirst) {
                if (
                    pathColumnIndex == -1 ||
                    idColumnIndex == -1 ||
                    folderNameColumnIndex == -1 ||
                    fileNameColumnIndex == -1 ||
                    fileSizeColumnIndex == -1 ||
                    mimeTypeColumnIndex == -1
                ) {
                    cursor.moveToPrevious()
                    continue
                }

                val path = cursor.getString(pathColumnIndex)
                val id: Long = cursor.getLong(idColumnIndex)
                val folderName: String = cursor.getString(folderNameColumnIndex)
                val mediaName = cursor.getString(fileNameColumnIndex)
                val mediaSize = cursor.getInt(fileSizeColumnIndex)
                var isFavorite = false
                if (favoriteColumnIndex != -1) {
                    isFavorite = isFavorite(cursor, favoriteColumnIndex)
                }
                var duration: Int? = null
                if (durationColumnIndex != -1) {
                    duration = getDuration(cursor, durationColumnIndex)
                }
                val dateAdded = cursor.getLong(dateAddedColumnIndex)

                val mediaType =
                    if (UtilMethods.isVideoFile(path)) {
                        MediaType.Video
                    } else if (UtilMethods.isImageFile(path)) {
                        MediaType.Image
                    } else {
                        cursor.moveToPrevious()
                        continue
                    }

                val mediaUri = getMediaUriFromId(mediaType, id, path)

                storageMediaList.add(
                    LocalMedia(
                        id = id,
                        uri = mediaUri,
                        name = mediaName,
                        duration = duration,
                        size = mediaSize,
                        folderName = folderName,
                        mediaType = mediaType,
                        hasFavorite = isFavorite,
                        path = path,
                        dateAdded = dateAdded,
                        videoThumbnail = ""
                    ),
                )
                cursor.moveToPrevious()
            }

            return@withContext storageMediaList
        }

    private fun getMediaUriFromId(mediaType: MediaType, id: Long, path: String): Uri {
        return when (mediaType) {
            MediaType.Video -> {
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }
            MediaType.Image -> {
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
    }

    private fun isFavorite(
        cursor: Cursor,
        columnIndex: Int,
    ): Boolean {
        return cursor.getInt(columnIndex) == 1
    }

    private fun getDuration(
        cursor: Cursor,
        columnIndex: Int,
    ): Int {
        return cursor.getInt(columnIndex)
    }

    suspend fun getAllFolders() {
        val mList = getAllMedia()

        val foldersNames = mList.map { it.folderName }.distinct()

        val listFolderData: MutableList<Folder> = ArrayList()
        val firstFolder = createFirstFolder(mList)
        val favoriteFolder = createFavoriteFolder(mList)
        if (firstFolder != null) {
            listFolderData.add(firstFolder)
        }
        if (favoriteFolder != null) {
            listFolderData.add(favoriteFolder)
        }

        for (i in foldersNames.indices) {
            var folderItemCount = 0
            val newList: MutableList<LocalMedia> = ArrayList()

            for (j in 0 until mList.size) {
                if (mList[j].folderName == foldersNames[i]) {
                    folderItemCount++
                    newList.add(mList[j])
                }
            }

            val item = Folder(i, foldersNames[i], folderItemCount, newList)
            listFolderData.add(item)
        }

        if (mediaFolders == null) {
            mediaFolders = mutableListOf()
            mediaFolders!!.addAll(listFolderData)
        }
    }

    fun getAllMediaFromFolders(): List<LocalMedia> {
        val allMedia = mediaFolders?.flatMap { it.folderItemList }
        return allMedia?.distinct() ?: emptyList()
    }

    fun getMediaForFolderId(folderId: Int): List<LocalMedia> {
        val selectedFolderItems = getAllMediaWithFolders().firstOrNull { it.id == folderId }
        return selectedFolderItems?.folderItemList ?: emptyList()
    }

    private fun createFirstFolder(mList: List<LocalMedia>): Folder? {
        return if (mList.isNotEmpty()) {
            Folder(FOLDER_ID_ALL, ALL_TITLE, mList.size, mList)
        } else {
            null
        }
    }

    private fun createFavoriteFolder(mList: List<LocalMedia>): Folder? {
        val favorite = mList.filter { it.hasFavorite }
        return if (favorite.isNotEmpty()) {
            Folder(
                FAVORITE_ID,
                FAVORITE_TITLE,
                favorite.size,
                favorite,
            )
        } else {
            null
        }
    }

}




























