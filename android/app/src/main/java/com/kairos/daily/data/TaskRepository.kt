package com.kairos.daily.data

import android.content.Context

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TaskRepository(context: Context) {
    private val database = TaskDatabase(context.applicationContext)

    fun getTasks(): List<Task> = database.allTasks()
    fun getTask(id: Long): Task? = database.task(id)
    fun getTaskBySyncId(syncId: String): Task? = database.taskBySyncId(syncId)?.takeUnless { it.isDeleted }
    fun getSyncTasks(): List<Task> = database.allForSync()
    fun add(task: Task): Long = database.insert(task).also { notifyDataChanged() }
    fun update(task: Task) {
        database.update(task.copy(updatedAt = System.currentTimeMillis()))
        notifyDataChanged()
    }
    fun delete(id: Long) {
        database.delete(id)
        notifyDataChanged()
    }
    fun replaceAll(tasks: List<Task>) {
        database.replaceAll(tasks)
        notifyDataChanged()
    }

    fun mergeSynced(tasks: List<Task>): Int {
        var changed = 0
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            tasks.forEach { remote ->
                val local = database.taskBySyncId(remote.syncId)
                if (local == null) {
                    database.insert(remote.copy(id = 0))
                    changed++
                } else if (shouldApplyRemote(local, remote)) {
                    database.update(remote.copy(id = local.id))
                    changed++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        if (changed > 0) notifyDataChanged()
        return changed
    }
    fun completePomodoro(taskId: Long) {
        getTask(taskId)?.let { task -> update(task.copy(completedPomodoros = task.completedPomodoros + 1)) }
    }

    fun move(task: Task, direction: Int) {
        val peers = getTasks().filter { !it.isCompleted && it.priority == task.priority }
        val swapped = swappedPositions(peers, task.id, direction) ?: return
        database.update(swapped.first.copy(updatedAt = System.currentTimeMillis()))
        database.update(swapped.second.copy(updatedAt = System.currentTimeMillis()))
        notifyDataChanged()
    }

    companion object {
        private val _dataEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val dataEvents: SharedFlow<Unit> = _dataEvents.asSharedFlow()

        fun notifyDataChanged() {
            _dataEvents.tryEmit(Unit)
        }
    }
}

fun shouldApplyRemote(local: Task, remote: Task): Boolean =
    local.syncId == remote.syncId && remote.updatedAt > local.updatedAt

fun swappedPositions(tasks: List<Task>, taskId: Long, direction: Int): Pair<Task, Task>? {
    if (direction != -1 && direction != 1) return null
    val index = tasks.indexOfFirst { it.id == taskId }
    val otherIndex = index + direction
    if (index < 0 || otherIndex !in tasks.indices) return null
    val task = tasks[index]
    val other = tasks[otherIndex]
    return task.copy(sortPosition = other.sortPosition) to other.copy(sortPosition = task.sortPosition)
}
