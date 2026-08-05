package org.bike4city.ciclofficinabot.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSession(sessionId: String): Flow<ChatSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET title = :title, category = :category, safetyLevel = :safetyLevel, outcome = :outcome, status = :status, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSession(
        sessionId: String,
        title: String,
        category: String,
        safetyLevel: String,
        outcome: String,
        status: String,
        updatedAt: Long
    )

    @Query("UPDATE chat_sessions SET remoteSyncStatus = :status, remoteSyncedAt = :syncedAt WHERE id = :sessionId")
    suspend fun updateRemoteSync(sessionId: String, status: String, syncedAt: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DiagnosisReportEntity)

    @Query("SELECT * FROM diagnosis_reports WHERE sessionId = :sessionId LIMIT 1")
    fun observeReport(sessionId: String): Flow<DiagnosisReportEntity?>

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
