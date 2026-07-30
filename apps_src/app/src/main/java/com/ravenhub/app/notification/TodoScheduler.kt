package com.ravenhub.app.notification

import android.content.Context
import androidx.work.*
import com.ravenhub.app.data.planner.TodoItem
import java.util.concurrent.TimeUnit

object TodoScheduler {

    fun schedule(context: Context, todo: TodoItem) {
        val due = todo.dueDateTime ?: return
        val offsetMinutes = todo.reminderOffsetMinutes ?: 0
        val triggerTime = due - (offsetMinutes * 60 * 1000L)
        val delayMillis = triggerTime - System.currentTimeMillis()

        if (delayMillis <= 0 || todo.isCompleted) {
            cancel(context, todo.id)
            return
        }

        val inputData = workDataOf(
            TodoReminderWorker.KEY_TODO_ID to todo.id,
            TodoReminderWorker.KEY_TODO_TITLE to todo.title
        )

        // ponytail: unique WorkManager request per todo id
        val workRequest = OneTimeWorkRequestBuilder<TodoReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("todo_${todo.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "todo_${todo.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancel(context: Context, todoId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("todo_$todoId")
    }
}
