package com.ravenhub.app.data.notes

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String = "",
    val category: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    // ponytail: regex to extract [[WikiLink]] titles
    fun extractBacklinks(): List<String> {
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        return regex.findAll(content).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.distinct().toList()
    }
}

@Serializable
data class NotesData(
    val notes: List<NoteItem> = emptyList()
)
