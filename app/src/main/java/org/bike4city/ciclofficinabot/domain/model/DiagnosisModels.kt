package org.bike4city.ciclofficinabot.domain.model

enum class SafetyLevel { SAFE, CAUTION, STOP }
enum class DiagnosisOutcome { GREEN, YELLOW, RED, UNDETERMINED }
enum class DiagnosisCategory { TIRES, WHEELS, BRAKES, TRANSMISSION, STEERING, FRAME, EBIKE, OTHER }

data class BikeProfile(
    val id: String = "primary-bike",
    val name: String = "La mia bici",
    val bikeType: String = "City bike",
    val isElectric: Boolean = false,
    val brand: String = "",
    val model: String = "",
    val brakeType: String = "Non lo so"
)

data class DiagnosisReport(
    val bikeSummary: String,
    val initialProblem: String,
    val checksPerformed: List<String>,
    val possibleCauses: List<String>,
    val safetyLevel: SafetyLevel,
    val outcome: DiagnosisOutcome,
    val usageRecommendation: String
)
