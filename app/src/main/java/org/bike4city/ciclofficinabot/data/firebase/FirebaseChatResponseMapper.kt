package org.bike4city.ciclofficinabot.data.firebase

import org.bike4city.ciclofficinabot.domain.model.ChatMessageType
import org.bike4city.ciclofficinabot.domain.model.ChatReply
import org.bike4city.ciclofficinabot.domain.model.DiagnosisCategory
import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel

data class RemoteChatResult(
    val reply: ChatReply,
    val category: DiagnosisCategory
)

object FirebaseChatResponseMapper {
    fun from(value: Any?): RemoteChatResult {
        val map = value as? Map<*, *> ?: error("Risposta Firebase non valida")
        val instruction = map["instruction"] as? Map<*, *>
        val reply = ChatReply(
            message = map.requiredString("assistantMessage", 1_500),
            type = enumValue(map.requiredString("messageType", 32)),
            safetyLevel = enumValue(map.requiredString("safetyLevel", 32)),
            outcome = enumValue(map.requiredString("outcome", 32)),
            quickReplies = map.stringList("quickReplies", 5, 100),
            instructionTitle = instruction?.optionalString("title", 120),
            instructionBody = instruction?.optionalString("body", 1_500),
            instructionWarnings = instruction?.stringList("warnings", 5, 300).orEmpty(),
            completed = map["conversationCompleted"] as? Boolean
                ?: error("conversationCompleted mancante")
        )
        return RemoteChatResult(
            reply = reply,
            category = enumValue(map.requiredString("category", 32))
        )
    }

    private fun Map<*, *>.requiredString(key: String, max: Int): String =
        ((this[key] as? String)?.trim()?.take(max))
            ?.takeIf(String::isNotEmpty) ?: error("$key mancante")

    private fun Map<*, *>.optionalString(key: String, max: Int): String? =
        (this[key] as? String)?.trim()?.take(max)?.takeIf(String::isNotEmpty)

    private fun Map<*, *>.stringList(key: String, maxItems: Int, maxLength: Int): List<String> =
        (this[key] as? List<*>)
            ?.mapNotNull { (it as? String)?.trim()?.take(maxLength)?.takeIf(String::isNotEmpty) }
            ?.take(maxItems)
            .orEmpty()

    private inline fun <reified T : Enum<T>> enumValue(value: String): T =
        enumValues<T>().firstOrNull { it.name == value } ?: error("Valore non previsto: $value")
}
