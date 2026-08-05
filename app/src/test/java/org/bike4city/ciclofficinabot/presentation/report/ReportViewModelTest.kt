package org.bike4city.ciclofficinabot.presentation.report

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bike4city.ciclofficinabot.domain.model.DiagnosisOutcome
import org.bike4city.ciclofficinabot.domain.model.DiagnosisReport
import org.bike4city.ciclofficinabot.domain.model.SafetyLevel
import org.bike4city.ciclofficinabot.domain.repository.RemoteReportRepository
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncInfo
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStatus
import org.bike4city.ciclofficinabot.domain.repository.ReportSyncStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var store: FakeReportStore
    private lateinit var remote: FakeRemoteReportRepository

    @Before fun setUp() {
        store = FakeReportStore()
        remote = FakeRemoteReportRepository()
    }

    @Test fun sincronizzazione_riuscita_viene_salvata_localmente() = runTest {
        val viewModel = ReportViewModel(store, remote, SESSION_ID)

        viewModel.syncReport()
        advanceUntilIdle()

        assertEquals(SESSION_ID, remote.savedSessionId)
        assertEquals(ReportSyncStatus.SYNCED, store.sync.value.status)
        assertTrue(viewModel.syncState.value is ReportSyncUiState.Synced)
    }

    @Test fun errore_remoto_lascia_il_report_locale_e_permette_di_riprovare() = runTest {
        remote.saveResult = Result.failure(IllegalStateException("offline"))
        val viewModel = ReportViewModel(store, remote, SESSION_ID)

        viewModel.syncReport()
        advanceUntilIdle()

        assertEquals(ReportSyncStatus.FAILED, store.sync.value.status)
        assertTrue(viewModel.syncState.value is ReportSyncUiState.Error)
        assertEquals(REPORT, store.report.value)
    }

    @Test fun eliminazione_remota_riporta_lo_stato_a_solo_locale() = runTest {
        store.sync.value = ReportSyncInfo(ReportSyncStatus.SYNCED, 123L)
        val viewModel = ReportViewModel(store, remote, SESSION_ID)

        viewModel.removeRemoteCopy()
        advanceUntilIdle()

        assertEquals(SESSION_ID, remote.deletedSessionId)
        assertEquals(ReportSyncStatus.LOCAL_ONLY, store.sync.value.status)
        assertTrue(viewModel.syncState.value is ReportSyncUiState.LocalOnly)
    }

    private class FakeReportStore : ReportSyncStore {
        val report = MutableStateFlow<DiagnosisReport?>(REPORT)
        val sync = MutableStateFlow(ReportSyncInfo())

        override fun observeReport(sessionId: String): Flow<DiagnosisReport?> = report
        override fun observeSyncInfo(sessionId: String): Flow<ReportSyncInfo> = sync
        override suspend fun updateSyncInfo(sessionId: String, info: ReportSyncInfo) {
            sync.value = info
        }
    }

    private class FakeRemoteReportRepository : RemoteReportRepository {
        override val isConfigured: Boolean = true
        var saveResult: Result<Unit> = Result.success(Unit)
        var deleteResult: Result<Unit> = Result.success(Unit)
        var savedSessionId: String? = null
        var deletedSessionId: String? = null

        override suspend fun saveReport(sessionId: String, report: DiagnosisReport): Result<Unit> {
            savedSessionId = sessionId
            return saveResult
        }

        override suspend fun deleteReport(sessionId: String): Result<Unit> {
            deletedSessionId = sessionId
            return deleteResult
        }
    }

    private companion object {
        const val SESSION_ID = "session-test"
        val REPORT = DiagnosisReport(
            bikeSummary = "City bike",
            initialProblem = "Catena rumorosa",
            checksPerformed = listOf("Controllo visivo"),
            possibleCauses = listOf("Lubrificazione"),
            safetyLevel = SafetyLevel.CAUTION,
            outcome = DiagnosisOutcome.YELLOW,
            usageRecommendation = "Verificare in ciclofficina"
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
