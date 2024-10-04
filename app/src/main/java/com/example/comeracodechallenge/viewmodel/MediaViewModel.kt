package com.example.comeracodechallenge.viewmodel

import androidx.lifecycle.ViewModel
import com.example.comeracodechallenge.model.repository.MediaRepository

class MediaViewModel(
    private val repository: MediaRepository
) : ViewModel() {
}