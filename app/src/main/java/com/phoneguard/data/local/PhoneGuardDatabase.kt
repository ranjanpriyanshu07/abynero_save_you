package com.phoneguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** PhoneGuard's on-device database. Nothing in it is ever uploaded. */
@Database(
    entities = [ScanHistoryEntity::class, LinkCheckEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PhoneGuardDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun linkCheckDao(): LinkCheckDao

    companion object {
        const val NAME = "phoneguard.db"
    }
}
