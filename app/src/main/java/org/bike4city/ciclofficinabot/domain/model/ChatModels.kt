package org.bike4city.ciclofficinabot.domain.model

enum class ChatRole { USER, ASSISTANT }
enum class ChatMessageType { QUESTION, INSTRUCTION, WARNING, SUMMARY }

data class ChatMessage(val id: Long, val role: ChatRole, val text: String, val type: ChatMessageType = ChatMessageType.QUESTION, val safetyLevel: SafetyLevel = SafetyLevel.SAFE)

data class ChatPhoto(val jpegBytes: ByteArray, val width: Int, val height: Int) {
    init {
        require(jpegBytes.isNotEmpty() && jpegBytes.size <= 1_250_000)
        require(width in 1..1_280 && height in 1..1_280)
    }
}

data class ChatReply(
    val message: String,
    val type: ChatMessageType,
    val safetyLevel: SafetyLevel,
    val outcome: DiagnosisOutcome = DiagnosisOutcome.UNDETERMINED,
    val quickReplies: List<String> = emptyList(),
    val instructionTitle: String? = null,
    val instructionBody: String? = null,
    val instructionWarnings: List<String> = emptyList(),
    val completed: Boolean = false,
    val serviceUnavailable: Boolean = false
)
