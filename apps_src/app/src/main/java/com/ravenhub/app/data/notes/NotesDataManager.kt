package com.ravenhub.app.data.notes

import android.content.Context
import com.ravenhub.app.security.SecureStorageEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object NotesDataManager {
    private val _data = MutableStateFlow(NotesData())
    val data = _data.asStateFlow()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var isLoaded = false

    @Synchronized
    fun load(context: Context) {
        val loaded = SecureStorageEngine.loadNotesSync(context)
        if (loaded != null) {
            _data.value = loaded
            isLoaded = true
        }
    }

    @Synchronized
    private fun persist(context: Context, newData: NotesData) {
        if (!isLoaded) {
            load(context)
        }
        _data.value = newData
        ioScope.launch {
            SecureStorageEngine.saveNotesSync(context, newData)
        }
    }

    fun addNote(context: Context, title: String, content: String, category: String = "") {
        val note = NoteItem(
            title = title,
            content = content,
            category = category
        )
        persist(context, _data.value.copy(notes = _data.value.notes + note))
    }

    fun updateNote(context: Context, id: String, title: String, content: String, category: String = "") {
        persist(
            context,
            _data.value.copy(
                notes = _data.value.notes.map { note ->
                    if (note.id == id) {
                        note.copy(
                            title = title,
                            content = content,
                            category = category,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else note
                }
            )
        )
    }

    fun deleteNote(context: Context, id: String) {
        persist(context, _data.value.copy(notes = _data.value.notes.filter { it.id != id }))
    }

    fun getBacklinksFor(title: String): List<NoteItem> {
        if (title.isBlank()) return emptyList()
        val tag = "[[$title]]"
        return _data.value.notes.filter { it.title != title && it.content.contains(tag, ignoreCase = true) }
    }
}
