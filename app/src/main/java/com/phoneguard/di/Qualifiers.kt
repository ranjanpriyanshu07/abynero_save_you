package com.phoneguard.di

import javax.inject.Qualifier

/** Dispatcher for disk and network work (all scans run here). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Coroutine scope that lives as long as the app process. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** The Google Safe Browsing API key from local.properties (may be empty). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SafeBrowsingApiKey
