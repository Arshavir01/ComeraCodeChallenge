package com.example.comeracodechallenge.di

import com.example.comeracodechallenge.model.repository.MediaRepository
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val appModule = module{
    viewModel {
        MediaViewModel(repository = get())
    }

    single {
        MediaRepository(appContext = get())
    }
}
