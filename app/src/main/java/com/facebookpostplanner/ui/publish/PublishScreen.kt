package com.facebookpostplanner.ui.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.facebookpostplanner.ui.components.PostCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val slotFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

/**
 * Publish tab = home. Today list grouped by hourly slot.
 * Only slots that actually have a scheduled post are shown — empty hours
 * are not listed. Multiple items can share a slot — each keeps its own
 * status/failure. Drafts are shown in a separate section below the queue.
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Today", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = { vm.togglePause() }) {
                Text(if (paused) "Resume queue" else "Pause queue")
            }
        }
        if (paused) Text("Queue paused — nothing will publish until you resume it.")
        error?.let {
            Text("Backend unreachable: $it — showing the local queue.")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("+ Schedule a post") }
        Spacer(Modifier.height(12.dp))

        // Only slots with at least one scheduled post get a section.
        val slotsWithPosts = slots.map { slot ->
            slot to queue.filter {
                it.scheduledAtMillis?.let { t -> slotFmt.format(Date(t)) == slotFmt.format(Date(slot)) } == true
            }
        }.filter { it.second.isNotEmpty() }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (slotsWithPosts.isEmpty() && drafts.isEmpty()) {
                item {
                    Text(
                        "Nothing scheduled yet — tap \"+ Schedule a post\" to line up the day.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(slotsWithPosts) { (slot, atSlot) ->
                Text(slotFmt.format(Date(slot)), style = MaterialTheme.typography.titleMedium)
                atSlot.forEach { post ->
                    PostCard(
                        post = post,
                        onPublishNow = { vm.publishNow(post) },
                        onDelete = { vm.delete(post) },
                        onRetry = { vm.publishNow(post) }
                    )
                }
            }
            if (drafts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Drafts", style = MaterialTheme.typography.titleMedium)
                }
                items(drafts) { post ->
                    PostCard(post = post, onDelete = { vm.delete(post) })
                }
            }
        }
    }
}
