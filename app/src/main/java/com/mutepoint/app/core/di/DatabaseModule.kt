package com.mutepoint.app.core.di

import android.content.Context
import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.mutepoint.app.core.security.SecurePassphraseProvider
import com.mutepoint.app.data.local.MutePointDatabase
import com.mutepoint.app.data.local.dao.AnalyticsDao
import com.mutepoint.app.data.local.dao.CardsDao
import com.mutepoint.app.data.local.dao.JournalDao
import com.mutepoint.app.data.local.dao.RealityAnchorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override suspend fun migrate(db: SQLiteConnection) {
            // No-op: schema changes between v1 and v2 are additive and handled
            // by Room's auto-migration. This migration exists to prevent
            // destructive fallback on existing installs.
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MutePointDatabase {
        val passphrase = SecurePassphraseProvider.getOrCreatePassphrase(context)
        return Room.databaseBuilder(
            context,
            MutePointDatabase::class.java,
            "mute_point_secure.db",
        )
            .setDriver(SQLCipherDriver(passphrase, null, null))
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideAnalyticsDao(database: MutePointDatabase): AnalyticsDao = database.analyticsDao()

    @Provides
    fun provideJournalDao(database: MutePointDatabase): JournalDao = database.journalDao()

    @Provides
    fun provideCardsDao(database: MutePointDatabase): CardsDao = database.cardsDao()

    @Provides
    fun provideRealityAnchorDao(database: MutePointDatabase): RealityAnchorDao =
        database.realityAnchorDao()
}
