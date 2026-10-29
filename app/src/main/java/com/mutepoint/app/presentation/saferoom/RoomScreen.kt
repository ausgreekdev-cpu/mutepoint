package com.mutepoint.app.presentation.saferoom

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PanToolAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mutepoint.app.R
import com.mutepoint.app.data.livekit.LiveKitStatus
import com.mutepoint.app.domain.model.BatonState
import com.mutepoint.app.domain.model.InterruptionEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    batonState: BatonState,
    isMonitoring: Boolean,
    lastInterruption: InterruptionEvent?,
    liveStatus: LiveKitStatus,
    onRequestBaton: () -> Unit,
    onPassBaton: () -> Unit,
    onToggleMonitoring: () -> Unit,
    onJoinRoom: () -> Unit,
    onLeaveRoom: () -> Unit,
) {
    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onJoinRoom()
    }
    val requestJoin = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onJoinRoom()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mute Point • Room 01", fontWeight = FontWeight.Bold)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Rule: Strict Single Speaker",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Intermittent Mute Enforced",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    LiveStatusChip(status = liveStatus)
                }
            }

            when (liveStatus) {
                is LiveKitStatus.Connected -> {
                    OutlinedButton(onClick = onLeaveRoom, modifier = Modifier.fillMaxWidth()) {
                        Text("Leave room", fontSize = 14.sp)
                    }
                }
                is LiveKitStatus.Connecting -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Connecting…", fontSize = 14.sp)
                    }
                }
                else -> {
                    Button(onClick = requestJoin, modifier = Modifier.fillMaxWidth()) {
                        Text("Join room", fontSize = 14.sp)
                    }
                }
            }

            MonitoringControls(
                isMonitoring = isMonitoring,
                onToggleMonitoring = onToggleMonitoring,
            )

            lastInterruption?.let { event ->
                InterruptionBanner(event = event)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (batonState) {
                    is BatonState.HoldingBaton -> {
                        ActiveSpeakerPulseRing(timeRemainingSec = batonState.timeRemainingSeconds)
                    }
                    is BatonState.Listening -> {
                        ListeningRing(speakerName = batonState.activeSpeakerName)
                    }
                    is BatonState.InQueue -> {
                        QueueRing(position = batonState.position)
                    }
                    is BatonState.Idle -> {
                        IdleRing()
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = batonState,
                    label = "ButtonState",
                ) { state ->
                    when (state) {
                        is BatonState.HoldingBaton -> {
                            Button(
                                onClick = onPassBaton,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Icon(Icons.Default.PanToolAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pass the Baton Early", fontSize = 16.sp)
                            }
                        }
                        is BatonState.InQueue -> {
                            OutlinedButton(
                                onClick = onPassBaton,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Text("Leave Queue (Pos #${state.position})", fontSize = 16.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onRequestBaton,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Icon(Icons.Default.PanTool, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Raise Hand to Speak", fontSize = 16.sp)
                            }
                        }
                    }
                }

                Text(
                    text = if (batonState is BatonState.HoldingBaton) {
                        "You have the floor. Microphone is ACTIVE."
                    } else {
                        "Microphone is HARD MUTED to prevent interjection."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun ActiveSpeakerPulseRing(timeRemainingSec: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp * pulseScale)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${timeRemainingSec}s",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "YOU ARE SPEAKING",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ListeningRing(speakerName: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .border(4.dp, MaterialTheme.colorScheme.secondary, CircleShape),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MicOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                speakerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Has the floor", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QueueRing(position: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .border(4.dp, MaterialTheme.colorScheme.tertiary, CircleShape),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "#$position",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("In Queue", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun IdleRing() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    ) {
        Text(
            "Floor is Open",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun LiveStatusChip(status: LiveKitStatus) {
    val (label, container, content) = when (status) {
        is LiveKitStatus.Connected -> Triple(
            if (status.participantCount > 1) {
                "LIVE • ${status.participantCount}"
            } else {
                "LIVE"
            },
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is LiveKitStatus.Connecting -> Triple(
            "CONNECTING",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is LiveKitStatus.Disconnected, is LiveKitStatus.Error -> Triple(
            "OFFLINE",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(color = container, shape = CircleShape) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun MonitoringControls(
    isMonitoring: Boolean,
    onToggleMonitoring: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (isMonitoring) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = CircleShape,
        ) {
            Text(
                text = stringResource(
                    if (isMonitoring) R.string.monitor_active_chip else R.string.monitor_idle_chip,
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (isMonitoring) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        OutlinedButton(onClick = onToggleMonitoring) {
            Icon(
                imageVector = if (isMonitoring) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(
                    if (isMonitoring) R.string.monitor_stop else R.string.monitor_start,
                ),
            )
        }
    }
}

@Composable
private fun InterruptionBanner(event: InterruptionEvent) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.monitor_interruption_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(
                    R.string.monitor_interruption_banner,
                    event.durationMillis / 1000.0f,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
