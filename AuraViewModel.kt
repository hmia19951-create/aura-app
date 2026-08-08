package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import com.example.data.ChatRepository
import com.example.data.ChatSessionEntity
import com.example.util.AuraTtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuraViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ChatRepository(database.chatDao(), application)
    val ttsManager = AuraTtsManager(application)

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _selectedMood = MutableStateFlow("Warm & Peer")
    val selectedMood: StateFlow<String> = _selectedMood.asStateFlow()

    private val _isAutoTtsEnabled = MutableStateFlow(false)
    val isAutoTtsEnabled: StateFlow<Boolean> = _isAutoTtsEnabled.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun createNewSession(title: String = "Conversation with Dulha") {
        viewModelScope.launch {
            val session = repository.createNewSession(title)
            _currentSessionId.value = session.id
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() && _selectedImageUri.value == null) return

        val sessionId = _currentSessionId.value ?: return
        val imageUriString = _selectedImageUri.value?.toString()
        _selectedImageUri.value = null

        viewModelScope.launch {
            _isThinking.value = true
            val result = repository.sendMessage(
                sessionId = sessionId,
                userText = userText,
                imageUri = imageUriString,
                currentMood = _selectedMood.value
            )
            _isThinking.value = false

            if (result.isSuccess && _isAutoTtsEnabled.value) {
                val text = result.getOrNull()
                if (!text.isNullOrBlank()) {
                    ttsManager.speak(text)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
