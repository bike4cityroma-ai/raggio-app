package org.bike4city.ciclofficinabot.domain.report

import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.DiagnosisReport
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel

object DiagnosisReportFactory {
    fun create(
        initialProblem: String,
        safetyLevel: SafetyLevel,
        outcome: DiagnosisOutcome,
        instructionTitle: String? = null,
        bikeSummary: String = "City bike"
    ): DiagnosisReport {
        val problem = initialProblem.ifBlank { "Procedura interrotta dall'utente" }
        val normalized = problem.lowercase()
        val possibleCause = when {
            "gomma" in normalized || "foratur" in normalized -> "Pressione insufficiente, foratura o copertone danneggiato"
            "catena" in normalized || "cambio" in normalized -> "Catena fuori sede o trasmissione da regolare"
            "freno" in normalized -> "Impianto frenante da controllare direttamente"
            "batteria" in normalized || "e-bike" in normalized -> "Batteria o collegamento elettrico da verificare"
            else -> "Causa non determinata con sufficiente sicurezza"
        }
        val recommendation = when (safetyLevel) {
            SafetyLevel.STOP -> "Non utilizzare né riparare la bici: rivolgiti a personale qualificato."
            SafetyLevel.CAUTION -> "Usa la bici soltanto dopo un controllo finale di freni, ruote e sterzo."
            SafetyLevel.SAFE -> "Esegui comunque un controllo visivo prima di usare la bici."
        }
        return DiagnosisReport(
            bikeSummary = bikeSummary,
            initialProblem = problem,
            checksPerformed = listOfNotNull(instructionTitle ?: "Valutazione guidata dei sintomi descritti"),
            possibleCauses = listOf(possibleCause),
            safetyLevel = safetyLevel,
            outcome = outcome,
            usageRecommendation = recommendation
        )
    }
}
