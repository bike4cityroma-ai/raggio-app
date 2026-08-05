package org.bike4city.ciclofficinabot.data.firebase

import org.bike4city.ciclofficinabot.domain.model.ChatMessageType
import org.bike4city.ciclofficinabot.domain.model.DiagnosisCategory
import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseChatResponseMapperTest {
    @Test
    fun `converte una risposta strutturata valida`() {
        val result = FirebaseChatResponseMapper.from(mapOf(
            "assistantMessage" to "Controlla la ruota a bici ferma.",
            "messageType" to "INSTRUCTION",
            "safetyLevel" to "CAUTION",
            "outcome" to "YELLOW",
            "category" to "WHEELS",
            "quickReplies" to listOf("Fatto", "Non riesco"),
            "instruction" to mapOf(
                "title" to "Controllo ruota",
                "body" to "Verifica che la ruota giri liberamente.",
                "warnings" to listOf("Non avvicinare le dita alle parti in movimento.")
            ),
            "conversationCompleted" to false
        ))

        assertEquals(ChatMessageType.INSTRUCTION, result.reply.type)
        assertEquals(SafetyLevel.CAUTION, result.reply.safetyLevel)
        assertEquals(DiagnosisOutcome.YELLOW, result.reply.outcome)
        assertEquals(DiagnosisCategory.WHEELS, result.category)
        assertEquals("Controllo ruota", result.reply.instructionTitle)
        assertEquals("Verifica che la ruota giri liberamente.", result.reply.instructionBody)
        assertEquals(
            listOf("Non avvicinare le dita alle parti in movimento."),
            result.reply.instructionWarnings
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `rifiuta una risposta priva del messaggio`() {
        FirebaseChatResponseMapper.from(mapOf("conversationCompleted" to false))
    }
}
