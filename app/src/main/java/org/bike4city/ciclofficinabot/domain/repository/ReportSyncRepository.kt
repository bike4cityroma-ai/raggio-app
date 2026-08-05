package org.bike4city.ciclofficinabot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.bike4city.ciclofficinabot.domain.model.DiagnosisReport

enum class ReportSyncStatus { LOCAL_ONLY, SYNCED, FAILED }

data class ReportSyncInfo(
    val status: ReportSyncStatus = ReportSyncStatus.LOCAL_ONLY,
    val syncedAt: Long? = null
)

interface ReportSyncStore {
    fun observeReport(sessionId: String): Flow<DiagnosisReport?>
    fun observeSyncInfo(sessionId: String): Flow<ReportSyncInfo>
    suspend fun updateSyncInfo(sessionId: String, info: ReportSyncInfo)
}

interface RemoteReportRepository {
    val isConfigured: Boolean
    suspend fun saveReport(sessionId: String, report: DiagnosisReport): Result<Unit>
    suspend fun deleteReport(sessionId: String): Result<Unit>
}

object DisabledRemoteReportRepository : RemoteReportRepository {
    override val isConfigured: Boolean = false

    override suspend fun saveReport(sessionId: String, report: DiagnosisReport): Result<Unit> =
        Result.failure(IllegalStateException("Firebase non configurato"))

    override suspend fun deleteReport(sessionId: String): Result<Unit> =
        Result.failure(IllegalStateException("Firebase non configurato"))
}
