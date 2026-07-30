package com.ravenhub.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ravenhub.app.data.notes.*
import kotlinx.coroutines.flow.StateFlow

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    val data: StateFlow<NotesData> = NotesDataManager.data

    fun reload() {
        NotesDataManager.load(getApplication())
    }

    fun addNote(title: String, content: String, category: String = "") {
        NotesDataManager.addNote(getApplication(), title, content, category)
    }

    fun updateNote(id: String, title: String, content: String, category: String = "") {
        NotesDataManager.updateNote(getApplication(), id, title, content, category)
    }

    fun deleteNote(id: String) {
        NotesDataManager.deleteNote(getApplication(), id)
    }

    fun getBacklinksFor(title: String): List<NoteItem> {
        return NotesDataManager.getBacklinksFor(title)
    }
}
