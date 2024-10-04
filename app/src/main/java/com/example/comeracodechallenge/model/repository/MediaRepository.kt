package com.example.comeracodechallenge.model.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.annotation.ChecksSdkIntAtLeast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.example.comeracodechallenge.model.entities.LocalMedia
import com.example.comeracodechallenge.model.entities.VideoThumbnail
import com.example.comeracodechallenge.utils.MediaType
import com.example.comeracodechallenge.utils.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MediaRepository(private val context: Context) {
    private val _mediaFlow: MutableStateFlow<List<LocalMedia>> = MutableStateFlow(emptyList())
    val mediaFlow: StateFlow<List<LocalMedia>> = _mediaFlow

    private val _videoThumbnailFlow: MutableSharedFlow<VideoThumbnail?> = MutableSharedFlow(1)
    val videoThumbnailFlow: SharedFlow<VideoThumbnail?> = _videoThumbnailFlow

    private val _loadingMediaFiles: MutableStateFlow<Status> = MutableStateFlow(Status.Unknown)
    val loadingMediaFiles: StateFlow<Status> = _loadingMediaFiles

    suspend fun getAllMediaFilesOnDevice() =
        withContext(Dispatchers.IO) {
            val storageMediaList: MutableList<LocalMedia> = ArrayList()

            try {
                _loadingMediaFiles.emit(Status.Loading)
                val columns = chooseColumns(isOSGreaterThen10())

                val cursorImages = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    columns,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED
                )

                val cursorVideos = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    columns,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED
                )



                cursorImages?.moveToLast()
                cursorVideos?.moveToLast()

                getDataFromCursor(cursorImages!!, storageMediaList, MediaType.Image, context)
                getDataFromCursor(cursorVideos!!, storageMediaList, MediaType.Video, context)

            } catch (e: Exception) {
                _loadingMediaFiles.emit(Status.Failur)
            }
        }

    private suspend fun getDataFromCursor(
        cursor: Cursor,
        storageMediaList: MutableList<LocalMedia>,
        mediaType: MediaType,
        context: Context
    ) = withContext(Dispatchers.IO) {
        while (!cursor.isBeforeFirst) {
            var path: String
            var id: Long
            var folderName: String

            val pathColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val idColumnIndex = cursor.getColumnIndex(BaseColumns._ID)
            val folderNameColumnIndex =
                cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            if (pathColumnIndex != -1) {
                path = cursor.getString(pathColumnIndex)
            } else {
                continue
            }

            if (idColumnIndex != -1) {
                id = cursor.getLong(idColumnIndex)
            } else {
                continue
            }

            if (folderNameColumnIndex != -1) {
                folderName = cursor.getString(folderNameColumnIndex)
            } else {
                continue
            }

            val mediaUri = getMediaUri(mediaType, id)
            val mediaName = path.substring(path.lastIndexOf("/") + 1)
            val mediaSize = (File(path).length() / 1024).toInt()
            val isFavorite = getFavorite(cursor, isOSGreaterThen10())
            val duration = getDuration(cursor, isOSGreaterThen10())

            storageMediaList.add(
                LocalMedia(
                    id = id,
                    uri = mediaUri,
                    name = mediaName,
                    duration = duration,
                    size = mediaSize,
                    folderName = folderName,
                    mediaType = mediaType,
                    isFavorite = isFavorite,
                    path = path,
                    videoThumbnail = ""
                )
            )
            cursor.moveToPrevious()
        }

        _loadingMediaFiles.emit(Status.Success)
        _mediaFlow.emit(storageMediaList)

        storageMediaList
            .filter { it.mediaType == MediaType.Video && it.uri != null }
            .map {
                CoroutineScope(Dispatchers.Default).launch  {
                    generateThumbnail(it.id, it.uri!!, mediaType, context)
                }
            }

    }

    private fun getMediaUri(mediaType: MediaType, id: Long): Uri {
        if (mediaType == MediaType.Video) {
            return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        } else {
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    }

    private fun getFavorite(cursor: Cursor, isOsGreaterThen10: Boolean): Boolean {
        var isFavorite = false
        if (isOsGreaterThen10) {
            val favoriteColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.IS_FAVORITE)

            if (favoriteColumnIndex != -1) {
                val favoriteStr =
                    cursor.getString(favoriteColumnIndex)
                isFavorite = favoriteStr == "1"
            }
        }
        return isFavorite
    }

    private fun getDuration(cursor: Cursor, isOsGreaterThen10: Boolean): Int? {
        val duration: Int?
        if (isOsGreaterThen10) {
            val durationColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            if (durationColumnIndex != -1) {
                duration = cursor.getInt(durationColumnIndex)
            } else {
                duration = null
            }
        } else {
            duration = null
        }
        return duration
    }

    private fun chooseColumns(isOsGreaterThen10: Boolean): Array<String> {
        if (isOsGreaterThen10) {
            val columns = arrayOf(
                BaseColumns._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.IS_FAVORITE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            return columns
        } else {
            val columns = arrayOf(
                BaseColumns._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            return columns
        }
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    private fun isOSGreaterThen10(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    private suspend fun generateThumbnail(
        id: Long,
        uri: Uri,
        mediaType: MediaType,
        context: Context
    ) = withContext(Dispatchers.Default) {
        if (mediaType == MediaType.Video) {
            val inputPath = FFmpegKitConfig.getSafParameterForRead(context, uri)
            val parentDir = context.cacheDir.path + "/" + "thumbnails/"
            val parentDirFile = File(parentDir)
            if (!parentDirFile.exists()) {
                parentDirFile.mkdirs()
            }
            val outputPath = parentDir + "video_thumb_${id}.jpg"
            val outputPathFile = File(outputPath)

            if (outputPathFile.exists()) {
                val videoThumbnail = VideoThumbnail(id, outputPath)
                _videoThumbnailFlow.emit(videoThumbnail)
                return@withContext
            }

            val command = "-i $inputPath -vframes 1 -vf scale=210:-1 $outputPath"
            val session = FFmpegKit.execute(command)
            val duration = session.duration.toDuration(DurationUnit.MILLISECONDS)

            val videoThumbnail = VideoThumbnail(id, outputPath)
            _videoThumbnailFlow.emit(videoThumbnail)

        } else {
            return@withContext
        }
    }


}