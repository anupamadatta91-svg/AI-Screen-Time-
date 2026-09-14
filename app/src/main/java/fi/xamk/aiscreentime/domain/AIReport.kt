package fi.xamk.aiscreentime.domain

enum class RiskLevel(val label: String) {
    HEALTHY("Healthy"),
    MODERATE("Moderate"),
    HIGH("High")
}

data class AIReport(
    val score: Int,
    val level: RiskLevel,
    val timePressureScore: Int,
    val concentrationScore: Int,
    val fragmentationScore: Int,
    val summary: String,
    val tips: List<String>,
    val totalMinutes: Int = 0,
    val dailyLimitMinutes: Int = 120,
    val topAppName: String? = null,
    val topAppMinutes: Int = 0,
    val topAppSharePercent: Int = 0,
    val highUseAppCount: Int = 0
)
