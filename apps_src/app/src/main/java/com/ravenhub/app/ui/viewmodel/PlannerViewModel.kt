package com.ravenhub.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ravenhub.app.data.planner.*
import kotlinx.coroutines.flow.StateFlow

class PlannerViewModel(app: Application) : AndroidViewModel(app) {

    val data: StateFlow<PlannerData> = PlannerDataManager.data

    fun reload() {
        PlannerDataManager.load(getApplication())
    }

    fun addTodo(
        title: String,
        category: String,
        dueDateTime: Long? = null,
        reminderOffsetMinutes: Int? = null,
        isAlarmEnabled: Boolean = false,
        subTasks: List<SubTaskItem> = emptyList()
    ): TodoItem {
        return PlannerDataManager.addTodo(getApplication(), title, category, dueDateTime, reminderOffsetMinutes, isAlarmEnabled, subTasks)
    }

    fun updateTodo(
        id: String,
        title: String,
        category: String,
        dueDateTime: Long? = null,
        reminderOffsetMinutes: Int? = null,
        isAlarmEnabled: Boolean = false,
        subTasks: List<SubTaskItem> = emptyList()
    ): TodoItem {
        return PlannerDataManager.updateTodo(getApplication(), id, title, category, dueDateTime, reminderOffsetMinutes, isAlarmEnabled, subTasks)
    }

    fun toggleTodo(id: String) {
        PlannerDataManager.toggleTodo(getApplication(), id)
    }

    fun toggleSubTask(todoId: String, subTaskId: String) {
        PlannerDataManager.toggleSubTask(getApplication(), todoId, subTaskId)
    }

    fun deleteTodo(id: String) {
        PlannerDataManager.deleteTodo(getApplication(), id)
    }

    fun addHabit(title: String, frequency: HabitFrequency) {
        PlannerDataManager.addHabit(getApplication(), title, frequency)
    }

    fun updateHabit(id: String, title: String, frequency: HabitFrequency) {
        PlannerDataManager.updateHabit(getApplication(), id, title, frequency)
    }

    fun toggleHabitToday(id: String) {
        PlannerDataManager.toggleHabitToday(getApplication(), id)
    }

    fun deleteHabit(id: String) {
        PlannerDataManager.deleteHabit(getApplication(), id)
    }
}
