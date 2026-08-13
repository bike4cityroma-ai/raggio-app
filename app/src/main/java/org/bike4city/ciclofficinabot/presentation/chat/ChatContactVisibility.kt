package org.bike4city.ciclofficinabot.presentation.chat

import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome

internal fun shouldShowWorkshopContact(completed: Boolean, outcome: DiagnosisOutcome): Boolean =
    completed && (outcome == DiagnosisOutcome.YELLOW || outcome == DiagnosisOutcome.RED)
