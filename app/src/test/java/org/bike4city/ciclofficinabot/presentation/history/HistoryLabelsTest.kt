package org.bike4city.ciclofficinabot.presentation.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryLabelsTest {
    @Test
    fun `traduce tutti i valori mostrati nella cronologia`() {
        assertEquals("catena e cambio", categoryLabel("TRANSMISSION"))
        assertEquals("e-bike", categoryLabel("EBIKE"))
        assertEquals("ruote", categoryLabel("WHEELS"))
        assertEquals("Completata", statusLabel("COMPLETED"))
        assertEquals("attenzione", safetyLabel("CAUTION"))
        assertEquals("stop", safetyLabel("STOP"))
        assertEquals("giallo", outcomeLabel("YELLOW"))
        assertEquals("rosso", outcomeLabel("RED"))
    }

    @Test
    fun `non espone codici sconosciuti all'utente`() {
        assertEquals("categoria non disponibile", categoryLabel("UNKNOWN"))
        assertEquals("Stato non disponibile", statusLabel("UNKNOWN"))
        assertEquals("non disponibile", safetyLabel("UNKNOWN"))
        assertEquals("non disponibile", outcomeLabel("UNKNOWN"))
    }
}
