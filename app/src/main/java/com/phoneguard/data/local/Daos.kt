package com.phoneguard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Access to saved scans. */
@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<ScanHistoryEntity?>

    @Query("DELETE FROM scan_history WHERE id NOT IN (SELECT id FROM scan_history ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM scan_history")
    suspend fun clear()
}

/** Access to link-check records. */
@Dao
interface LinkCheckDao {
    @Insert
    suspend fun insert(entity: LinkCheckEntity)

    @Query("SELECT COUNT(*) FROM link_checks WHERE timestamp >= :since")
    fun observeCheckedSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM link_checks WHERE timestamp >= :since AND level = 'HIGH'")
    fun observeDangerousSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM link_checks WHERE timestamp >= :since")
    suspend fun checkedSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM link_checks WHERE timestamp >= :since AND level = 'HIGH'")
    suspend fun dangerousSince(since: Long): Int

    @Query("DELETE FROM link_checks WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
