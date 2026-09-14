package fi.xamk.aiscreentime.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fi.xamk.aiscreentime.MainActivity
import fi.xamk.aiscreentime.R
import java.util.Calendar

class ScreenTimeNotificationHelper(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Limit Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when daily screen time target is exceeded"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifyLimitExceeded(totalMinutes: Int, limitMinutes: Int, force: Boolean = false) {
        if (totalMinutes < limitMinutes) return
        if (!hasNotificationPermission()) return

        // Prevent repeated alerts for the same day unless forced
        val todayKey = getTodayDateKey()
        val lastNotifiedDate = prefs.getString(KEY_LAST_NOTIFIED_DATE, null)
        val lastNotifiedLimit = prefs.getInt(KEY_LAST_NOTIFIED_LIMIT, -1)

        if (!force && todayKey == lastNotifiedDate && limitMinutes == lastNotifiedLimit) {
            // Already alerted today for this limit
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalFormatted = formatMinutes(totalMinutes)
        val limitFormatted = formatMinutes(limitMinutes)
        val diffMinutes = totalMinutes - limitMinutes
        val diffFormatted = formatMinutes(diffMinutes)

        val title = "Daily Limit Exceeded!"
        val contentText = "You have used your device for $totalFormatted (Target: $limitFormatted). $diffFormatted over limit."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$contentText\n\nAI Insight: High screen time can impact focus and wellbeing. Consider taking a break or activating bedtime mode.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            prefs.edit()
                .putString(KEY_LAST_NOTIFIED_DATE, todayKey)
                .putInt(KEY_LAST_NOTIFIED_LIMIT, limitMinutes)
                .apply()
        }
    }

    fun showTestNotification() {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Screen Time Alert Active")
            .setContentText("Notifications are working properly! You will be alerted when limits are crossed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun getTodayDateKey(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    companion object {
        const val CHANNEL_ID = "screen_time_alerts"
        const val NOTIFICATION_ID = 1001
        const val TEST_NOTIFICATION_ID = 1002
        private const val KEY_LAST_NOTIFIED_DATE = "last_notified_date"
        private const val KEY_LAST_NOTIFIED_LIMIT = "last_notified_limit"
    }
}
