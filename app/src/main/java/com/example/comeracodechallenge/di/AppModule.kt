package com.example.comeracodechallenge.di

import com.example.comeracodechallenge.model.repository.MediaRepository
import com.example.comeracodechallenge.model.repository.MediaRepositoryImpl
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module{
    viewModel {
        MediaViewModel(repo = get(), application = get())
    }
}

val repositoryModule = module {
    single<MediaRepository> {
        MediaRepositoryImpl(context = get())
    }
}
