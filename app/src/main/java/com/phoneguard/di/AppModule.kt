package com.phoneguard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.phoneguard.BuildConfig
import com.phoneguard.data.local.LinkCheckDao
import com.phoneguard.data.local.PhoneGuardDatabase
import com.phoneguard.data.local.ScanHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

/** App-wide singletons: dispatchers, clock, database and DataStore. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @SafeBrowsingApiKey
    fun provideSafeBrowsingApiKey(): String = BuildConfig.SAFE_BROWSING_API_KEY

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhoneGuardDatabase =
        Room.databaseBuilder(context, PhoneGuardDatabase::class.java, PhoneGuardDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideScanHistoryDao(database: PhoneGuardDatabase): ScanHistoryDao = database.scanHistoryDao()

    @Provides
    fun provideLinkCheckDao(database: PhoneGuardDatabase): LinkCheckDao = database.linkCheckDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("phoneguard_prefs") }
}
