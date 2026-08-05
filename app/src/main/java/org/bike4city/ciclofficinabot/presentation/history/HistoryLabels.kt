package org.bike4city.ciclofficinabot.presentation.history

internal fun categoryLabel(value: String): String = when (value) {
    "TIRES" -> "pneumatici"
    "WHEELS" -> "ruote"
    "BRAKES" -> "freni"
    "TRANSMISSION" -> "catena e cambio"
    "STEERING" -> "sterzo"
    "FRAME" -> "telaio"
    "EBIKE" -> "e-bike"
    "OTHER" -> "altro"
    else -> "categoria non disponibile"
}

internal fun statusLabel(value: String): String = when (value) {
    "OPEN" -> "In corso"
    "COMPLETED" -> "Completata"
    else -> "Stato non disponibile"
}

internal fun safetyLabel(value: String): String = when (value) {
    "SAFE" -> "normale"
    "CAUTION" -> "attenzione"
    "STOP" -> "stop"
    else -> "non disponibile"
}

internal fun outcomeLabel(value: String): String = when (value) {
    "GREEN" -> "verde"
    "YELLOW" -> "giallo"
    "RED" -> "rosso"
    "UNDETERMINED" -> "da determinare"
    else -> "non disponibile"
}
