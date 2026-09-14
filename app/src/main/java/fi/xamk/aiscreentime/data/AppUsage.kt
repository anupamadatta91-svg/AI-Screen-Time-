package fi.xamk.aiscreentime.data

import android.graphics.drawable.Drawable

enum class TimeRange(val title: String) {
    TODAY("Today"),
    LAST_24_HOURS("Last 24h")
}

data class AppUsage(
    val packageName: String,
    val appName: String,
    val minutes: Int,
    val percentageOfTotal: Float = 0f,
    val icon: Drawable? = null
)
