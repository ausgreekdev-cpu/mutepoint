package com.mutepoint.app.presentation.analytics

import com.mutepoint.app.domain.model.SessionAnalytics
import com.mutepoint.app.domain.repository.AnalyticsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
private class FakeAnalyticsRepository(
    initial: List<SessionAnalytics> = emptyList(),
) : AnalyticsRepository {

    private val sessions = MutableStateFlow(initial)

    fun emit(value: List<SessionAnalytics>) {
        sessions.value = value
    }

    override fun observeSession(sessionId: String): Flow<SessionAnalytics?> =
        sessions.map { list -> list.find { it.sessionId == sessionId } }

    override fun observeRecentSessions(limit: Int): Flow<List<SessionAnalytics>> =
        sessions.mapLatest { it.take(limit) }

    override suspend fun startSession(sessionId: String, startedAtMs: Long) = Unit

    override suspend fun endSession(sessionId: String, endedAtMs: Long) = Unit

    override suspend fun recordSpeakingTime(sessionId: String, speakingMillis: Long) = Unit

    override suspend fun recordInterruption(
        sessionId: String,
        event: com.mutepoint.app.domain.model.InterruptionEvent,
    ) = Unit

    override suspend fun recentSessions(limit: Int): List<SessionAnalytics> =
        sessions.value.take(limit)

    override suspend fun deleteOlderThan(cutoffMs: Long) {
        sessions.value = sessions.value.filter { it.startedAtMs >= cutoffMs }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun session(
        id: String,
        startedAtMs: Long,
        speakingSec: Long = 30L,
        listeningSec: Long = 30L,
        interruptions: Int = 0,
        cutOffMs: Long = 0L,
    ) = SessionAnalytics(
        sessionId = id,
        startedAtMs = startedAtMs,
        totalSpeakingDurationSec = speakingSec,
        totalListeningDurationSec = listeningSec,
        timeCutOffMillis = cutOffMs,
        interruptionCount = interruptions,
        interruptions = emptyList(),
    )

    private val now = System.currentTimeMillis()
    private val dayMs = 24L * 60L * 60L * 1000L

    @Test
    fun `empty repository shows empty not loading state`() = runTest(testDispatcher) {
        val vm = AnalyticsViewModel(FakeAnalyticsRepository())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals(0, state.totalInterruptions)
    }

    @Test
    fun `default 7 day filter excludes sessions older than a week`() = runTest(testDispatcher) {
        val repo = FakeAnalyticsRepository(
            listOf(
                session("recent", startedAtMs = now - dayMs, interruptions = 2),
                session("old", startedAtMs = now - 40 * dayMs, interruptions = 9),
            ),
        )
        val vm = AnalyticsViewModel(repo)

        val state = vm.uiState.value
        assertEquals(listOf("recent"), state.sessions.map { it.sessionId })
        assertEquals(2, state.totalInterruptions)
    }

    @Test
    fun `ALL filter includes old sessions`() = runTest(testDispatcher) {
        val repo = FakeAnalyticsRepository(
            listOf(
                session("recent", startedAtMs = now - dayMs),
                session("old", startedAtMs = now - 40 * dayMs),
            ),
        )
        val vm = AnalyticsViewModel(repo)

        vm.setFilter(InsightsFilter.ALL)

        val state = vm.uiState.value
        assertEquals(InsightsFilter.ALL, state.filter)
        assertEquals(listOf("recent", "old"), state.sessions.map { it.sessionId })
    }

    @Test
    fun `aggregates average speaking percent and sum interruptions`() = runTest(testDispatcher) {
        val repo = FakeAnalyticsRepository(
            listOf(
                session("a", startedAtMs = now - dayMs, speakingSec = 75L, listeningSec = 25L, interruptions = 3),
                session("b", startedAtMs = now - 2 * dayMs, speakingSec = 25L, listeningSec = 75L, interruptions = 1),
            ),
        )
        val vm = AnalyticsViewModel(repo)

        val state = vm.uiState.value
        assertEquals(4, state.totalInterruptions)
        assertEquals(50f, state.speakingPercent, 0.01f)
        assertEquals(2, state.sessions.size)
    }

    @Test
    fun `interruption bars are oldest first matching chart layout`() = runTest(testDispatcher) {
        val repo = FakeAnalyticsRepository(
            listOf(
                session("newest", startedAtMs = now - dayMs, interruptions = 7),
                session("oldest", startedAtMs = now - 3 * dayMs, interruptions = 2),
            ),
        )
        val vm = AnalyticsViewModel(repo)

        assertEquals(listOf(2, 7), vm.uiState.value.interruptionBars)
    }

    @Test
    fun `selectSession emits navigation event`() = runTest(testDispatcher) {
        val vm = AnalyticsViewModel(FakeAnalyticsRepository())

        val emitted = kotlinx.coroutines.CompletableDeferred<String>()
        backgroundScope.launch { emitted.complete(vm.openSession.first()) }
        vm.selectSession("session-42")

        assertEquals("session-42", emitted.await())
    }
}
