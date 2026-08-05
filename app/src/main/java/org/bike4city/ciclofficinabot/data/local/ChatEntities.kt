package org.bike4city.ciclofficinabot.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val bikeName: String,
    val title: String,
    val category: String,
    val safetyLevel: String,
    val outcome: String,
    val status: String,
    val startedAt: Long,
    val updatedAt: Long,
    val remoteSyncStatus: String = "LOCAL_ONLY",
    val remoteSyncedAt: Long? = null
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = ChatSessionEntity::class,
        parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: Long,
    val sessionId: String,
    val role: String,
    val text: String,
    val messageType: String,
    val safetyLevel: String,
    val timestamp: Long
)

@Entity(
    tableName = "diagnosis_reports",
    foreignKeys = [ForeignKey(
        entity = ChatSessionEntity::class,
        parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DiagnosisReportEntity(
    @PrimaryKey val sessionId: String,
    val bikeSummary: String,
    val initialProblem: String,
    val checksPerformed: String,
    val possibleCauses: String,
    val safetyLevel: String,
    val outcome: String,
    val usageRecommendation: String,
    val generatedAt: Long
)
