package org.bike4city.ciclofficinabot.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatPhotoTest {
    @Test
    fun `accetta jpeg entro i limiti`() {
        val photo = ChatPhoto(ByteArray(1_250_000), 1_280, 960)
        assertEquals(1_250_000, photo.jpegBytes.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rifiuta file oltre il limite`() {
        ChatPhoto(ByteArray(1_250_001), 1_280, 960)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rifiuta dimensioni oltre il limite`() {
        ChatPhoto(ByteArray(10), 1_281, 960)
    }
}
