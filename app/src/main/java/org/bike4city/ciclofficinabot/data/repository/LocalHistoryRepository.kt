package org.bike4city.ciclofficinabot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bike4city.ciclofficinabot.data.local.*
import org.bike4city.ciclofficinabot.domain.model.*
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncInfo
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStatus
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStore

class LocalHistoryRepository(private val dao: ChatDao) : ReportSyncStore {
    fun observeSessions(): Flow<List<ChatSessionEntity>> = dao.observeSessions()
    override fun observeReport(sessionId: String): Flow<DiagnosisReport?> =
        dao.observeReport(sessionId).map { it?.toDomain() }

    override fun observeSyncInfo(sessionId: String): Flow<ReportSyncInfo> =
        dao.observeSession(sessionId).map { session ->
            ReportSyncInfo(
                status = session?.remoteSyncStatus
                    ?.let { runCatching { enumValueOf<ReportSyncStatus>(it) }.getOrNull() }
                    ?: ReportSyncStatus.LOCAL_ONLY,
                syncedAt = session?.remoteSyncedAt
            )
        }

    override suspend fun updateSyncInfo(sessionId: String, info: ReportSyncInfo) =
        dao.updateRemoteSync(sessionId, info.status.name, info.syncedAt)

    suspend fun ensureSession(id: String, startedAt: Long, bikeName: String = "La mia bici") = dao.insertSession(
        ChatSessionEntity(
            id = id,
            bikeName = bikeName.ifBlank { "La mia bici" },
            title = "Diagnosi in corso",
            category = DiagnosisCategory.OTHER.name,
            safetyLevel = SafetyLevel.SAFE.name,
            outcome = DiagnosisOutcome.UNDETERMINED.name,
            status = "OPEN",
            startedAt = startedAt,
            updatedAt = startedAt
        )
    )

    suspend fun saveMessage(sessionId: String, message: ChatMessage) = dao.insertMessage(
        ChatMessageEntity(
            id = message.id,
            sessionId = sessionId,
            role = message.role.name,
            text = message.text,
            messageType = message.type.name,
            safetyLevel = message.safetyLevel.name,
            timestamp = System.currentTimeMillis()
        )
    )

    suspend fun updateSession(
        sessionId: String,
        initialProblem: String,
        safetyLevel: SafetyLevel,
        outcome: DiagnosisOutcome,
        completed: Boolean
    ) = dao.updateSession(
        sessionId = sessionId,
        title = initialProblem.take(60).ifBlank { "Diagnosi in corso" },
        category = categoryFor(initialProblem).name,
        safetyLevel = safetyLevel.name,
        outcome = outcome.name,
        status = if (completed) "COMPLETED" else "OPEN",
        updatedAt = System.currentTimeMillis()
    )

    suspend fun delete(sessionId: String) = dao.deleteSession(sessionId)

    suspend fun saveReport(sessionId: String, report: DiagnosisReport) {
        dao.insertReport(
            DiagnosisReportEntity(
                sessionId = sessionId,
                bikeSummary = report.bikeSummary,
                initialProblem = report.initialProblem,
                checksPerformed = report.checksPerformed.joinToString(LIST_SEPARATOR),
                possibleCauses = report.possibleCauses.joinToString(LIST_SEPARATOR),
                safetyLevel = report.safetyLevel.name,
                outcome = report.outcome.name,
                usageRecommendation = report.usageRecommendation,
                generatedAt = System.currentTimeMillis()
            )
        )
        updateSyncInfo(sessionId, ReportSyncInfo())
    }

    private fun categoryFor(text: String): DiagnosisCategory {
        val value = text.lowercase()
        return when {
            "gomma" in value || "foratur" in value -> DiagnosisCategory.TIRES
            "ruota" in value -> DiagnosisCategory.WHEELS
            "freno" in value -> DiagnosisCategory.BRAKES
            "catena" in value || "cambio" in value -> DiagnosisCategory.TRANSMISSION
            "sterzo" in value -> DiagnosisCategory.STEERING
            "batteria" in value || "e-bike" in value -> DiagnosisCategory.EBIKE
            else -> DiagnosisCategory.OTHER
        }
    }

    private fun DiagnosisReportEntity.toDomain() = DiagnosisReport(
        bikeSummary = bikeSummary,
        initialProblem = initialProblem,
        checksPerformed = checksPerformed.split(LIST_SEPARATOR).filter(String::isNotBlank),
        possibleCauses = possibleCauses.split(LIST_SEPARATOR).filter(String::isNotBlank),
        safetyLevel = enumValueOf(safetyLevel),
        outcome = enumValueOf(outcome),
        usageRecommendation = usageRecommendation
    )

    private companion object { const val LIST_SEPARATOR = "\u001F" }
}
