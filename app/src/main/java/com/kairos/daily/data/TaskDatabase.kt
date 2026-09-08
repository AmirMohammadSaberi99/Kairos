package com.kairos.daily.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaskDatabase(context: Context) : SQLiteOpenHelper(context, "kairos.db", null, 6) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sync_id TEXT NOT NULL UNIQUE,
                title TEXT NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                scheduled_date TEXT,
                reminder_minutes INTEGER,
                priority INTEGER NOT NULL DEFAULT 0,
                repeat_rule TEXT NOT NULL DEFAULT 'NONE',
                sort_position INTEGER NOT NULL DEFAULT 0,
                estimated_pomodoros INTEGER NOT NULL DEFAULT 0,
                completed_pomodoros INTEGER NOT NULL DEFAULT 0,
                is_completed INTEGER NOT NULL DEFAULT 0,
                completed_at INTEGER,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
                ,is_deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tasks_date ON tasks(scheduled_date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE tasks ADD COLUMN repeat_rule TEXT NOT NULL DEFAULT 'NONE'")
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN sort_position INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE tasks SET sort_position = created_at WHERE sort_position = 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN estimated_pomodoros INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE tasks ADD COLUMN completed_pomodoros INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN sync_id TEXT")
            db.execSQL("ALTER TABLE tasks ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE tasks SET sync_id = lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1,1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))) WHERE sync_id IS NULL")
            db.execSQL("CREATE UNIQUE INDEX idx_tasks_sync_id ON tasks(sync_id)")
        }
        if (oldVersion < 6) {
            // Earlier versions treated null dates as an implicit Today bucket.
            // Make that meaning explicit before Today becomes date-strict.
            db.execSQL("UPDATE tasks SET scheduled_date = date('now', 'localtime'), updated_at = ? WHERE scheduled_date IS NULL AND is_deleted = 0", arrayOf(System.currentTimeMillis()))
        }
    }

    fun allTasks(): List<Task> = readableDatabase.query(
        "tasks", null, "is_deleted = 0", null, null, null,
        "is_completed ASC, CASE priority WHEN 1 THEN 0 WHEN 2 THEN 1 WHEN 3 THEN 2 WHEN 4 THEN 3 ELSE 4 END, sort_position ASC, scheduled_date ASC, reminder_minutes ASC, created_at ASC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toTask()) } }

    fun task(id: Long): Task? = readableDatabase.query(
        "tasks", null, "id = ? AND is_deleted = 0", arrayOf(id.toString()), null, null, null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toTask() else null }

    fun taskBySyncId(syncId: String): Task? = readableDatabase.query(
        "tasks", null, "sync_id = ?", arrayOf(syncId), null, null, null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toTask() else null }

    fun allForSync(): List<Task> = readableDatabase.query(
        "tasks", null, null, null, null, null, "updated_at ASC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toTask()) } }

    fun insert(task: Task): Long = writableDatabase.insertOrThrow("tasks", null, task.values())

    fun update(task: Task) {
        writableDatabase.update("tasks", task.values(), "id = ?", arrayOf(task.id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.execSQL("UPDATE tasks SET is_deleted = 1, updated_at = ? WHERE id = ?", arrayOf(System.currentTimeMillis(), id))
    }

    fun replaceAll(tasks: List<Task>) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("tasks", null, null)
            tasks.forEach { task ->
                val values = task.values().apply { if (task.id > 0) put("id", task.id) }
                writableDatabase.insertOrThrow("tasks", null, values)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    private fun Task.values() = ContentValues().apply {
        put("title", title)
        put("sync_id", syncId)
        put("notes", notes)
        if (scheduledDate == null) putNull("scheduled_date") else put("scheduled_date", scheduledDate)
        if (reminderMinutes == null) putNull("reminder_minutes") else put("reminder_minutes", reminderMinutes)
        put("priority", priority)
        put("repeat_rule", repeatRule)
        put("sort_position", sortPosition)
        put("estimated_pomodoros", estimatedPomodoros)
        put("completed_pomodoros", completedPomodoros)
        put("is_completed", if (isCompleted) 1 else 0)
        if (completedAt == null) putNull("completed_at") else put("completed_at", completedAt)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
        put("is_deleted", if (isDeleted) 1 else 0)
    }

    private fun Cursor.toTask() = Task(
        id = getLong(getColumnIndexOrThrow("id")),
        syncId = getString(getColumnIndexOrThrow("sync_id")),
        title = getString(getColumnIndexOrThrow("title")),
        notes = getString(getColumnIndexOrThrow("notes")),
        scheduledDate = getString(getColumnIndexOrThrow("scheduled_date")),
        reminderMinutes = getColumnIndexOrThrow("reminder_minutes").let { if (isNull(it)) null else getInt(it) },
        priority = getInt(getColumnIndexOrThrow("priority")),
        repeatRule = getString(getColumnIndexOrThrow("repeat_rule")),
        sortPosition = getLong(getColumnIndexOrThrow("sort_position")),
        estimatedPomodoros = getInt(getColumnIndexOrThrow("estimated_pomodoros")),
        completedPomodoros = getInt(getColumnIndexOrThrow("completed_pomodoros")),
        isCompleted = getInt(getColumnIndexOrThrow("is_completed")) == 1,
        completedAt = getColumnIndexOrThrow("completed_at").let { if (isNull(it)) null else getLong(it) },
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        isDeleted = getInt(getColumnIndexOrThrow("is_deleted")) == 1
    )
}
