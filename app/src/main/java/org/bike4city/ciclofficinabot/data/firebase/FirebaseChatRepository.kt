package org.bike4city.ciclofficinabot.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import org.bike4city.ciclofficinabot.domain.model.ChatMessageType
import org.bike4city.ciclofficinabot.domain.model.ChatPhoto
import org.bike4city.ciclofficinabot.domain.model.ChatReply
import org.bike4city.ciclofficinabot.domain.model.DiagnosisCategory
import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import org.bike4city.ciclofficinabot.domain.repository.ChatRepository
import org.bike4city.ciclofficinabot.domain.safety.BikeSafetyEngine
import java.util.UUID

class FirebaseChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west1"),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val safetyEngine: BikeSafetyEngine = BikeSafetyEngine()
) : ChatRepository {
    private var category: DiagnosisCategory? = null
    private var messageCount = 0
    private val history = ArrayDeque<Map<String, String>>()

    override suspend fun replyTo(sessionId: String, message: String, photo: ChatPhoto?): ChatReply {
        val local = safetyEngine.assess(message)
        if (local.level == SafetyLevel.STOP) return localStop(local.reason)

        return try {
            ensureAuthenticated()
            val imagePath = photo?.let { uploadPhoto(sessionId, it) }
            val data = mapOf(
                "sessionId" to sessionId,
                "message" to message,
                "category" to category?.name,
                "messageCount" to messageCount,
                "history" to history.toList(),
                "imagePath" to imagePath
            )
            val raw = try {
                callWithRetry(data)
            } catch (error: Exception) {
                if (imagePath != null) runCatching { storage.reference.child(imagePath).delete().await() }
                throw error
            }
            val remote = FirebaseChatResponseMapper.from(raw)
            category = remote.category
            messageCount++
            appendHistory("USER", message)
            appendHistory("ASSISTANT", remote.reply.message)
            val finalSafety = safetyEngine.assess(message, remote.reply.safetyLevel)
            if (finalSafety.level == SafetyLevel.STOP) {
                remote.reply.copy(
                    type = ChatMessageType.WARNING,
                    safetyLevel = SafetyLevel.STOP,
                    outcome = DiagnosisOutcome.RED,
                    completed = true,
                    quickReplies = emptyList()
                )
            } else {
                remote.reply
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            temporarilyUnavailable()
        }
    }

    override fun reset() {
        category = null
        messageCount = 0
        history.clear()
    }

    private suspend fun callWithRetry(data: Map<String, Any?>): Any? {
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                return functions.getHttpsCallable("bikeMechanicChat").call(data).await().data
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastError = error
                if (attempt == 0) delay(800)
            }
        }
        throw requireNotNull(lastError)
    }

    private suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    private fun appendHistory(role: String, text: String) {
        history.addLast(mapOf("role" to role, "text" to text.take(1_000)))
        while (history.size > 20) history.removeFirst()
    }

    private fun localStop(reason: String?) = ChatReply(
        message = "${reason ?: "Possibile pericolo rilevato"}. Non utilizzare la bicicletta e rivolgiti alla ciclofficina.",
        type = ChatMessageType.WARNING,
        safetyLevel = SafetyLevel.STOP,
        outcome = DiagnosisOutcome.RED,
        completed = true
    )

    private fun temporarilyUnavailable() = ChatReply(
        message = "L'assistente è temporaneamente non disponibile. Nessuna diagnosi è stata prodotta: attendi qualche secondo e riprova.",
        type = ChatMessageType.WARNING,
        safetyLevel = SafetyLevel.CAUTION,
        outcome = DiagnosisOutcome.UNDETERMINED,
        serviceUnavailable = true
    )

    private suspend fun uploadPhoto(sessionId: String, photo: ChatPhoto): String {
        val uid = requireNotNull(auth.currentUser?.uid)
        val path = "ciclofficinaBotUsers/$uid/sessions/$sessionId/photos/${UUID.randomUUID()}.jpg"
        val expiresAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1_000
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("expiresAtEpochMs", expiresAt.toString())
            .setCustomMetadata("width", photo.width.toString())
            .setCustomMetadata("height", photo.height.toString())
            .build()
        storage.reference.child(path).putBytes(photo.jpegBytes, metadata).await()
        return path
    }
}
