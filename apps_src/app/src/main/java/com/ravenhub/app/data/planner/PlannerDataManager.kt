package com.ravenhub.app.data.planner

import android.content.Context
import com.ravenhub.app.security.SecureStorageEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlannerDataManager {
    private val _data = MutableStateFlow(PlannerData())
    val data = _data.asStateFlow()

    @Volatile
    private var isLoaded = false

    @Synchronized
    fun load(context: Context) {
        val loaded = SecureStorageEngine.loadPlannerSync(context)
        if (loaded != null) {
            _data.value = loaded
            isLoaded = true
        }
    }

    @Synchronized
    private fun persist(context: Context, newData: PlannerData) {
        if (!isLoaded) {
            load(context)
        }
        _data.value = newData
        SecureStorageEngine.savePlannerSync(context, newData)
    }

    // --- Todos ---
    fun addTodo(
        context: Context,
        title: String,
        category: String,
        dueDateTime: Long? = null,
        reminderOffsetMinutes: Int? = null,
        isAlarmEnabled: Boolean = false,
        subTasks: List<SubTaskItem> = emptyList()
    ): TodoItem {
        val todo = TodoItem(
            title = title,
            category = category,
            dueDateTime = dueDateTime,
            reminderOffsetMinutes = reminderOffsetMinutes,
            isAlarmEnabled = isAlarmEnabled,
            subTasks = subTasks
        )
        persist(context, _data.value.copy(todos = _data.value.todos + todo))
        return todo
    }

    fun updateTodo(
        context: Context,
        id: String,
        title: String,
        category: String,
        dueDateTime: Long? = null,
        reminderOffsetMinutes: Int? = null,
        isAlarmEnabled: Boolean = false,
        subTasks: List<SubTaskItem> = emptyList()
    ): TodoItem {
        val existing = _data.value.todos.find { it.id == id }
        val updated = TodoItem(
            id = id,
            title = title,
            category = category,
            dueDateTime = dueDateTime,
            reminderOffsetMinutes = reminderOffsetMinutes,
            isAlarmEnabled = isAlarmEnabled,
            subTasks = subTasks,
            isCompleted = existing?.isCompleted ?: false
        )
        persist(context, _data.value.copy(todos = _data.value.todos.map { if (it.id == id) updated else it }))
        return updated
    }

    fun toggleTodo(context: Context, id: String) {
        persist(
            context,
            _data.value.copy(
                todos = _data.value.todos.map {
                    if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
                }
            )
        )
    }

    fun toggleSubTask(context: Context, todoId: String, subTaskId: String) {
        persist(
            context,
            _data.value.copy(
                todos = _data.value.todos.map { todo ->
                    if (todo.id == todoId) {
                        val updatedSubTasks = todo.subTasks.map { sub ->
                            if (sub.id == subTaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
                        }
                        val allSubCompleted = updatedSubTasks.isNotEmpty() && updatedSubTasks.all { it.isCompleted }
                        todo.copy(subTasks = updatedSubTasks, isCompleted = if (allSubCompleted) true else todo.isCompleted)
                    } else todo
                }
            )
        )
    }

    fun deleteTodo(context: Context, id: String) {
        persist(context, _data.value.copy(todos = _data.value.todos.filter { it.id != id }))
    }

    // --- Habits ---
    fun addHabit(context: Context, title: String, frequency: HabitFrequency) {
        val habit = HabitItem(title = title, frequency = frequency)
        persist(context, _data.value.copy(habits = _data.value.habits + habit))
    }

    fun updateHabit(context: Context, id: String, title: String, frequency: HabitFrequency) {
        persist(
            context,
            _data.value.copy(
                habits = _data.value.habits.map { habit ->
                    if (habit.id == id) habit.copy(title = title, frequency = frequency) else habit
                }
            )
        )
    }

    fun toggleHabitToday(context: Context, id: String) {
        val today = System.currentTimeMillis() / 86_400_000L
        persist(
            context,
            _data.value.copy(
                habits = _data.value.habits.map { habit ->
                    if (habit.id == id) {
                        val dates = habit.completedDates.toMutableList()
                        if (today in dates) dates.remove(today) else dates.add(today)
                        habit.copy(completedDates = dates)
                    } else habit
                }
            )
        )
    }

    fun deleteHabit(context: Context, id: String) {
        persist(context, _data.value.copy(habits = _data.value.habits.filter { it.id != id }))
    }
}
