package com.example.comeracodechallenge.model.entities

data class Folder(
    val id: Int = -1,
    val name: String,
    val count: Int = 0,
    val folderItemList: List<LocalMedia> = emptyList(),
)
