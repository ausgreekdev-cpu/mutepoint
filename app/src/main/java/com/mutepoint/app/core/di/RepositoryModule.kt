package com.mutepoint.app.core.di

import com.mutepoint.app.data.repository.AnalyticsRepositoryImpl
import com.mutepoint.app.data.repository.ContactsRepositoryImpl
import com.mutepoint.app.data.repository.PracticeRepositoryImpl
import com.mutepoint.app.data.repository.RealityAnchorRepositoryImpl
import com.mutepoint.app.data.repository.RoomRepositoryImpl
import com.mutepoint.app.domain.repository.AnalyticsRepository
import com.mutepoint.app.domain.repository.ContactsRepository
import com.mutepoint.app.domain.repository.PracticeRepository
import com.mutepoint.app.domain.repository.RealityAnchorRepository
import com.mutepoint.app.domain.repository.RoomRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds
    abstract fun bindPracticeRepository(impl: PracticeRepositoryImpl): PracticeRepository

    @Binds
    abstract fun bindRoomRepository(impl: RoomRepositoryImpl): RoomRepository

    @Binds
    abstract fun bindRealityAnchorRepository(
        impl: RealityAnchorRepositoryImpl,
    ): RealityAnchorRepository

    @Binds
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository
}
