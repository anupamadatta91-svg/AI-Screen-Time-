package fi.xamk.aiscreentime.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UsageRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("screen_time_prefs", Context.MODE_PRIVATE)

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsage(timeRange: TimeRange): List<AppUsage> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val end = System.currentTimeMillis()
        val start = when (timeRange) {
            TimeRange.TODAY -> {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            TimeRange.LAST_24_HOURS -> {
                end - TimeUnit.DAYS.toMillis(1)
            }
        }

        // Query aggregated stats first; fallback to queryUsageStats if empty
        val aggregatedMap: Map<String, Long> = queryAggregatedForegroundTime(manager, start, end)

        val pm = context.packageManager
        val selfPackage = context.packageName

        val rawList = aggregatedMap
            .filter { (pkg, millis) ->
                millis >= 60_000L && pkg != selfPackage
            }
            .map { (pkg, millis) ->
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millis).toInt()
                val (appName, icon) = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val iconDrawable = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    label to iconDrawable
                } catch (_: Exception) {
                    pkg to null
                }
                AppUsage(
                    packageName = pkg,
                    appName = appName,
                    minutes = minutes,
                    icon = icon
                )
            }
            .sortedByDescending { it.minutes }

        val totalMinutes = rawList.sumOf { it.minutes }

        return rawList.map { app ->
            app.copy(
                percentageOfTotal = if (totalMinutes > 0) app.minutes.toFloat() / totalMinutes else 0f
            )
        }
    }

    private fun queryAggregatedForegroundTime(
        manager: UsageStatsManager,
        start: Long,
        end: Long
    ): Map<String, Long> {
        val aggregated = manager.queryAndAggregateUsageStats(start, end)
        if (!aggregated.isNullOrEmpty()) {
            return aggregated.mapValues { it.value.totalTimeInForeground }
        }

        // Fallback for devices where queryAndAggregateUsageStats returns empty
        val statsList = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        if (statsList.isNullOrEmpty()) return emptyMap()

        val resultMap = mutableMapOf<String, Long>()
        for (stat in statsList) {
            if (stat.totalTimeInForeground > 0L) {
                resultMap[stat.packageName] =
                    (resultMap[stat.packageName] ?: 0L) + stat.totalTimeInForeground
            }
        }
        return resultMap
    }

    fun getDailyLimitMinutes(): Int {
        return prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
    }

    fun saveDailyLimitMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_LIMIT, minutes).apply()
    }

    companion object {
        private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
        const val DEFAULT_DAILY_LIMIT = 120
    }
}
