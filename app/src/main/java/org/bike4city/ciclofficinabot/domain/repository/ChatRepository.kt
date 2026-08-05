package org.bike4city.ciclofficinabot.domain.repository

import org.bike4city.ciclofficinabot.domain.model.ChatReply
import org.bike4city.ciclofficinabot.domain.model.ChatPhoto

interface ChatRepository {
    suspend fun replyTo(sessionId: String, message: String, photo: ChatPhoto? = null): ChatReply
    fun reset()
}
