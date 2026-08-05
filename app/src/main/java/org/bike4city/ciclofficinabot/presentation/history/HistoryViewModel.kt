package org.bike4city.ciclofficinabot.presentation.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bike4city.ciclofficinabot.BuildConfig
import org.bike4city.ciclofficinabot.data.firebase.FirebaseSessionRepository
import org.bike4city.ciclofficinabot.data.local.Bike4CityDatabase
import org.bike4city.ciclofficinabot.data.local.ChatSessionEntity
import org.bike4city.ciclofficinabot.data.repository.LocalHistoryRepository
import org.bike4city.ciclofficinabot.domain.repository.DisabledRemoteReportRepository
import org.bike4city.ciclofficinabot.domain.repository.RemoteReportRepository
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStatus

data class HistoryDeleteState(
    val deletingSessionId: String? = null,
    val error: String? = null
)

class HistoryViewModel(
    private val repository: LocalHistoryRepository,
    private val remote: RemoteReportRepository
) : ViewModel() {
    val sessions = repository.observeSessions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val _deleteState = MutableStateFlow(HistoryDeleteState())
    val deleteState: StateFlow<HistoryDeleteState> = _deleteState.asStateFlow()

    fun delete(session: ChatSessionEntity) {
        if (_deleteState.value.deletingSessionId != null) return
        viewModelScope.launch {
            _deleteState.value = HistoryDeleteState(deletingSessionId = session.id)
            if (remote.isConfigured) {
                val remoteDeleted = remote.deleteReport(session.id).isSuccess
                if (!remoteDeleted) {
                    _deleteState.value = HistoryDeleteState(
                        error = "La copia online non è stata eliminata. La conversazione locale è stata conservata."
                    )
                    return@launch
                }
            }
            repository.delete(session.id)
            _deleteState.value = HistoryDeleteState()
        }
    }

    fun dismissError() {
        _deleteState.value = _deleteState.value.copy(error = null)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val remote = if (BuildConfig.FIREBASE_CONFIGURED) {
                    FirebaseSessionRepository(appContext)
                } else {
                    DisabledRemoteReportRepository
                }
                return HistoryViewModel(
                    LocalHistoryRepository(Bike4CityDatabase.get(appContext).chatDao()),
                    remote
                ) as T
            }
        }
    }
}
