package com.kairos.daily.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.daily.data.Task
import com.kairos.daily.data.TaskRepository
import com.kairos.daily.data.nextOccurrence
import com.kairos.daily.data.PreferencesRepository
import com.kairos.daily.data.UserPreferences
import com.kairos.daily.reminders.ReminderScheduler
import com.kairos.daily.focus.FocusTimerRepository
import com.kairos.daily.focus.FocusTimerState
import com.kairos.daily.sync.LocalSync
import com.kairos.daily.sync.SyncReceiverService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KairosUiState(
    val tasks: List<Task> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val focus: FocusTimerState = FocusTimerState(),
    val focusRemainingMillis: Long = 0,
    val syncStatus: String = "Not connected",
    val syncEndpoint: String = "",
    val syncCode: String = ""
)

class KairosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)
    private val reminders = ReminderScheduler(application)
    private val preferences = PreferencesRepository(application)
    private val focus = FocusTimerRepository(application)
    private val localSync = LocalSync(application)

    private val _uiState = MutableStateFlow(KairosUiState(
        tasks = emptyList(),
        preferences = preferences.get(),
        focus = focus.get(),
        focusRemainingMillis = focus.get().remaining(),
        syncStatus = localSync.lastStatus,
        syncEndpoint = localSync.endpoint,
        syncCode = localSync.pairingCode
    ))
    val uiState: StateFlow<KairosUiState> = _uiState.asStateFlow()

    private val _focusRemainingMillis = MutableStateFlow(focus.get().remaining())
    val focusRemainingMillis: StateFlow<Long> = _focusRemainingMillis.asStateFlow()

    private var timerJob: Job? = null

    init {
        refresh()
        reconcileReminders()

        // Reactively update whenever tasks are mutated (local or synced)
        viewModelScope.launch {
            TaskRepository.dataEvents.collect {
                refresh()
            }
        }

        // Start countdown only if focus timer is actively running
        if (focus.get().running) {
            startTimerTicking()
        }
    }

    private fun startTimerTicking() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val current = focus.get()
                val remaining = current.remaining()
                _focusRemainingMillis.value = remaining
                if (!current.running || remaining <= 0) {
                    _uiState.value = _uiState.value.copy(focus = current, focusRemainingMillis = remaining)
                    break
                }
                delay(1_000)
            }
        }
    }

    fun saveTask(existing: Task?, title: String, notes: String, date: String?, reminderMinutes: Int?, priority: Int, repeatRule: String, estimatedPomodoros: Int) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (existing == null) {
                val task = Task(title = cleanTitle, notes = notes.trim(), scheduledDate = date, reminderMinutes = reminderMinutes, priority = priority, repeatRule = repeatRule, estimatedPomodoros = estimatedPomodoros)
                val id = repository.add(task)
                if (preferences.get().remindersOnThisDevice) reminders.schedule(task.copy(id = id))
            } else {
                val task = existing.copy(title = cleanTitle, notes = notes.trim(), scheduledDate = date, reminderMinutes = reminderMinutes, priority = priority, repeatRule = repeatRule, estimatedPomodoros = estimatedPomodoros)
                repository.update(task)
                if (preferences.get().remindersOnThisDevice) reminders.schedule(task) else reminders.cancel(task.id)
            }
            refresh()
        }
    }

    fun toggle(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val completed = !task.isCompleted
            repository.update(task.copy(isCompleted = completed, completedAt = if (completed) System.currentTimeMillis() else null))
            if (completed || !preferences.get().remindersOnThisDevice) reminders.cancel(task.id) else reminders.schedule(task)
            if (completed) createNextOccurrence(task)
            refresh()
        }
    }

    private fun createNextOccurrence(task: Task) {
        val occurrence = task.nextOccurrence() ?: return
        val id = repository.add(occurrence)
        if (preferences.get().remindersOnThisDevice) reminders.schedule(occurrence.copy(id = id))
    }

    fun delete(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(task.id)
            reminders.cancel(task.id)
            refresh()
        }
    }

    fun move(task: Task, direction: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.move(task, direction)
            refresh()
        }
    }

    fun setDefaultReminder(minutes: Int?) { preferences.setDefaultReminder(minutes); refresh() }
    fun setWeekStartsMonday(value: Boolean) { preferences.setWeekStartsMonday(value); refresh() }
    fun setSound(value: Boolean) { preferences.setSound(value); refresh() }
    fun setVibration(value: Boolean) { preferences.setVibration(value); refresh() }
    fun setQuotes(value: Boolean) { preferences.setQuotes(value); refresh() }
    fun setNotificationTone(value: String) { preferences.setNotificationTone(value); refresh() }
    fun setFollowUp(value: Boolean) { preferences.setFollowUp(value); refresh() }
    fun setFocusMinutes(value: Int) { preferences.setFocusMinutes(value); refresh() }
    fun setBreakMinutes(value: Int) { preferences.setBreakMinutes(value); refresh() }
    fun setRemindersOnThisDevice(value: Boolean) {
        preferences.setRemindersOnThisDevice(value)
        reconcileReminders()
        refresh()
    }

    fun startSyncReceiver() {
        ContextCompat.startForegroundService(getApplication(), Intent(getApplication(), SyncReceiverService::class.java))
        updateSyncStatus("Starting receiver at ${localSync.endpoint}…")
    }

    fun syncTo(endpoint: String, code: String) {
        updateSyncStatus("Connecting…")
        localSync.connect(endpoint, code) { result ->
            result.onSuccess {
                reconcileReminders()
                refreshKeepingSync("Synced with ${it.deviceName} · ${it.received} updates received")
            }.onFailure { updateSyncStatus("Sync failed: ${it.message}") }
        }
    }

    fun startFocus(taskId: Long) {
        focus.start(taskId, preferences.get().focusMinutes)
        _focusRemainingMillis.value = focus.get().remaining()
        refresh()
        startTimerTicking()
    }

    fun pauseFocus() {
        focus.pause()
        timerJob?.cancel()
        _focusRemainingMillis.value = focus.get().remaining()
        refresh()
    }

    fun resumeFocus() {
        focus.resume()
        _focusRemainingMillis.value = focus.get().remaining()
        refresh()
        startTimerTicking()
    }

    fun finishFocus() {
        timerJob?.cancel()
        val state = focus.get()
        if (!state.isBreak) state.taskId?.let(repository::completePomodoro)
        focus.clear()
        _focusRemainingMillis.value = 0
        refresh()
    }

    fun stopFocus() {
        timerJob?.cancel()
        focus.clear()
        _focusRemainingMillis.value = 0
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = repository.getTasks()
            val prefs = preferences.get()
            val currentFocus = focus.get()
            val status = localSync.lastStatus
            val prev = _uiState.value
            _uiState.value = KairosUiState(
                tasks = tasks,
                preferences = prefs,
                focus = currentFocus,
                focusRemainingMillis = currentFocus.remaining(),
                syncStatus = if (status != "Not connected") status else prev.syncStatus,
                syncEndpoint = localSync.endpoint,
                syncCode = localSync.pairingCode
            )
        }
    }

    private fun reconcileReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = preferences.get().remindersOnThisDevice
            repository.getTasks().forEach { if (enabled) reminders.schedule(it) else reminders.cancel(it.id) }
        }
    }

    private fun updateSyncStatus(status: String) { _uiState.value = _uiState.value.copy(syncStatus = status) }
    private fun refreshKeepingSync(status: String = _uiState.value.syncStatus) { refresh(); updateSyncStatus(status) }
}
