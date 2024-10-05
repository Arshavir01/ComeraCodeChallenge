package com.example.comeracodechallenge.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.example.comeracodechallenge.model.entities.LocalMedia
import com.example.comeracodechallenge.model.repository.MediaRepository
import com.example.comeracodechallenge.utils.AppConstants.NO_ID
import com.example.comeracodechallenge.utils.Filter
import com.example.comeracodechallenge.utils.MediaType
import com.example.comeracodechallenge.utils.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MediaViewModel(
    private val repo: MediaRepository,
    context: Context
) : ViewModel() {

    private val permissionGranted = MutableStateFlow(false)
    private val mediaFlow = MutableStateFlow<List<LocalMedia>>(emptyList())
    private val currentFilter = MutableStateFlow(Filter.All)
    private val thumbGenerationQueue = ArrayDeque<LocalMedia>()
    private var thumbnailGenerationJob: Job? = null

    val viewState =
        permissionGranted
            .filter { it }
            .flatMapConcat {
                return@flatMapConcat combine(
                    mediaFlow,
                    repo.loadingStatus,
                    currentFilter
                ) { media, status, filter ->
                    setAlreadyGeneratedThumbs(context, media)
                    return@combine ViewState(media, status, filter)
                }
            }

    init {
        permissionGranted
            .filter { it }
            .take(1)
            .onEach {
                repo.getAllFolders()
                val list = repo.getAllMediaFromFolders()
                mediaFlow.value = list
            }
            .launchIn(viewModelScope)
    }

    fun onPermissionGranted() {
        permissionGranted.value = true
    }

    private fun setAlreadyGeneratedThumbs(context: Context, list: List<LocalMedia>) {
        list.forEach {
            val outputPath = getOutputPathFromId(it.id, context)
            val outputPathFile = File(outputPath)
            if (outputPathFile.exists()) {
                addVideoThumbsToList(videoThumbnail = outputPath, id = it.id)
            }
        }
    }

    private fun addVideoThumbsToList(videoThumbnail: String, id: Long) {
        mediaFlow.update { oldList ->
            return@update copyListAfterThumbGeneration(oldList, id, videoThumbnail)
        }
    }

    private fun copyListAfterThumbGeneration(
        oldList: List<LocalMedia>,
        id: Long,
        videoThumbnail: String,
    ): List<LocalMedia> {
        val newMediaList = oldList.toMutableList()
        val index = newMediaList.indexOfFirst { it.id == id }
        if (index != NO_ID) {
            newMediaList[index] = newMediaList[index].copy(videoThumbnail = videoThumbnail)
        }
        return newMediaList
    }

    private fun getOutputPathFromId(id: Long, context: Context): String {
        val parentDir = getParentDir(context)
        return parentDir + "video_thumb_$id.jpg"
    }

    private fun getParentDir(context: Context): String {
        return context.cacheDir.path + "/" + "thumbnails/"
    }

    fun onScrollPositionChanged(firstVisiblePos: Int, lastVisiblePos: Int, context: Context) {
        val newList = filterMediaList(mediaFlow.value, currentFilter.value)
        for (i in lastVisiblePos downTo firstVisiblePos) {
            if (
                i < newList.size &&
                newList[i].mediaType == MediaType.Video &&
                !thumbGenerationQueue.contains(newList[i])
            ) {
                thumbGenerationQueue.addLast(newList[i])
            }
        }

        thumbnailGenerationJob?.cancel()
        thumbnailGenerationJob =
            viewModelScope.launch(Dispatchers.IO) {
                while (thumbGenerationQueue.isNotEmpty()) {
                    val item = thumbGenerationQueue.peekLast()!!
                    val thumbnail = generateThumbnail(context, item.id, item.uri!!)
                    addVideoThumbsToList(thumbnail, item.id)
                    thumbGenerationQueue.removeLast()
                }
            }
    }

    private fun filterMediaList(list: List<LocalMedia>, filter: Filter): List<LocalMedia> {
        val filteredList =
            when (filter) {
                Filter.All -> list
                Filter.Video -> filterVideos(list)
                Filter.Photo -> filterPhotos(list)
            }
        return filteredList
    }

    private fun filterPhotos(media: List<LocalMedia>): List<LocalMedia> {
        return media.filter { it.mediaType == MediaType.Image }
    }

    private fun filterVideos(media: List<LocalMedia>): List<LocalMedia> {
        return media.filter { it.mediaType == MediaType.Video }
    }

    private suspend fun generateThumbnail(
        context: Context,
        id: Long,
        uri: Uri,
    ): String =
        withContext(Dispatchers.IO) {
            val inputPath = FFmpegKitConfig.getSafParameterForRead(context, uri)
            val parentDir = getParentDir(context)
            val parentDirFile = File(parentDir)

            if (!parentDirFile.exists()) {
                parentDirFile.mkdirs()
            }
            val outputPath = getOutputPathFromId(id, context)
            val outputPathFile = File(outputPath)

            Log.d("TAG_MediaViewModel", "generateThumbnail: $outputPath")

            if (outputPathFile.exists()) {
                return@withContext outputPath
            }

            val command =
                "-i $inputPath -vf scale=210:210:'force_original_aspect_ratio=decrease' -vframes 1 $outputPath"
            val session = FFmpegKit.execute(command)
            val duration = session.duration.toDuration(DurationUnit.MILLISECONDS)

            return@withContext outputPath
        }

    fun onFilterChanged(filter: Filter) {
        currentFilter.value = filter
    }

    fun generateFirstPartOfVideoThumbs(lastVisiblePos: Int, context: Context) {
        val list = mediaFlow.value
        thumbnailGenerationJob?.cancel()
        thumbnailGenerationJob =
            viewModelScope.launch {
                list
                    .filter { it.mediaType == MediaType.Video }
                    .forEachIndexed { index, item ->
                        if (index <= lastVisiblePos) {
                            val thumbnail = generateThumbnail(context, item.id, item.uri!!)
                            addVideoThumbsToList(thumbnail, item.id)
                        } else {
                            return@launch
                        }
                    }
            }
    }
}

data class ViewState(
    val media: List<LocalMedia>,
    val loadingStatus: Status,
    val filter: Filter
)