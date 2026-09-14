package fi.xamk.aiscreentime.domain

import fi.xamk.aiscreentime.data.AppUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AIInsightEngineTest {

    private lateinit var engine: AIInsightEngine

    @Before
    fun setUp() {
        engine = AIInsightEngine()
    }

    @Test
    fun `empty apps returns healthy status with zero score`() {
        val report = engine.analyze(emptyList(), dailyLimitMinutes = 120)

        assertEquals(0, report.score)
        assertEquals(RiskLevel.HEALTHY, report.level)
        assertEquals(0, report.totalMinutes)
        assertEquals(0, report.timePressureScore)
        assertEquals(0, report.concentrationScore)
        assertEquals(0, report.fragmentationScore)
        assertTrue(report.summary.contains("No usage data is available"))
    }

    @Test
    fun `brief usage does not unfairly penalize single app concentration`() {
        // User used WhatsApp for 3 minutes total
        val apps = listOf(
            AppUsage(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                minutes = 3,
                percentageOfTotal = 1f
            )
        )
        val report = engine.analyze(apps, dailyLimitMinutes = 120)

        // Concentration should be scaled down by (3/30 = 0.1) -> ~3-4 points max
        assertTrue("Concentration score should be low for brief usage", report.concentrationScore <= 5)
        assertEquals(RiskLevel.HEALTHY, report.level)
        assertTrue(report.score < 10)
    }

    @Test
    fun `exceeding daily limit yields maximum time pressure and high risk`() {
        val apps = listOf(
            AppUsage("com.google.android.youtube", "YouTube", 150),
            AppUsage("com.instagram.android", "Instagram", 90)
        )
        // Total = 240m, Limit = 120m
        val report = engine.analyze(apps, dailyLimitMinutes = 120)

        assertEquals(40, report.timePressureScore)
        assertEquals(RiskLevel.HIGH, report.level)
        assertTrue(report.summary.contains("exceeds your daily target by 120 min"))
        assertTrue(report.tips.any { it.contains("Daily limit exceeded") })
    }

    @Test
    fun `single app binge triggers high concentration warning`() {
        val apps = listOf(
            AppUsage("com.zhiliaoapp.musically", "TikTok", 150),
            AppUsage("com.spotify.music", "Spotify", 30)
        )
        // Total = 180m, TikTok has 150m (83.3%)
        val report = engine.analyze(apps, dailyLimitMinutes = 240)

        assertTrue("Concentration score should be high", report.concentrationScore >= 28)
        assertTrue(report.tips.any { it.contains("TikTok accounts for") })
    }

    @Test
    fun `multiple apps exceeding one hour increments fragmentation score`() {
        val apps = listOf(
            AppUsage("app1", "App One", 70),
            AppUsage("app2", "App Two", 65),
            AppUsage("app3", "App Three", 80),
            AppUsage("app4", "App Four", 60)
        )
        // 4 apps >= 60 min -> 4 * 8 = 32 -> capped at 25 max
        val report = engine.analyze(apps, dailyLimitMinutes = 480)

        assertEquals(4, report.highUseAppCount)
        assertEquals(25, report.fragmentationScore)
        assertTrue(report.tips.any { it.contains("4 apps exceeding 1 hour") })
    }

    @Test
    fun `total score is clamped at 100 under extreme usage`() {
        val apps = (1..10).map { i ->
            AppUsage("com.app.$i", "Heavy App $i", 120)
        }
        // Total = 1200m, limit = 60m
        val report = engine.analyze(apps, dailyLimitMinutes = 60)

        assertEquals(100, report.score)
        assertEquals(RiskLevel.HIGH, report.level)
    }
}
