package org.bike4city.ciclofficinabot.presentation.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bike4city.ciclofficinabot.data.image.PhotoSanitizer
import org.bike4city.ciclofficinabot.BuildConfig
import org.bike4city.ciclofficinabot.data.firebase.FirebaseChatRepository
import org.bike4city.ciclofficinabot.data.local.Bike4CityDatabase
import org.bike4city.ciclofficinabot.data.preferences.UserPreferencesRepository
import org.bike4city.ciclofficinabot.data.repository.LocalHistoryRepository
import org.bike4city.ciclofficinabot.domain.model.*
import org.bike4city.ciclofficinabot.domain.report.DiagnosisReportFactory
import org.bike4city.ciclofficinabot.domain.repository.ChatRepository

data class ChatUiState(
    val sessionId: String = "",
    val messages: List<ChatMessage> = listOf(
        ChatMessage(0, ChatRole.ASSISTANT, "Ciao! Quale zona della bici ti sta dando problemi? Ti farò una domanda alla volta.")
    ),
    val quickReplies: List<String> = listOf("Gomma o ruota", "Freni", "Catena o cambio", "E-bike"),
    val isLoading: Boolean = false,
    val safetyLevel: SafetyLevel = SafetyLevel.SAFE,
    val outcome: DiagnosisOutcome = DiagnosisOutcome.UNDETERMINED,
    val instructionTitle: String? = null,
    val instructionBody: String? = null,
    val instructionWarnings: List<String> = emptyList(),
    val completed: Boolean = false,
    val photoError: String? = null,
    val serviceUnavailable: Boolean = false
)

class ChatViewModel(
    private val repository: ChatRepository,
    private val history: LocalHistoryRepository,
    private val preferences: UserPreferencesRepository,
    private val appContext: Context
) : ViewModel() {
    private var sessionId = newSessionId()
    private var startedAt = System.currentTimeMillis()
    private var initialProblem = ""
    private val _state = MutableStateFlow(ChatUiState(sessionId = sessionId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init { createSession() }

    fun send(text: String, photoUri: Uri? = null) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.isLoading || _state.value.completed) return
        if (initialProblem.isBlank()) initialProblem = clean
        val userMessage = ChatMessage(System.nanoTime(), ChatRole.USER, if (photoUri == null) clean else "$clean\n📷 Foto allegata")
        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                quickReplies = emptyList(),
                isLoading = true,
                photoError = null,
                serviceUnavailable = false
            )
        }
        viewModelScope.launch {
            val photo = try {
                photoUri?.let { PhotoSanitizer.sanitize(appContext, it) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, photoError = "Non è stato possibile preparare la foto. Prova un'altra immagine.") }
                return@launch
            }
            val bikeProfile = preferences.current().bikeProfile
            history.ensureSession(sessionId, startedAt, bikeProfile.name)
            history.saveMessage(sessionId, userMessage)
            val reply = repository.replyTo(sessionId, clean, photo)
            val botMessage = ChatMessage(System.nanoTime(), ChatRole.ASSISTANT, reply.message, reply.type, reply.safetyLevel)
            history.saveMessage(sessionId, botMessage)
            history.updateSession(sessionId, initialProblem, reply.safetyLevel, reply.outcome, reply.completed)
            if (reply.completed) {
                history.saveReport(
                    sessionId,
                    DiagnosisReportFactory.create(
                        initialProblem = initialProblem,
                        safetyLevel = reply.safetyLevel,
                        outcome = reply.outcome,
                        instructionTitle = _state.value.instructionTitle,
                        bikeSummary = bikeProfile.summary()
                    )
                )
            }
            _state.update {
                it.copy(
                    messages = it.messages + botMessage,
                    quickReplies = reply.quickReplies,
                    isLoading = false,
                    safetyLevel = reply.safetyLevel,
                    outcome = reply.outcome,
                    instructionTitle = reply.instructionTitle,
                    instructionBody = reply.instructionBody,
                    instructionWarnings = reply.instructionWarnings,
                    completed = reply.completed,
                    serviceUnavailable = reply.serviceUnavailable
                )
            }
        }
    }

    fun stop() {
        if (_state.value.safetyLevel == SafetyLevel.STOP) return
        val problem = initialProblem
        val instructionTitle = _state.value.instructionTitle
        _state.update { it.copy(safetyLevel = SafetyLevel.STOP, outcome = DiagnosisOutcome.RED, completed = true) }
        viewModelScope.launch {
            val bikeProfile = preferences.current().bikeProfile
            val report = DiagnosisReportFactory.create(
                initialProblem = problem,
                safetyLevel = SafetyLevel.STOP,
                outcome = DiagnosisOutcome.RED,
                instructionTitle = instructionTitle,
                bikeSummary = bikeProfile.summary()
            )
            history.ensureSession(sessionId, startedAt, bikeProfile.name)
            history.updateSession(sessionId, report.initialProblem, SafetyLevel.STOP, DiagnosisOutcome.RED, true)
            history.saveReport(sessionId, report)
        }
    }

    fun reset() {
        repository.reset()
        sessionId = newSessionId()
        startedAt = System.currentTimeMillis()
        initialProblem = ""
        _state.value = ChatUiState(sessionId = sessionId)
        createSession()
    }

    private fun createSession() {
        viewModelScope.launch {
            val bikeProfile = preferences.current().bikeProfile
            history.ensureSession(sessionId, startedAt, bikeProfile.name)
            history.saveMessage(sessionId, _state.value.messages.first())
        }
    }

    private fun BikeProfile.summary() = buildString {
        append(bikeType)
        if (isElectric) append(" · e-bike")
        if (brand.isNotBlank()) append(" · $brand")
        if (model.isNotBlank()) append(" $model")
    }

    private fun newSessionId() = "local-${System.currentTimeMillis()}-${System.nanoTime()}"

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                check(BuildConfig.FIREBASE_CONFIGURED) {
                    "La configurazione Firebase è necessaria per la versione definitiva"
                }
                val chatRepository: ChatRepository = FirebaseChatRepository()
                return ChatViewModel(
                    chatRepository,
                    LocalHistoryRepository(Bike4CityDatabase.get(context).chatDao()),
                    UserPreferencesRepository(context.applicationContext),
                    context.applicationContext
                ) as T
            }
        }
    }
}
