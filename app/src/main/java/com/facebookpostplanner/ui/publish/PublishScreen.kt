package com.facebookpostplanner.ui.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.facebookpostplanner.ui.components.PostCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val slotFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

/**
 * Publish tab = home. Buffer-style Today timeline: each hourly slot gets a
 * time pin (dot + time), then its cards. Only slots that actually have a
 * scheduled post are shown — empty hours are not listed. Multiple items
 * can share a slot — each keeps its own status/failure. Drafts are shown
 * in a separate section below the queue.
 */
@Composable
fun PublishScreen(
    onCreate: () -> Unit,
    vm: PublishViewModel = viewModel()
) {
    val queue by vm.queue.collectAsState(initial = emptyList())
    val drafts by vm.drafts.collectAsState(initial = emptyList())
    val paused by vm.paused.collectAsState()
    val error by vm.error.collectAsState()
    val slots = vm.todaySlots()

    // Pull the latest queue every time this tab is shown, so posts scheduled
    // elsewhere (website, or after this screen was first opened) appear.
    LaunchedEffect(Unit) { vm.refresh() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Today", style = MaterialTheme.typography.headlineMedium)
                Text(
                    dateFmt.format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { vm.togglePause() }) {
                Icon(
                    if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(if (paused) "Resume queue" else "Pause queue")
            }
        }
        if (paused) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Queue paused — nothing will publish until you resume it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        error?.let {
            Text(
                "Backend unreachable: $it — showing the local queue.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // A post belongs to a slot when it falls in that slot's hour on the
        // same day — matching by exact HH:mm hid every post that wasn't
        // scheduled exactly on the hour (e.g. 14:15 never matched 14:00).
        val hourFmt = SimpleDateFormat("yyyy-DDD-HH", Locale.getDefault())
        val slotsWithPosts = slots.map { slot ->
            slot to queue.filter {
                it.scheduledAtMillis?.let { t ->
                    hourFmt.format(Date(t)) == hourFmt.format(Date(slot))
                } == true
            }
        }.filter { it.second.isNotEmpty() }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (slotsWithPosts.isEmpty() && drafts.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth, contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Nothing else is queued up",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + Schedule a post to line up the day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onCreate) { Text("+ Schedule a post") }
                    }
                }
            }
            items(slotsWithPosts) { (slot, atSlot) ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp).background(
                                MaterialTheme.colorScheme.primary, CircleShape
                            )
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            slotFmt.format(Date(slot)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (atSlot.size > 1) {
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "${atSlot.size} posts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    atSlot.forEach { post ->
                        PostCard(
                            post = post,
                            onPublishNow = { vm.publishNow(post) },
                            onDelete = { vm.delete(post) },
                            onRetry = { vm.publishNow(post) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            if (drafts.isNotEmpty()) {
                item {
                    Text("Drafts", style = MaterialTheme.typography.titleMedium)
                }
                items(drafts) { post ->
                    PostCard(post = post, onDelete = { vm.delete(post) })
                }
            }
        }
    }
}
