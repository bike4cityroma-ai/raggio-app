package org.bike4city.ciclofficinabot.presentation.chat

import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContactVisibilityTest {
    @Test fun `mostra il contatto solo alla chiusura gialla o rossa`() {
        assertTrue(shouldShowWorkshopContact(true, DiagnosisOutcome.YELLOW))
        assertTrue(shouldShowWorkshopContact(true, DiagnosisOutcome.RED))
        assertFalse(shouldShowWorkshopContact(false, DiagnosisOutcome.YELLOW))
        assertFalse(shouldShowWorkshopContact(true, DiagnosisOutcome.GREEN))
        assertFalse(shouldShowWorkshopContact(true, DiagnosisOutcome.UNDETERMINED))
    }
}
