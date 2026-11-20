package com.facebookpostplanner.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.facebookpostplanner.data.local.PostType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composer: feed post / reel (single MP4) / story (photo in v1).
 * Text + photos, hashtags. Crop is done by the system photo picker / editor
 * before the URI reaches here — v1 does not ship its own cropper.
 * Schedule = a time today (hourly slots); Publish Now bypasses the queue.
 * Saving with no time = Draft.
 */
@Composable
fun CreateScreen(
    onDone: () -> Unit,
    vm: CreateViewModel = viewModel()
) {
    val type by vm.type.collectAsState()
    val saving by vm.saving.collectAsState()
    val error by vm.error.collectAsState()
    val done by vm.done.collectAsState()

    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf<Long?>(vm.defaultScheduleMillis()) }
    val media = remember { mutableStateListOf<Uri>() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> media.addAll(uris) }

    if (done) {
        onDone()
        vm.resetDone()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Create", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Row {
            PostType.entries.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { vm.setType(t) },
                    label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        OutlinedTextField(
            value = caption, onValueChange = { caption = it },
            label = { Text("Caption") }, modifier = Modifier.fillMaxWidth().height(140.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = hashtags, onValueChange = { hashtags = it },
            label = { Text("Hashtags (e.g. #travel #food)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            picker.launch(if (type == PostType.REEL) "video/*" else "image/*")
        }) { Text(if (type == PostType.REEL) "Pick video (MP4)" else "Pick photos") }
        if (media.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow {
                items(media) { uri ->
                    AsyncImage(
                        model = uri, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Scheduled: " + (schedule?.let {
                SimpleDateFormat("HH:mm, d MMM", Locale.getDefault()).format(Date(it))
            } ?: "Draft (no time)")
        )
        Row {
            OutlinedButton(onClick = { schedule = vm.defaultScheduleMillis() }) { Text("Next hour") }
            OutlinedButton(onClick = { schedule = null }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Save as draft")
            }
        }
        Spacer(Modifier.height(16.dp))
        error?.let { Text(it) }
        Button(
            onClick = { vm.save(caption, hashtags, media.toList(), schedule, publishNow = false) },
            enabled = !saving, modifier = Modifier.fillMaxWidth()
        ) { Text(if (schedule == null) "Save draft" else "Schedule") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.save(caption, hashtags, media.toList(), null, publishNow = true) },
            enabled = !saving, modifier = Modifier.fillMaxWidth()
        ) { Text("Publish now") }
    }
}
