package com.mutepoint.app.core.di

import com.mutepoint.app.core.audio.VadConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    fun provideVadConfig(): VadConfig = VadConfig()
}
