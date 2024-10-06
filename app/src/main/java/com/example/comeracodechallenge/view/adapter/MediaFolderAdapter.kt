package com.example.comeracodechallenge.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.comeracodechallenge.databinding.ListItemFolderBinding
import com.example.comeracodechallenge.model.entities.Folder

class MediaFolderAdapter(private val itemClickListener: (Int) -> Unit): ListAdapter<Folder, MediaFolderAdapter.MediaFolderViewHolder>(FolderItemDiffer()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MediaFolderViewHolder {
        val binding =
            ListItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaFolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaFolderViewHolder, position: Int) {
        val item = getItem(position)
        val list = item.folderItemList
        if (list.isNotEmpty()){
            Glide.with(holder.itemView.context)
                .load(item.folderItemList[0].uri)
                .into(holder.binding.folder)
        }
        holder.binding.folderName.text = item.name
        holder.binding.itemCount.text = item.count.toString()
    }

    inner class MediaFolderViewHolder(val binding: ListItemFolderBinding): RecyclerView.ViewHolder(binding.root) {
         init {
             itemView.setOnClickListener {
                 itemClickListener.invoke(getItem(adapterPosition).id)
             }
         }
    }

    class FolderItemDiffer : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }
}