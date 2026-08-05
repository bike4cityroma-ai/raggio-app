package org.bike4city.ciclofficinabot.domain.safety

import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class BikeSafetyEngineTest {
    private val engine = BikeSafetyEngine()

    @Test fun `i cinque segnali critici richiesti producono STOP e RED`() {
        listOf(
            "La leva arriva fino al manubrio.",
            "La batteria è gonfia.",
            "Vedo una crepa sulla forcella.",
            "Il copertone mostra i fili.",
            "La ruota davanti si muove."
        ).forEach { phrase ->
            val result = engine.assess(phrase)
            assertEquals(phrase, SafetyLevel.STOP, result.level)
            assertEquals(phrase, DiagnosisOutcome.RED, result.outcome)
        }
    }

    @Test fun `freno che fischia e catena caduta non producono STOP automatico`() {
        listOf("Il freno fischia ma frena.", "La catena è caduta.").forEach {
            assertEquals(SafetyLevel.SAFE, engine.assess(it).level)
        }
    }

    @Test fun `la regola locale prevale sul risultato remoto`() {
        assertEquals(SafetyLevel.STOP, engine.assess("La batteria è gonfia", SafetyLevel.CAUTION).level)
    }
}
