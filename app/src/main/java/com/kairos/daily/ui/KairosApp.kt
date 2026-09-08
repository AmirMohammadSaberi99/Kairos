@file:OptIn(ExperimentalMaterial3Api::class)

package com.kairos.daily.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kairos.daily.data.Task
import com.kairos.daily.data.EisenhowerQuadrant
import com.kairos.daily.data.RepeatRule
import com.kairos.daily.data.UserPreferences
import com.kairos.daily.data.NotificationTone
import com.kairos.daily.data.TaskBackup
import com.kairos.daily.focus.FocusTimerState
import com.kairos.daily.ui.theme.KairosAccent
import com.kairos.daily.ui.theme.KairosBackground
import com.kairos.daily.ui.theme.KairosMint
import com.kairos.daily.ui.theme.KairosMuted
import com.kairos.daily.ui.theme.KairosSurface
import com.kairos.daily.ui.theme.KairosSurfaceHigh
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class Destination(val label: String, val mark: String) {
    Today("Today", "●"), Upcoming("Upcoming", "◇"), Focus("Focus", "◉"), History("History", "✓"), Settings("Settings", "⚙")
}

private enum class EditorOrigin { Today, Upcoming, Existing }

@Composable
fun KairosApp(requestedFocusTaskId: Long? = null, viewModel: KairosViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf(Destination.Today) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var editorOrigin by remember { mutableStateOf(EditorOrigin.Today) }
    var selectedFocusTaskId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(requestedFocusTaskId) {
        if (requestedFocusTaskId != null) {
            selectedFocusTaskId = requestedFocusTaskId
            destination = Destination.Focus
        }
    }

    BackHandler(enabled = editorVisible) {
        editorVisible = false
    }

    BackHandler(enabled = !editorVisible && destination != Destination.Today) {
        destination = Destination.Today
    }

    Scaffold(
        containerColor = KairosBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = { BottomNavigation(destination) { destination = it } },
        floatingActionButton = {
            if (destination == Destination.Today || destination == Destination.Upcoming) {
                Surface(
                    modifier = Modifier.size(58.dp).clickable {
                        editingTask = null
                        editorOrigin = if (destination == Destination.Today) EditorOrigin.Today else EditorOrigin.Upcoming
                        editorVisible = true
                    },
                    shape = RoundedCornerShape(19.dp),
                    color = KairosAccent,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (destination) {
                Destination.Today -> TodayScreen(state.tasks, state.preferences, viewModel::toggle, viewModel::move, onFocus = {
                    selectedFocusTaskId = it.id
                    destination = Destination.Focus
                }) {
                    editingTask = it
                    editorOrigin = EditorOrigin.Existing
                    editorVisible = true
                }
                Destination.Upcoming -> UpcomingScreen(state.tasks, viewModel::toggle, viewModel::move, onFocus = {
                    selectedFocusTaskId = it.id
                    destination = Destination.Focus
                }) {
                    editingTask = it
                    editorOrigin = EditorOrigin.Existing
                    editorVisible = true
                }
                Destination.History -> HistoryScreen(state.tasks, viewModel::toggle) {
                    editingTask = it
                    editorOrigin = EditorOrigin.Existing
                    editorVisible = true
                }
                Destination.Focus -> {
                    val focusRemainingMillis by viewModel.focusRemainingMillis.collectAsStateWithLifecycle()
                    FocusScreen(
                        tasks = state.tasks,
                        selectedTaskId = state.focus.taskId ?: selectedFocusTaskId,
                        timer = state.focus,
                        remainingMillis = focusRemainingMillis,
                        preferences = state.preferences,
                        onSelect = { selectedFocusTaskId = it.id },
                        onClearSelection = { selectedFocusTaskId = null },
                        onStart = viewModel::startFocus,
                        onPause = viewModel::pauseFocus,
                        onResume = viewModel::resumeFocus,
                        onFinish = viewModel::finishFocus,
                        onStop = viewModel::stopFocus
                    )
                }
                Destination.Settings -> SettingsScreen(
                    preferences = state.preferences,
                    tasks = state.tasks,
                    onDefaultReminder = viewModel::setDefaultReminder,
                    onWeekStart = viewModel::setWeekStartsMonday,
                    onSound = viewModel::setSound,
                    onVibration = viewModel::setVibration,
                    onQuotes = viewModel::setQuotes,
                    onTone = viewModel::setNotificationTone,
                    onFollowUp = viewModel::setFollowUp,
                    onFocusMinutes = viewModel::setFocusMinutes,
                    onBreakMinutes = viewModel::setBreakMinutes,
                    syncStatus = state.syncStatus,
                    syncEndpoint = state.syncEndpoint,
                    syncCode = state.syncCode,
                    onStartSyncReceiver = viewModel::startSyncReceiver,
                    onSyncTo = viewModel::syncTo,
                    onReminderOwner = viewModel::setRemindersOnThisDevice,
                    onRestoreComplete = viewModel::refresh
                )
            }
        }
    }

    if (editorVisible) {
        TaskEditorSheet(
            task = editingTask,
            origin = editorOrigin,
            onDismiss = { editorVisible = false },
            onSave = { title, notes, date, time, priority, repeat, pomodoros ->
                viewModel.saveTask(editingTask, title, notes, date, time, priority, repeat, pomodoros)
                editorVisible = false
            },
            onDelete = editingTask?.let { task ->
                {
                    viewModel.delete(task)
                    editorVisible = false
                }
            },
            defaultReminder = state.preferences.defaultReminderMinutes,
            weekStartsMonday = state.preferences.weekStartsMonday
        )
    }
}

@Composable
private fun TodayScreen(
    tasks: List<Task>,
    preferences: UserPreferences,
    onToggle: (Task) -> Unit,
    onMove: (Task, Int) -> Unit,
    onFocus: (Task) -> Unit,
    onEdit: (Task) -> Unit
) {
    val today = LocalDate.now()
    val relevant = tasksForToday(tasks, today)
    val active = relevant.filterNot { it.isCompleted }
    val done = relevant.filter { it.isCompleted }
    val total = relevant.size
    val progress = if (total == 0) 0f else done.size.toFloat() / total

    LazyColumn(
        modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = KairosAccent
            )
            Spacer(Modifier.height(7.dp))
            Text("Make today count.", style = MaterialTheme.typography.displaySmall)
            if (preferences.quotesEnabled) {
                val quote = DailyQuotes.forDate(today)
                Spacer(Modifier.height(12.dp))
                Text("“${quote.text}”", style = MaterialTheme.typography.bodyLarge, color = KairosMuted, modifier = Modifier.widthIn(max = 620.dp))
                Spacer(Modifier.height(3.dp))
                Text(quote.source, style = MaterialTheme.typography.labelMedium, color = Color(0xFF676B78))
            }
            Spacer(Modifier.height(22.dp))
            ProgressCard(done.size, total, progress)
            Spacer(Modifier.height(14.dp))
        }

        if (active.isEmpty()) {
            item { EmptyToday(hasCompleted = done.isNotEmpty()) }
        } else {
            EisenhowerQuadrant.entries.filter { quadrant -> active.any { it.priority == quadrant.value } }.forEach { quadrant ->
                item { SectionLabel(if (quadrant == EisenhowerQuadrant.NONE) "UNSORTED" else quadrant.label.uppercase() + " · " + quadrant.hint.uppercase()) }
                items(active.filter { it.priority == quadrant.value }, key = { it.id }) { task -> TaskCard(task, onToggle, onEdit, onMove, onFocus) }
            }
        }

        if (done.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel("COMPLETED · ${done.size}")
            }
            items(done, key = { it.id }) { task -> TaskCard(task, onToggle, onEdit) }
        }
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, progress: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KairosSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (total == 0) "A clear day" else "$done of $total complete", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (total == 0) "Add the first thing you want to accomplish."
                        else if (done == total) "Everything is done. Nicely handled."
                        else "One focused step at a time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KairosMuted
                    )
                }
                Text("${(progress * 100).toInt()}%", color = KairosMint, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(17.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = KairosMint,
                trackColor = KairosSurfaceHigh,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun EmptyToday(hasCompleted: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(KairosSurfaceHigh), contentAlignment = Alignment.Center) {
            Text(if (hasCompleted) "✓" else "+", color = if (hasCompleted) KairosMint else KairosAccent, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(15.dp))
        Text(if (hasCompleted) "Today is complete" else "Nothing planned yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(5.dp))
        Text(if (hasCompleted) "Enjoy the space you made." else "Tap + to add your first task.", color = KairosMuted)
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onMove: ((Task, Int) -> Unit)? = null,
    onFocus: ((Task) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { onEdit(task) },
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) KairosSurface.copy(alpha = .62f) else KairosSurface)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(25.dp).clickable { onToggle(task) },
                shape = CircleShape,
                color = if (task.isCompleted) KairosMint else Color.Transparent,
                border = if (task.isCompleted) null else androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF555A69))
            ) {
                if (task.isCompleted) Box(contentAlignment = Alignment.Center) { Text("✓", color = KairosBackground, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null),
                    color = if (task.isCompleted) KairosMuted else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = taskMeta(task)
                AnimatedVisibility(meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.bodyMedium, color = if (isOverdue(task)) MaterialTheme.colorScheme.error else KairosMuted, modifier = Modifier.padding(top = 4.dp))
                }
                if (task.priority != EisenhowerQuadrant.NONE.value && !task.isCompleted) {
                    val quadrant = EisenhowerQuadrant.from(task.priority)
                    Text(
                        quadrant.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = quadrantColor(quadrant),
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                if (task.estimatedPomodoros > 0) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "FOCUS ${task.completedPomodoros.coerceAtMost(task.estimatedPomodoros)}/${task.estimatedPomodoros}",
                            style = MaterialTheme.typography.labelMedium,
                            color = KairosMint
                        )
                        if (onFocus != null && !task.isCompleted) {
                            Spacer(Modifier.width(10.dp))
                            Text("START", style = MaterialTheme.typography.labelMedium, color = KairosAccent, modifier = Modifier.clickable { onFocus(task) }.padding(vertical = 3.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (onMove != null && !task.isCompleted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMove(task, -1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▲", color = KairosMuted, style = MaterialTheme.typography.labelMedium)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMove(task, 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▼", color = KairosMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Text("›", color = Color(0xFF5F6370), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun taskMeta(task: Task): String {
    val parts = mutableListOf<String>()
    task.scheduledDate?.let {
        val date = runCatching { LocalDate.parse(it) }.getOrNull()
        if (date != null) parts += when (date) {
            LocalDate.now() -> "Today"
            LocalDate.now().minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
    task.reminderMinutes?.let { parts += "%02d:%02d".format(it / 60, it % 60) }
    return parts.joinToString("  ·  ")
}

private fun isOverdue(task: Task): Boolean = !task.isCompleted && task.scheduledDate?.let {
    runCatching { LocalDate.parse(it) < LocalDate.now() }.getOrDefault(false)
} == true

private fun quadrantColor(quadrant: EisenhowerQuadrant): Color = when (quadrant) {
    EisenhowerQuadrant.DO_NOW -> Color(0xFFFF7D8B)
    EisenhowerQuadrant.SCHEDULE -> KairosAccent
    EisenhowerQuadrant.DELEGATE -> Color(0xFFFFC56B)
    EisenhowerQuadrant.ELIMINATE -> KairosMuted
    EisenhowerQuadrant.NONE -> KairosMuted
}

@Composable
private fun UpcomingScreen(tasks: List<Task>, onToggle: (Task) -> Unit, onMove: (Task, Int) -> Unit, onFocus: (Task) -> Unit, onEdit: (Task) -> Unit) {
    val future = tasksForUpcoming(tasks, LocalDate.now())
    val grouped = future.groupBy { it.scheduledDate!! }
    LazyColumn(
        modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("PLAN AHEAD", style = MaterialTheme.typography.labelMedium, color = KairosAccent)
            Spacer(Modifier.height(7.dp))
            Text("Upcoming", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("A calm view of what comes next.", color = KairosMuted)
            Spacer(Modifier.height(22.dp))
        }
        if (future.isEmpty()) item { EmptyUpcoming() }
        grouped.forEach { (dateString, dateTasks) ->
            item {
                val date = LocalDate.parse(dateString)
                SectionLabel(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)).uppercase())
            }
            items(dateTasks, key = { it.id }) { task -> TaskCard(task, onToggle, onEdit, onMove, onFocus) }
        }
    }
}

@Composable
private fun EmptyUpcoming() {
    Card(colors = CardDefaults.cardColors(containerColor = KairosSurface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text("The horizon is clear", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Future tasks will appear here, grouped by day.", color = KairosMuted)
        }
    }
}

@Composable
private fun FocusScreen(
    tasks: List<Task>,
    selectedTaskId: Long?,
    timer: FocusTimerState,
    remainingMillis: Long,
    preferences: UserPreferences,
    onSelect: (Task) -> Unit,
    onClearSelection: () -> Unit,
    onStart: (Long) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onStop: () -> Unit
) {
    val selected = tasks.firstOrNull { it.id == (timer.taskId ?: selectedTaskId) && !it.isCompleted }
    val available = tasks.filter { !it.isCompleted }
    LazyColumn(
        modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("DEEP WORK", style = MaterialTheme.typography.labelMedium, color = KairosAccent)
            Spacer(Modifier.height(7.dp))
            Text("Focus", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("One task. One session. Nothing else.", color = KairosMuted)
            Spacer(Modifier.height(22.dp))
        }
        if (selected == null) {
            item {
                Text("Choose a task", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("Assign Pomodoros in the task editor, or focus on any open task.", color = KairosMuted)
            }
            if (available.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = KairosSurface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp)) {
                            Text("Nothing needs your focus", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text("Your open tasks will appear here.", color = KairosMuted)
                        }
                    }
                }
            } else {
                items(available, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(task) },
                        colors = CardDefaults.cardColors(containerColor = KairosSurface),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (task.estimatedPomodoros > 0) "${task.completedPomodoros}/${task.estimatedPomodoros} sessions" else "No estimate",
                                    color = KairosMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text("Focus ›", color = KairosAccent, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = KairosSurface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (timer.isBreak) "Break time" else selected.title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        val estimate = selected.estimatedPomodoros
                        Text(
                            if (timer.isBreak) "REST AND RESET" else if (estimate > 0) "SESSION ${selected.completedPomodoros + 1} OF $estimate" else "FOCUS SESSION",
                            style = MaterialTheme.typography.labelMedium,
                            color = KairosMint
                        )
                        Spacer(Modifier.height(26.dp))
                        val displayMillis = if (timer.taskId != null) remainingMillis else preferences.focusMinutes * 60_000L
                        val totalSeconds = (displayMillis / 1_000).coerceAtLeast(0)
                        Text("%02d:%02d".format(totalSeconds / 60, totalSeconds % 60), fontSize = 68.sp, lineHeight = 74.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(if (timer.isBreak) "${preferences.breakMinutes} minute break in progress" else "${preferences.focusMinutes} min focus - ${preferences.breakMinutes} min break", color = KairosMuted, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(28.dp))
                        when {
                            timer.taskId == null -> Button(
                                onClick = { onStart(selected.id) },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KairosAccent)
                            ) { Text("Start focus", style = MaterialTheme.typography.labelLarge) }
                            timer.running -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onPause, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Pause") }
                                Button(onClick = onFinish, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = KairosMint, contentColor = KairosBackground)) { Text(if (timer.isBreak) "End break" else "Finish") }
                            }
                            else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onResume, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Resume") }
                                TextButton(onClick = onStop, modifier = Modifier.weight(1f).height(52.dp)) { Text("Abandon", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
            if (timer.taskId == null) {
                item { TextButton(onClick = onClearSelection, modifier = Modifier.fillMaxWidth()) { Text("Choose another task", color = KairosMuted) } }
            }
        }
    }
}

@Composable
private fun HistoryScreen(tasks: List<Task>, onToggle: (Task) -> Unit, onEdit: (Task) -> Unit) {
    val completed = tasks.filter { it.isCompleted }.sortedByDescending { it.completedAt ?: 0L }
    LazyColumn(
        modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("PROGRESS, KEPT", style = MaterialTheme.typography.labelMedium, color = KairosAccent)
            Spacer(Modifier.height(7.dp))
            Text("History", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("${completed.size} completed ${if (completed.size == 1) "task" else "tasks"}", color = KairosMuted)
            Spacer(Modifier.height(22.dp))
        }
        if (completed.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = KairosSurface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Your wins will collect here", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("Complete a task to start your history.", color = KairosMuted)
                    }
                }
            }
        } else {
            items(completed, key = { it.id }) { task -> TaskCard(task, onToggle, onEdit) }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: UserPreferences,
    tasks: List<Task>,
    onDefaultReminder: (Int?) -> Unit,
    onWeekStart: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onQuotes: (Boolean) -> Unit,
    onTone: (String) -> Unit,
    onFollowUp: (Boolean) -> Unit,
    onFocusMinutes: (Int) -> Unit,
    onBreakMinutes: (Int) -> Unit,
    syncStatus: String,
    syncEndpoint: String,
    syncCode: String,
    onStartSyncReceiver: () -> Unit,
    onSyncTo: (String, String) -> Unit,
    onReminderOwner: (Boolean) -> Unit,
    onRestoreComplete: () -> Unit
) {
    val context = LocalContext.current
    val backup = remember { TaskBackup(context) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var peerEndpoint by remember { mutableStateOf("") }
    var peerCode by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(backup.exportJson()) }
                    ?: error("Could not open backup file")
            }.onSuccess { message = "Backup saved" }.onFailure { message = "Backup failed: ${it.message}" }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingRestore = uri }
    val alarms = context.getSystemService(AlarmManager::class.java)
    val exactAllowed = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    val defaultLabel = preferences.defaultReminderMinutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "None"

    LazyColumn(
        modifier = Modifier.fillMaxHeight().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("MAKE IT YOURS", style = MaterialTheme.typography.labelMedium, color = KairosAccent)
            Spacer(Modifier.height(7.dp))
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(22.dp))
        }
        item {
            SettingsGroup("Reminders", "Default reminder", defaultLabel) {
                onDefaultReminder(when (preferences.defaultReminderMinutes) { null -> 540; 540 -> 720; 720 -> 1080; else -> null })
            }
        }
        item { SettingsToggle("Reminders", "Sound", preferences.soundEnabled, onSound) }
        item { SettingsToggle("Reminders", "Vibration", preferences.vibrationEnabled, onVibration) }
        item {
            val tone = NotificationTone.from(preferences.notificationTone)
            SettingsGroup("Reminders", "Notification voice", tone.label) {
                onTone(NotificationTone.entries[(NotificationTone.entries.indexOf(tone) + 1) % NotificationTone.entries.size].value)
            }
        }
        item { SettingsToggle("Reminders", "One firm follow-up", preferences.followUpEnabled, onFollowUp) }
        item {
            SettingsGroup("Reminders", "Exact alarm access", if (exactAllowed) "Allowed" else "Review") {
                if (Build.VERSION.SDK_INT >= 31 && !alarms.canScheduleExactAlarms()) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
        }
        item { SettingsToggle("Planning", "Week starts Monday", preferences.weekStartsMonday, onWeekStart) }
        item { SettingsToggle("Planning", "Daily scientist quote", preferences.quotesEnabled, onQuotes) }
        item {
            SettingsGroup("Focus", "Focus duration", "${preferences.focusMinutes} min") {
                onFocusMinutes(if (preferences.focusMinutes == 25) 50 else 25)
            }
        }
        item {
            SettingsGroup("Focus", "Break duration", "${preferences.breakMinutes} min") {
                onBreakMinutes(if (preferences.breakMinutes == 5) 10 else 5)
            }
        }
        item { SettingsToggle("Devices", "Reminders on this device", preferences.remindersOnThisDevice, onReminderOwner) }
        item {
            SettingsGroup("Devices", "Receive local sync", syncEndpoint) { onStartSyncReceiver() }
        }
        item {
            SettingsGroup("Devices", "Pairing code", syncCode) { }
        }
        item {
            SettingsGroup("Devices", "Sync to another device", "Connect") { showSyncDialog = true }
        }
        item { Text(syncStatus, color = KairosMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
        item { SettingsGroup("Your data", "Create JSON backup", "${tasks.size} tasks") { export.launch("kairos-backup.json") } }
        item { SettingsGroup("Your data", "Restore JSON backup", "Choose file") { restore.launch(arrayOf("application/json", "text/plain")) } }
        message?.let { value -> item { Text(value, color = KairosMint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp)) } }
        item {
            Text("KAIROS · MVP", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.labelMedium, color = Color(0xFF5F6370))
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces all current tasks with the tasks in the selected backup. This cannot be undone unless you export a backup first.") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Could not read backup")
                        backup.restoreJson(json)
                    }.onSuccess { count ->
                        onRestoreComplete()
                        message = "$count tasks restored"
                    }.onFailure { message = "Restore failed: ${it.message}" }
                    pendingRestore = null
                }) { Text("Replace tasks", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
            containerColor = KairosSurface
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync Kairos devices") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("On the other device, tap Receive local sync. Enter its address and six-digit code here. Both devices must use the same private Wi-Fi.", color = KairosMuted)
                    OutlinedTextField(value = peerEndpoint, onValueChange = { peerEndpoint = it }, label = { Text("IP address and port") }, placeholder = { Text("192.168.0.98:45873") }, singleLine = true)
                    OutlinedTextField(value = peerCode, onValueChange = { peerCode = it.filter(Char::isDigit).take(6) }, label = { Text("Pairing code") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = peerEndpoint.isNotBlank() && peerCode.length == 6,
                    onClick = { onSyncTo(peerEndpoint, peerCode); showSyncDialog = false }
                ) { Text("Sync now") }
            },
            dismissButton = { TextButton(onClick = { showSyncDialog = false }) { Text("Cancel") } },
            containerColor = KairosSurface
        )
    }
}

@Composable
private fun SettingsGroup(eyebrow: String, title: String, value: String, onClick: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = KairosSurface), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = KairosMuted)
                Spacer(Modifier.height(5.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(value, color = KairosAccent, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingsToggle(eyebrow: String, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = KairosSurface), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = KairosMuted)
                Spacer(Modifier.height(5.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = KairosMuted, modifier = Modifier.padding(start = 3.dp, top = 3.dp, bottom = 2.dp))
}

@Composable
private fun BottomNavigation(selected: Destination, onSelect: (Destination) -> Unit) {
    Surface(color = KairosBackground.copy(alpha = .97f), shadowElevation = 10.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(70.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Destination.entries.forEach { destination ->
                val active = destination == selected
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { onSelect(destination) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(destination.mark, color = if (active) KairosAccent else KairosMuted, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(destination.label, color = if (active) MaterialTheme.colorScheme.onSurface else KairosMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TaskEditorSheet(
    task: Task?,
    origin: EditorOrigin,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Int?, Int, String, Int) -> Unit,
    onDelete: (() -> Unit)?,
    defaultReminder: Int?,
    weekStartsMonday: Boolean
) {
    val today = LocalDate.now()
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var notes by remember(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var priority by remember(task?.id) { mutableIntStateOf(task?.priority ?: 0) }
    var repeatRule by remember(task?.id) { mutableStateOf(task?.repeatRule ?: RepeatRule.NONE.value) }
    var estimatedPomodoros by remember(task?.id) { mutableIntStateOf(task?.estimatedPomodoros ?: 0) }
    var dateChoice by remember(task?.id, origin) {
        mutableIntStateOf(when (task?.scheduledDate) {
            null -> if (origin == EditorOrigin.Upcoming) 2 else if (task == null) 1 else 0
            today.toString() -> 1
            today.plusDays(1).toString() -> 2
            else -> 3
        })
    }
    var reminder by remember(task?.id) { mutableStateOf(task?.reminderMinutes ?: if (task == null) defaultReminder else null) }
    var customDate by remember(task?.id, origin) { mutableStateOf(task?.scheduledDate ?: today.plusDays(7).toString()) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current
    val selectedDate = when (dateChoice) {
        0 -> null
        1 -> today.toString()
        2 -> today.plusDays(1).toString()
        else -> customDate
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KairosSurface, dragHandle = null) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 760.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (task == null) "New task" else "Edit task", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Close", color = KairosMuted) }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What needs doing?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes (optional)") },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
            )
            Spacer(Modifier.height(19.dp))
            SectionLabel("WHEN")
            Spacer(Modifier.height(7.dp))
            if (task == null && origin == EditorOrigin.Today) {
                FixedPlanningDate("Today", today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)))
            } else if (task == null && origin == EditorOrigin.Upcoming) {
                ChoiceRow(
                    choices = listOf("Tomorrow", formatEditorDate(customDate)),
                    selected = if (dateChoice == 2) 0 else 1,
                    onSelect = { index ->
                        dateChoice = if (index == 0) 2 else 3
                        if (index == 1) showPlanningDatePicker(context, customDate, today.plusDays(1), weekStartsMonday) { customDate = it }
                    }
                )
            } else {
                ChoiceRow(
                    choices = listOf("Anytime", "Today", "Tomorrow", formatEditorDate(customDate)),
                    selected = dateChoice,
                    onSelect = { index ->
                        dateChoice = index
                        if (index == 3) showPlanningDatePicker(context, customDate, today, weekStartsMonday) { customDate = it }
                    }
                )
            }
            Spacer(Modifier.height(19.dp))
            SectionLabel("REMIND ME")
            Spacer(Modifier.height(7.dp))
            ChoiceRow(
                choices = listOf("None", "09:00", "12:00", reminder?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "Custom"),
                selected = when (reminder) { null -> 0; 540 -> 1; 720 -> 2; else -> 3 },
                onSelect = { index ->
                    when (index) {
                        0 -> reminder = null
                        1 -> reminder = 540
                        2 -> reminder = 720
                        else -> {
                            val initial = reminder ?: 1080
                            TimePickerDialog(context, { _, hour, minute -> reminder = hour * 60 + minute }, initial / 60, initial % 60, true).show()
                        }
                    }
                }
            )
            Spacer(Modifier.height(19.dp))
            SectionLabel("EISENHOWER BOX")
            Spacer(Modifier.height(7.dp))
            EisenhowerPicker(priority) { priority = it }
            Spacer(Modifier.height(19.dp))
            SectionLabel("PLANNED POMODOROS")
            Spacer(Modifier.height(7.dp))
            PomodoroPicker(estimatedPomodoros) { estimatedPomodoros = it }
            Spacer(Modifier.height(19.dp))
            SectionLabel("REPEAT")
            Spacer(Modifier.height(7.dp))
            ChoiceRow(
                choices = RepeatRule.entries.map { it.label },
                selected = RepeatRule.entries.indexOf(RepeatRule.from(repeatRule)),
                onSelect = { repeatRule = RepeatRule.entries[it].value }
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSave(title, notes, selectedDate, reminder, priority, repeatRule, estimatedPomodoros) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KairosAccent)
            ) { Text(if (task == null) "Add to plan" else "Save changes", style = MaterialTheme.typography.labelLarge) }
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete task", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

internal fun tasksForToday(tasks: List<Task>, today: LocalDate = LocalDate.now()): List<Task> =
    tasks.filter { task -> task.scheduledDate?.let { runCatching { LocalDate.parse(it) == today }.getOrDefault(false) } == true }

internal fun tasksForUpcoming(tasks: List<Task>, today: LocalDate = LocalDate.now()): List<Task> =
    tasks.filter { task -> task.scheduledDate?.let { runCatching { LocalDate.parse(it) > today }.getOrDefault(false) } == true }
        .sortedWith(compareBy<Task> { it.scheduledDate }.thenBy { it.sortPosition })

@Composable
private fun FixedPlanningDate(title: String, detail: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = KairosAccent.copy(alpha = .18f), border = androidx.compose.foundation.BorderStroke(1.dp, KairosAccent.copy(alpha = .7f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color(0xFFD8D3FF), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text(detail, color = KairosMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun showPlanningDatePicker(context: android.content.Context, current: String, minimum: LocalDate, weekStartsMonday: Boolean, onDate: (String) -> Unit) {
    val initial = runCatching { LocalDate.parse(current) }.getOrDefault(minimum).coerceAtLeast(minimum)
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day).toString()) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
        datePicker.minDate = minimum.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePicker.firstDayOfWeek = if (weekStartsMonday) java.util.Calendar.MONDAY else java.util.Calendar.SUNDAY
    }.show()
}

@Composable
private fun PomodoroPicker(value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        PomodoroButton("−", enabled = value > 0) { onChange((value - 1).coerceAtLeast(0)) }
        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = KairosAccent.copy(alpha = .18f), border = androidx.compose.foundation.BorderStroke(1.dp, KairosAccent.copy(alpha = .7f))) {
            Box(Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                Text(if (value == 0) "None" else "$value sessions", color = Color(0xFFD8D3FF), style = MaterialTheme.typography.labelLarge)
            }
        }
        PomodoroButton("+", enabled = value < 99) { onChange((value + 1).coerceAtMost(99)) }
        PomodoroButton("+5", enabled = value < 99) { onChange((value + 5).coerceAtMost(99)) }
    }
}

@Composable
private fun PomodoroButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(width = 54.dp, height = 44.dp).alpha(if (enabled) 1f else .4f).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = KairosSurfaceHigh
    ) { Box(contentAlignment = Alignment.Center) { Text(label, color = KairosAccent, style = MaterialTheme.typography.labelLarge) } }
}

private fun formatEditorDate(value: String?): String = value?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d")) }.getOrDefault("Next week")
} ?: "Next week"

@Composable
private fun EisenhowerPicker(selected: Int, onSelect: (Int) -> Unit) {
    val boxes = listOf(
        EisenhowerQuadrant.DO_NOW,
        EisenhowerQuadrant.SCHEDULE,
        EisenhowerQuadrant.DELEGATE,
        EisenhowerQuadrant.ELIMINATE
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        boxes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { quadrant ->
                    val active = selected == quadrant.value
                    Surface(
                        modifier = Modifier.weight(1f).height(70.dp).clickable { onSelect(quadrant.value) },
                        shape = RoundedCornerShape(13.dp),
                        color = if (active) quadrantColor(quadrant).copy(alpha = .18f) else KairosSurfaceHigh,
                        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, quadrantColor(quadrant)) else null
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center) {
                            Text(quadrant.label, style = MaterialTheme.typography.labelLarge, color = if (active) quadrantColor(quadrant) else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(quadrant.hint, style = MaterialTheme.typography.labelMedium, color = KairosMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (selected != EisenhowerQuadrant.NONE.value) {
            TextButton(onClick = { onSelect(EisenhowerQuadrant.NONE.value) }, modifier = Modifier.align(Alignment.End)) {
                Text("Clear box", color = KairosMuted)
            }
        }
    }
}

@Composable
private fun ChoiceRow(choices: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        choices.forEachIndexed { index, label ->
            val active = selected == index
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (active) KairosAccent.copy(alpha = .18f) else KairosSurfaceHigh,
                border = if (active) androidx.compose.foundation.BorderStroke(1.dp, KairosAccent.copy(alpha = .7f)) else null
            ) {
                Box(Modifier.padding(horizontal = 4.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, color = if (active) Color(0xFFD8D3FF) else KairosMuted)
                }
            }
        }
    }
}
