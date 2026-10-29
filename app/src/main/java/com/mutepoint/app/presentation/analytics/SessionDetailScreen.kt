package com.mutepoint.app.presentation.analytics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mutepoint.app.R
import com.mutepoint.app.domain.model.InterruptionEvent
import com.mutepoint.app.domain.model.SessionAnalytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: SessionAnalytics?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = session?.let { formatDate(it.startedAtMs) }
                            ?: stringResource(R.string.detail_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                actions = {
                    if (session != null) {
                        val context = LocalContext.current
                        val shareTitle = stringResource(R.string.detail_share)
                        IconButton(
                            onClick = {
                                val text = formatSessionExport(session)
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        send,
                                        shareTitle,
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.detail_share),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.insights_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SpeakingSplitBar(session = session)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailStatCard(
                        title = stringResource(R.string.insights_speaking),
                        value = "%.0f%%".format(session.speakingPercent),
                        modifier = Modifier.weight(1f),
                    )
                    DetailStatCard(
                        title = stringResource(R.string.insights_cutoff),
                        value = formatDuration(session.timeCutOffMillis),
                        modifier = Modifier.weight(1f),
                    )
                    DetailStatCard(
                        title = stringResource(R.string.insights_interruptions_total),
                        value = session.interruptionCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.detail_timeline_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (session.interruptions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.detail_no_interruptions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                itemsIndexed(session.interruptions) { index, event ->
                    InterruptionRow(
                        index = index,
                        event = event,
                        sessionStartMs = session.startedAtMs,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeakingSplitBar(session: SessionAnalytics) {
    val speakingSec = session.totalSpeakingDurationSec
    val listeningSec = session.totalListeningDurationSec
    val total = (speakingSec + listeningSec).coerceAtLeast(1L)
    val speakingFraction = speakingSec.toFloat() / total

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_split_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .weight(speakingFraction.coerceIn(0.05f, 1f))
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                )
                Box(
                    modifier = Modifier
                        .weight((1f - speakingFraction).coerceIn(0.05f, 1f))
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.detail_speaking_time,
                        formatDuration(speakingSec * 1000L),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.detail_session_length,
                        formatDuration(total * 1000L),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun DetailStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InterruptionRow(
    index: Int,
    event: InterruptionEvent,
    sessionStartMs: Long,
) {
    val offsetMs = (event.timestamp.toEpochMilli() - sessionStartMs).coerceAtLeast(0L)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.detail_event_offset,
                        formatOffset(offsetMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.detail_event_meta,
                        formatDuration(event.durationMillis),
                        event.speakerVolumeDb.toInt(),
                        event.interruptionVolumeDb.toInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

