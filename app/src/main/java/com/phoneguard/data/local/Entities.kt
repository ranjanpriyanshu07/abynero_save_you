package com.phoneguard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One saved scan. Enum values are stored by name. */
@Entity(tableName = "scan_history", indices = [Index("timestamp")])
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val score: Int,
    @ColumnInfo(name = "apps_score") val appsScore: Int,
    @ColumnInfo(name = "device_score") val deviceScore: Int,
    @ColumnInfo(name = "network_score") val networkScore: Int,
    @ColumnInfo(name = "links_score") val linksScore: Int,
    @ColumnInfo(name = "apps_scanned") val appsScanned: Int,
    @ColumnInfo(name = "high_risk_apps") val highRiskApps: Int,
    @ColumnInfo(name = "medium_risk_apps") val mediumRiskApps: Int,
    @ColumnInfo(name = "device_issues") val deviceIssues: Int,
    @ColumnInfo(name = "scan_trigger") val trigger: String,
)

/**
 * One link check. Only the host name is kept (never the full link or message) so the
 * history reveals as little as possible.
 */
@Entity(tableName = "link_checks", indices = [Index("timestamp")])
data class LinkCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val host: String?,
    val level: String,
)
