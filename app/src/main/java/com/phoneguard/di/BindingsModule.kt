package com.phoneguard.di

import com.phoneguard.data.preferences.DataStorePreferencesRepository
import com.phoneguard.data.repository.DefaultScanRepository
import com.phoneguard.data.repository.RoomHistoryRepository
import com.phoneguard.domain.repository.HistoryRepository
import com.phoneguard.domain.repository.PreferencesRepository
import com.phoneguard.domain.repository.ScanRepository
import com.phoneguard.scanner.AndroidAppScanner
import com.phoneguard.scanner.AndroidDeviceSettingsScanner
import com.phoneguard.scanner.AndroidWifiScanner
import com.phoneguard.scanner.AppScanner
import com.phoneguard.scanner.DefaultLinkScanner
import com.phoneguard.scanner.DeviceSettingsScanner
import com.phoneguard.scanner.HttpSafeBrowsingClient
import com.phoneguard.scanner.LinkScanner
import com.phoneguard.scanner.SafeBrowsingClient
import com.phoneguard.scanner.WifiScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds interfaces to their implementations so each can be swapped in tests. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindAppScanner(impl: AndroidAppScanner): AppScanner

    @Binds
    abstract fun bindDeviceSettingsScanner(impl: AndroidDeviceSettingsScanner): DeviceSettingsScanner

    @Binds
    abstract fun bindWifiScanner(impl: AndroidWifiScanner): WifiScanner

    @Binds
    abstract fun bindSafeBrowsingClient(impl: HttpSafeBrowsingClient): SafeBrowsingClient

    @Binds
    abstract fun bindLinkScanner(impl: DefaultLinkScanner): LinkScanner

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: DefaultScanRepository): ScanRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository
}
