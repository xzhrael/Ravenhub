package com.ravenhub.app.data.planner

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SubTaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

@Serializable
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String = "",
    val isCompleted: Boolean = false,
    val dueDateTime: Long? = null,
    val reminderOffsetMinutes: Int? = null,
    val subTasks: List<SubTaskItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class HabitItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val completedDates: List<Long> = emptyList(), // ponytail: epoch days, not millis
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class HabitFrequency { DAILY, WEEKLY }

@Serializable
data class PlannerData(
    val todos: List<TodoItem> = emptyList(),
    val habits: List<HabitItem> = emptyList()
)
