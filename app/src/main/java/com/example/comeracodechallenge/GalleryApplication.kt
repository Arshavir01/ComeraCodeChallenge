package com.example.comeracodechallenge

import android.app.Application
import com.example.comeracodechallenge.di.repositoryModule
import com.example.comeracodechallenge.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class GalleryApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@GalleryApplication)
            modules(viewModelModule, repositoryModule)
        }
    }
}