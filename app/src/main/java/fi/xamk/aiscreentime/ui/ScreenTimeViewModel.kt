package fi.xamk.aiscreentime.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.xamk.aiscreentime.data.AppUsage
import fi.xamk.aiscreentime.data.TimeRange
import fi.xamk.aiscreentime.data.UsageRepository
import fi.xamk.aiscreentime.domain.AIInsightEngine
import fi.xamk.aiscreentime.domain.AIReport
import fi.xamk.aiscreentime.notification.LimitCheckWorker
import fi.xamk.aiscreentime.notification.ScreenTimeNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScreenTimeUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val timeRange: TimeRange = TimeRange.TODAY,
    val apps: List<AppUsage> = emptyList(),
    val dailyLimitMinutes: Int = 120,
    val aiReport: AIReport? = null
)

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)
    private val aiEngine = AIInsightEngine()
    private val notificationHelper = ScreenTimeNotificationHelper(application)

    private val _uiState = MutableStateFlow(
        ScreenTimeUiState(
            dailyLimitMinutes = repository.getDailyLimitMinutes(),
            hasNotificationPermission = notificationHelper.hasNotificationPermission()
        )
    )
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    init {
        LimitCheckWorker.enqueuePeriodicCheck(application)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val hasPerm = repository.hasUsagePermission()
            val hasNotifPerm = notificationHelper.hasNotificationPermission()
            val currentLimit = repository.getDailyLimitMinutes()
            val currentTimeRange = _uiState.value.timeRange

            if (!hasPerm) {
                val report = aiEngine.analyze(emptyList(), currentLimit)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasPermission = false,
                        hasNotificationPermission = hasNotifPerm,
                        apps = emptyList(),
                        dailyLimitMinutes = currentLimit,
                        aiReport = report
                    )
                }
                return@launch
            }

            val apps = withContext(Dispatchers.IO) {
                repository.getUsage(currentTimeRange)
            }
            val report = aiEngine.analyze(apps, currentLimit)
            val totalMinutes = apps.sumOf { it.minutes }

            // Trigger notification alert if screen time exceeds limit
            if (currentTimeRange == TimeRange.TODAY && totalMinutes >= currentLimit) {
                notificationHelper.checkAndNotifyLimitExceeded(totalMinutes, currentLimit)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasPermission = true,
                    hasNotificationPermission = hasNotifPerm,
                    apps = apps,
                    dailyLimitMinutes = currentLimit,
                    aiReport = report
                )
            }
        }
    }

    fun setTimeRange(timeRange: TimeRange) {
        if (_uiState.value.timeRange == timeRange) return
        _uiState.update { it.copy(timeRange = timeRange) }
        refresh()
    }

    fun updateDailyLimit(minutes: Int) {
        repository.saveDailyLimitMinutes(minutes)
        val totalMinutes = _uiState.value.apps.sumOf { it.minutes }

        if (_uiState.value.timeRange == TimeRange.TODAY && totalMinutes >= minutes) {
            notificationHelper.checkAndNotifyLimitExceeded(totalMinutes, minutes, force = true)
        }

        _uiState.update { state ->
            val newReport = state.apps.let { aiEngine.analyze(it, minutes) }
            state.copy(
                dailyLimitMinutes = minutes,
                aiReport = newReport
            )
        }
    }

    fun sendTestNotification() {
        notificationHelper.showTestNotification()
    }

    fun checkNotificationPermission() {
        val hasNotifPerm = notificationHelper.hasNotificationPermission()
        _uiState.update { it.copy(hasNotificationPermission = hasNotifPerm) }
    }
}
