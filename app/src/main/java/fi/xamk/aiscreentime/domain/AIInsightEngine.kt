package fi.xamk.aiscreentime.domain

import fi.xamk.aiscreentime.data.AppUsage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AIInsightEngine {

    fun analyze(apps: List<AppUsage>, dailyLimitMinutes: Int): AIReport {
        val total = apps.sumOf { it.minutes }
        val topApp = apps.firstOrNull()
        val topMinutes = topApp?.minutes ?: 0
        val topShare = if (total > 0) topMinutes.toDouble() / total else 0.0

        // 1. Screen Time Pressure (Max 40 points)
        val safeLimit = max(1, dailyLimitMinutes)
        val rawTimeFactor = (total.toDouble() / safeLimit) * 40.0
        val timePressureScore = min(40, rawTimeFactor.roundToInt())

        // 2. Single-App Concentration (Max 35 points)
        // Scaled if total usage is minimal (< 30 mins) to prevent false alerts for brief usage
        val engagementWeight = if (total < 30) (total.toDouble() / 30.0) else 1.0
        val rawConcentration = topShare * 35.0 * engagementWeight
        val concentrationScore = min(35, rawConcentration.roundToInt())

        // 3. High-Use Apps Spread (Max 25 points)
        // Apps used for 60 minutes or longer
        val highUseApps = apps.count { it.minutes >= 60 }
        val appFactor = min(25, highUseApps * 8)
        val fragmentationScore = appFactor

        // Total Risk Score (0 - 100)
        val score = min(100, timePressureScore + concentrationScore + fragmentationScore)

        val level = when {
            score < 35 -> RiskLevel.HEALTHY
            score < 65 -> RiskLevel.MODERATE
            else -> RiskLevel.HIGH
        }

        val summary = when {
            total == 0 -> "No usage data is available yet. Ensure Usage Access is granted and refresh."
            total > dailyLimitMinutes ->
                "Your screen time exceeds your daily target by ${total - dailyLimitMinutes} min."
            total == dailyLimitMinutes ->
                "You have reached your daily limit of ${dailyLimitMinutes} min."
            else ->
                "You are within your daily limit with ${dailyLimitMinutes - total} min remaining."
        }

        val tips = buildList {
            if (total > dailyLimitMinutes) {
                add("Daily limit exceeded. Consider putting your device away for the rest of the day.")
            } else if (timePressureScore >= 35) {
                add("You have used over 85% of your daily limit. Pace your remaining screen time.")
            }

            if (topShare >= 0.45 && topMinutes >= 30 && topApp != null) {
                val sharePercent = (topShare * 100).roundToInt()
                add("${topApp.appName} accounts for $sharePercent% of your active screen time.")
            }

            if (highUseApps >= 3) {
                add("You have $highUseApps apps exceeding 1 hour each. Grouping app checks into scheduled focus blocks can reduce fatigue.")
            }

            if (isEmpty()) {
                add("Your screen time pattern is healthy and balanced. Keep monitoring your daily trend.")
            }
        }

        return AIReport(
            score = score,
            level = level,
            timePressureScore = timePressureScore,
            concentrationScore = concentrationScore,
            fragmentationScore = fragmentationScore,
            summary = summary,
            tips = tips,
            totalMinutes = total,
            dailyLimitMinutes = dailyLimitMinutes,
            topAppName = topApp?.appName,
            topAppMinutes = topMinutes,
            topAppSharePercent = (topShare * 100).roundToInt(),
            highUseAppCount = highUseApps
        )
    }
}
