package org.bike4city.ciclofficinabot.domain.report

import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisReportFactoryTest {
    @Test fun `un problema alla catena produce un report coerente`() {
        val report = DiagnosisReportFactory.create(
            initialProblem = "La catena è caduta",
            safetyLevel = SafetyLevel.CAUTION,
            outcome = DiagnosisOutcome.YELLOW,
            instructionTitle = "Riposizionamento della catena"
        )

        assertEquals("La catena è caduta", report.initialProblem)
        assertTrue(report.checksPerformed.first().contains("Riposizionamento"))
        assertTrue(report.possibleCauses.first().contains("trasmissione"))
    }

    @Test fun `un arresto produce una raccomandazione di non utilizzo`() {
        val report = DiagnosisReportFactory.create(
            initialProblem = "La batteria è gonfia",
            safetyLevel = SafetyLevel.STOP,
            outcome = DiagnosisOutcome.RED
        )

        assertEquals(DiagnosisOutcome.RED, report.outcome)
        assertTrue(report.usageRecommendation.startsWith("Non utilizzare"))
    }
}
