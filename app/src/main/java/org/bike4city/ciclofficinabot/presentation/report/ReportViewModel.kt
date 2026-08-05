package org.bike4city.ciclofficinabot.presentation.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bike4city.ciclofficinabot.BuildConfig
import org.bike4city.ciclofficinabot.data.firebase.FirebaseSessionRepository
import org.bike4city.ciclofficinabot.data.local.Bike4CityDatabase
import org.bike4city.ciclofficinabot.data.repository.LocalHistoryRepository
import org.bike4city.ciclofficinabot.domain.repository.DisabledRemoteReportRepository
import org.bike4city.ciclofficinabot.domain.repository.RemoteReportRepository
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncInfo
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStatus
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStore

sealed interface ReportSyncUiState {
    data object Unavailable : ReportSyncUiState
    data object LocalOnly : ReportSyncUiState
    data object Syncing : ReportSyncUiState
    data object Removing : ReportSyncUiState
    data class Synced(val syncedAt: Long) : ReportSyncUiState
    data class Error(val message: String) : ReportSyncUiState
}

class ReportViewModel(
    private val store: ReportSyncStore,
    private val remote: RemoteReportRepository,
    private val sessionId: String
) : ViewModel() {
    val report = store.observeReport(sessionId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null
    )

    private val operation = MutableStateFlow<ReportSyncUiState?>(null)
    val syncState = combine(store.observeSyncInfo(sessionId), operation) { saved, active ->
        if (!remote.isConfigured) return@combine ReportSyncUiState.Unavailable
        active ?: when (saved.status) {
            ReportSyncStatus.LOCAL_ONLY -> ReportSyncUiState.LocalOnly
            ReportSyncStatus.SYNCED -> ReportSyncUiState.Synced(saved.syncedAt ?: 0L)
            ReportSyncStatus.FAILED -> ReportSyncUiState.Error(RETRY_MESSAGE)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        if (remote.isConfigured) ReportSyncUiState.LocalOnly else ReportSyncUiState.Unavailable
    )

    fun syncReport() {
        if (!remote.isConfigured || syncState.value is ReportSyncUiState.Syncing ||
            syncState.value is ReportSyncUiState.Removing || syncState.value is ReportSyncUiState.Synced
        ) return
        viewModelScope.launch {
            val saved = store.observeSyncInfo(sessionId).first()
            if (saved.status == ReportSyncStatus.SYNCED) {
                operation.value = ReportSyncUiState.Synced(saved.syncedAt ?: 0L)
                return@launch
            }
            operation.value = ReportSyncUiState.Syncing
            val current = store.observeReport(sessionId).filterNotNull().first()
            remote.saveReport(sessionId, current).fold(
                onSuccess = {
                    val syncedAt = System.currentTimeMillis()
                    store.updateSyncInfo(sessionId, ReportSyncInfo(ReportSyncStatus.SYNCED, syncedAt))
                    operation.value = ReportSyncUiState.Synced(syncedAt)
                },
                onFailure = {
                    runCatching {
                        store.updateSyncInfo(sessionId, ReportSyncInfo(ReportSyncStatus.FAILED))
                    }
                    operation.value = ReportSyncUiState.Error(RETRY_MESSAGE)
                }
            )
        }
    }

    fun removeRemoteCopy() {
        if (!remote.isConfigured || syncState.value is ReportSyncUiState.Syncing ||
            syncState.value is ReportSyncUiState.Removing
        ) return
        viewModelScope.launch {
            val saved = store.observeSyncInfo(sessionId).first()
            if (saved.status != ReportSyncStatus.SYNCED) return@launch
            operation.value = ReportSyncUiState.Removing
            remote.deleteReport(sessionId).fold(
                onSuccess = {
                    store.updateSyncInfo(sessionId, ReportSyncInfo())
                    operation.value = ReportSyncUiState.LocalOnly
                },
                onFailure = {
                    operation.value = ReportSyncUiState.Error(DELETE_ERROR_MESSAGE)
                }
            )
        }
    }

    fun dismissError() {
        if (syncState.value is ReportSyncUiState.Error) operation.value = null
    }

    companion object {
        private const val RETRY_MESSAGE =
            "Sincronizzazione non riuscita. Controlla la connessione e riprova."
        private const val DELETE_ERROR_MESSAGE =
            "La copia online non è stata eliminata. Controlla la connessione e riprova."

        fun factory(context: Context, sessionId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val remote = if (BuildConfig.FIREBASE_CONFIGURED) {
                        FirebaseSessionRepository(appContext)
                    } else {
                        DisabledRemoteReportRepository
                    }
                    return ReportViewModel(
                        LocalHistoryRepository(Bike4CityDatabase.get(appContext).chatDao()),
                        remote,
                        sessionId
                    ) as T
                }
            }
    }
}
