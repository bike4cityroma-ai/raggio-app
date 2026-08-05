package org.bike4city.ciclofficinabot.domain.safety

import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import java.text.Normalizer

data class SafetyAssessment(
    val level: SafetyLevel,
    val outcome: DiagnosisOutcome,
    val reason: String? = null
)

/** Regole locali prudenziali: un risultato STOP prevale sempre su backend e procedure. */
class BikeSafetyEngine(
    private val stopRules: List<StopRule> = defaultStopRules
) {
    fun assess(text: String, remoteLevel: SafetyLevel = SafetyLevel.SAFE): SafetyAssessment {
        val normalized = text.normalized()
        val match = stopRules.firstOrNull { rule -> rule.phrases.any(normalized::contains) }
        if (match != null) return SafetyAssessment(SafetyLevel.STOP, DiagnosisOutcome.RED, match.reason)
        return when (remoteLevel) {
            SafetyLevel.STOP -> SafetyAssessment(SafetyLevel.STOP, DiagnosisOutcome.RED, "Il controllo remoto richiede l'arresto")
            SafetyLevel.CAUTION -> SafetyAssessment(SafetyLevel.CAUTION, DiagnosisOutcome.YELLOW)
            SafetyLevel.SAFE -> SafetyAssessment(SafetyLevel.SAFE, DiagnosisOutcome.UNDETERMINED)
        }
    }
}

data class StopRule(val reason: String, val phrases: Set<String>)

private fun String.normalized(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace(Regex("[^a-z0-9 ]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private val defaultStopRules = listOf(
    StopRule("La frenata potrebbe essere compromessa", setOf("leva arriva fino al manubrio", "leva tocca il manubrio", "freno non frena", "frenata assente", "cavo freno spezzato", "perdita freno idraulico")),
    StopRule("La batteria potrebbe essere danneggiata", setOf("batteria gonfia", "batteria e gonfia", "batteria si e gonfiata", "batteria surriscaldata", "odore dalla batteria", "involucro batteria lesionato", "cavi elettrici scoperti")),
    StopRule("Telaio o forcella potrebbero essere danneggiati", setOf("crepa sulla forcella", "crepa sul telaio", "forcella deformata")),
    StopRule("Lo pneumatico non è sicuro", setOf("copertone mostra i fili", "tela visibile", "pneumatico tagliato")),
    StopRule("La ruota potrebbe non essere fissata", setOf("ruota davanti si muove", "ruota non fissata", "forte gioco del mozzo", "piu raggi rotti", "ruota gravemente deformata")),
    StopRule("Lo sterzo potrebbe non essere sicuro", setOf("sterzo allentato", "manubrio si muove indipendentemente")),
    StopRule("La pedivella potrebbe staccarsi", setOf("pedivella molto allentata"))
)
