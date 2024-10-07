package com.example.comeracodechallenge.di

import com.example.comeracodechallenge.model.repository.MediaRepository
import com.example.comeracodechallenge.viewmodel.MediaViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module{
    viewModel {
        MediaViewModel(repo = get(), application = get())
    }

    single {
        MediaRepository(context =  get())
    }
}
