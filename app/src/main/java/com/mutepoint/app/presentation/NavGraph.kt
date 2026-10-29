package com.mutepoint.app.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mutepoint.app.presentation.analytics.AnalyticsScreen
import com.mutepoint.app.presentation.analytics.AnalyticsViewModel
import com.mutepoint.app.presentation.analytics.SessionDetailScreen
import com.mutepoint.app.presentation.analytics.SessionDetailViewModel
import com.mutepoint.app.presentation.journal.RealityJournalScreen
import com.mutepoint.app.presentation.journal.RealityJournalViewModel
import com.mutepoint.app.presentation.practice.PracticeScreen
import com.mutepoint.app.presentation.practice.PracticeViewModel
import com.mutepoint.app.presentation.saferoom.RoomScreen
import com.mutepoint.app.presentation.saferoom.RoomViewModel
import com.mutepoint.app.presentation.security.VaultViewModel
import com.mutepoint.app.presentation.settings.SettingsScreen
import com.mutepoint.app.presentation.support.SupportScreen
import kotlinx.serialization.Serializable

@Serializable
data object JournalKey : NavKey

@Serializable
data object PracticeKey : NavKey

@Serializable
data object SafeRoomKey : NavKey

@Serializable
data object InsightsKey : NavKey

@Serializable
data class SessionDetailKey(val sessionId: String) : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object SupportKey : NavKey

private data class Tab(
    val key: NavKey,
    val title: String,
    val icon: ImageVector,
)

@Composable
fun MainAppScaffold() {
    val tabs = remember {
        listOf(
            Tab(JournalKey, "Journal", Icons.Default.Book),
            Tab(PracticeKey, "Practice", Icons.Default.Mic),
            Tab(SafeRoomKey, "Safe Rooms", Icons.Default.Groups),
            Tab(InsightsKey, "Insights", Icons.Default.Insights),
            Tab(SupportKey, "Support", Icons.Default.Shield),
        )
    }

    var currentTab by remember { mutableStateOf<NavKey>(tabs.first().key) }

    val journalBackStack = rememberNavBackStack(JournalKey)
    val practiceBackStack = rememberNavBackStack(PracticeKey)
    val safeRoomBackStack = rememberNavBackStack(SafeRoomKey)
    val insightsBackStack = rememberNavBackStack(InsightsKey)
    val supportBackStack = rememberNavBackStack(SupportKey)

    val activeBackStack = when (currentTab) {
        PracticeKey -> practiceBackStack
        SafeRoomKey -> safeRoomBackStack
        InsightsKey -> insightsBackStack
        SupportKey -> supportBackStack
        else -> journalBackStack
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = currentTab == tab.key,
                        onClick = { currentTab = tab.key },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavDisplay(
                backStack = activeBackStack,
                onBack = { activeBackStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<JournalKey> {
                        val viewModel: RealityJournalViewModel = hiltViewModel()
                        RealityJournalScreen(
                            viewModel = viewModel,
                            showBack = practiceBackStack.size > 1,
                            onBack = { practiceBackStack.removeLastOrNull() },
                        )
                    }
                    entry<PracticeKey> {
                        val viewModel: PracticeViewModel = hiltViewModel()
                        PracticeScreen(
                            viewModel = viewModel,
                            onNewJournal = { practiceBackStack.add(JournalKey) },
                        )
                    }
                    entry<SafeRoomKey> {
                        val viewModel: RoomViewModel = hiltViewModel()
                        val state by viewModel.batonState.collectAsStateWithLifecycle()
                        val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
                        val lastInterruption by viewModel.lastInterruption.collectAsStateWithLifecycle()
                        val liveStatus by viewModel.liveStatus.collectAsStateWithLifecycle()
                        RoomScreen(
                            batonState = state,
                            isMonitoring = isMonitoring,
                            lastInterruption = lastInterruption,
                            liveStatus = liveStatus,
                            onRequestBaton = viewModel::requestBaton,
                            onPassBaton = viewModel::passBaton,
                            onToggleMonitoring = viewModel::toggleMonitoring,
                            onJoinRoom = viewModel::joinRoom,
                            onLeaveRoom = viewModel::leaveRoom,
                        )
                    }
                    entry<InsightsKey> {
                        val viewModel: AnalyticsViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            viewModel.openSession.collect { sessionId ->
                                insightsBackStack.add(SessionDetailKey(sessionId))
                            }
                        }
                        AnalyticsScreen(
                            uiState = uiState,
                            onFilterSelected = viewModel::setFilter,
                            onSessionClick = viewModel::selectSession,
                        )
                    }
                    entry<SessionDetailKey> { key ->
                        val viewModel: SessionDetailViewModel = hiltViewModel()
                        LaunchedEffect(key.sessionId) {
                            viewModel.load(key.sessionId)
                        }
                        val session by viewModel.session.collectAsStateWithLifecycle()
                        SessionDetailScreen(
                            session = session,
                            onBack = { insightsBackStack.removeLastOrNull() },
                        )
                    }
                    entry<SupportKey> {
                        SupportScreen(
                            onOpenSettings = { supportBackStack.add(SettingsKey) },
                        )
                    }
                    entry<SettingsKey> {
                        // Activity-scoped: settings writes must reach the same
                        // VaultViewModel instance that MainActivity observes
                        // for FLAG_SECURE and relock behavior.
                        val activity = LocalActivity.current as ComponentActivity
                        val vaultViewModel: VaultViewModel =
                            hiltViewModel(viewModelStoreOwner = activity)
                        val relockPolicy by vaultViewModel.relockPolicy.collectAsStateWithLifecycle()
                        val flagSecure by vaultViewModel.flagSecure.collectAsStateWithLifecycle()
                        SettingsScreen(
                            relockPolicy = relockPolicy,
                            flagSecure = flagSecure,
                            onRelockChange = vaultViewModel::setRelockPolicy,
                            onFlagSecureChange = vaultViewModel::setFlagSecure,
                            onChangePin = vaultViewModel::changePin,
                            onBack = { supportBackStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}
