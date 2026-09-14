package fi.xamk.aiscreentime.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fi.xamk.aiscreentime.data.TimeRange
import fi.xamk.aiscreentime.data.UsageRepository
import java.util.concurrent.TimeUnit

class LimitCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = UsageRepository(appContext)
        if (!repository.hasUsagePermission()) {
            return Result.success()
        }

        val limitMinutes = repository.getDailyLimitMinutes()
        val apps = repository.getUsage(TimeRange.TODAY)
        val totalMinutes = apps.sumOf { it.minutes }

        if (totalMinutes >= limitMinutes) {
            val notificationHelper = ScreenTimeNotificationHelper(appContext)
            notificationHelper.checkAndNotifyLimitExceeded(totalMinutes, limitMinutes)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "PeriodicScreenTimeLimitCheck"

        fun enqueuePeriodicCheck(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<LimitCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
