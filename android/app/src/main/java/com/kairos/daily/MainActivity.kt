package com.kairos.daily

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.kairos.daily.ui.KairosApp
import com.kairos.daily.ui.theme.KairosTheme
import com.kairos.daily.reminders.ReminderReceiver
import com.kairos.daily.focus.FocusCompleteReceiver
import com.kairos.daily.focus.FocusTimerRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var requestedFocusTaskId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedFocusTaskId = intent.getLongExtra(EXTRA_FOCUS_TASK_ID, -1).takeIf { it >= 0 }
        ReminderReceiver.createChannels(this)
        FocusCompleteReceiver.createChannels(this)
        FocusTimerRepository(this).restoreAfterSystemChange()
        enableEdgeToEdge()
        setContent {
            KairosTheme { KairosApp(requestedFocusTaskId = requestedFocusTaskId) }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedFocusTaskId = intent.getLongExtra(EXTRA_FOCUS_TASK_ID, -1).takeIf { it >= 0 }
    }

    companion object {
        const val EXTRA_FOCUS_TASK_ID = "focus_task_id"
    }
}
