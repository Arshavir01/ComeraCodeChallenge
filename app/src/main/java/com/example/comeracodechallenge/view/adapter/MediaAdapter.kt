package com.example.comeracodechallenge.view.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.comeracodechallenge.databinding.ListItemLocalMediaBinding
import com.example.comeracodechallenge.model.entities.LocalMedia
import com.example.comeracodechallenge.utils.MediaType
import com.example.comeracodechallenge.utils.MediaUtils
import com.example.comeracodechallenge.utils.MediaUtils.dpAsPx
import kotlin.math.roundToInt

class MediaAdapter : ListAdapter<LocalMedia, MediaAdapter.MediaViewHolder>(MediaListItemDiffer()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding =
            ListItemLocalMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = getItem(position)

        val itemWidth = computeScreenSize()
        val preferredHeight = (((itemWidth * 3f) / 4f)).roundToInt()

        holder.binding.photo.updateLayoutParams<FrameLayout.LayoutParams> {
            width = itemWidth
            height = preferredHeight
        }

        if (item.mediaType == MediaType.Video) {
            val hasDuration = item.duration != null
            holder.binding.videoDuration.isVisible = hasDuration

            if (hasDuration) {
                 val duration = MediaUtils.convertSecondsToHour(item.duration!!.toInt())
                holder.binding.durationTextView.text = duration
            }
        } else {
            holder.binding.videoDuration.isVisible = false
        }
        holder.binding.photo.load(item.uri)
    }

    private fun computeScreenSize(): Int {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val itemWidth = screenWidth / 3.5f
        return itemWidth.roundToInt()
    }

    inner class MediaViewHolder(val binding: ListItemLocalMediaBinding): RecyclerView.ViewHolder(binding.root)

    class MediaListItemDiffer : DiffUtil.ItemCallback<LocalMedia>() {
        override fun areItemsTheSame(oldItem: LocalMedia, newItem: LocalMedia): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: LocalMedia, newItem: LocalMedia): Boolean {
            return oldItem == newItem
        }
    }
}