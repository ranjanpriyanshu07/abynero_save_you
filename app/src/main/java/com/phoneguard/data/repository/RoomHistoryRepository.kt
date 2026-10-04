package com.phoneguard.data.repository

import com.phoneguard.data.local.LinkCheckDao
import com.phoneguard.data.local.LinkCheckEntity
import com.phoneguard.data.local.ScanHistoryDao
import com.phoneguard.data.local.ScanHistoryEntity
import com.phoneguard.domain.model.LinkStats
import com.phoneguard.domain.model.RiskLevel
import com.phoneguard.domain.model.ScanHistoryEntry
import com.phoneguard.domain.model.ScanResult
import com.phoneguard.domain.model.ScanTrigger
import com.phoneguard.domain.model.ScoreCategory
import com.phoneguard.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** [HistoryRepository] backed by Room. */
@Singleton
class RoomHistoryRepository @Inject constructor(
    private val scanDao: ScanHistoryDao,
    private val linkDao: LinkCheckDao,
    private val clock: Clock,
) : HistoryRepository {

    override fun observeScans(limit: Int): Flow<List<ScanHistoryEntry>> =
        scanDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeLatestScan(): Flow<ScanHistoryEntry?> = scanDao.observeLatest().map { it?.toDomain() }

    override suspend fun saveScan(result: ScanResult, trigger: ScanTrigger) {
        val score = result.score
        scanDao.insert(
            ScanHistoryEntity(
                timestamp = result.timestamp,
                score = score.overall,
                appsScore = score.scoreOf(ScoreCategory.APPS),
                deviceScore = score.scoreOf(ScoreCategory.DEVICE),
                networkScore = score.scoreOf(ScoreCategory.NETWORK),
                linksScore = score.scoreOf(ScoreCategory.LINKS),
                appsScanned = result.apps.size,
                highRiskApps = result.highRiskAppCount,
                mediumRiskApps = result.mediumRiskAppCount,
                deviceIssues = result.deviceIssueCount,
                trigger = trigger.name,
            ),
        )
        scanDao.trimTo(MAX_SCANS)
        linkDao.deleteOlderThan(clock.millis() - TimeUnit.DAYS.toMillis(LINK_RETENTION_DAYS))
    }

    override suspend fun clearScans() = scanDao.clear()

    override suspend fun recordLinkCheck(host: String?, level: RiskLevel) {
        linkDao.insert(LinkCheckEntity(timestamp = clock.millis(), host = host, level = level.name))
    }

    override fun observeLinkStats(sinceMillis: Long): Flow<LinkStats> =
        combine(linkDao.observeCheckedSince(sinceMillis), linkDao.observeDangerousSince(sinceMillis)) { checked, bad ->
            LinkStats(checked = checked, dangerous = bad)
        }

    override suspend fun linkStatsSince(sinceMillis: Long): LinkStats =
        LinkStats(checked = linkDao.checkedSince(sinceMillis), dangerous = linkDao.dangerousSince(sinceMillis))

    private fun ScanHistoryEntity.toDomain() = ScanHistoryEntry(
        id = id,
        timestamp = timestamp,
        score = score,
        appsScore = appsScore,
        deviceScore = deviceScore,
        networkScore = networkScore,
        linksScore = linksScore,
        appsScanned = appsScanned,
        highRiskApps = highRiskApps,
        mediumRiskApps = mediumRiskApps,
        deviceIssues = deviceIssues,
        trigger = ScanTrigger.entries.firstOrNull { it.name == trigger } ?: ScanTrigger.MANUAL,
    )

    private companion object {
        const val MAX_SCANS = 200
        const val LINK_RETENTION_DAYS = 30L
    }
}
