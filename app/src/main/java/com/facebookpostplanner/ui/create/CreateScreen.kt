package com.facebookpostplanner.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.facebookpostplanner.data.local.PostType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Composer: feed post / reel (single MP4) / story (photo in v1).
 * Text + photos, hashtags. Crop is done by the system photo picker / editor
 * before the URI reaches here — v1 does not ship its own cropper.
 * Schedule = a time today (hourly slots); Publish Now bypasses the queue.
 * Saving with no time = Draft.
 *
 * Media previews are fixed-size thumbnails with a remove button, and the
 * whole screen scrolls, so the Schedule / Publish now buttons are always
 * reachable no matter how many photos are picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onDone: () -> Unit,
    onConnectPage: () -> Unit,
    vm: CreateViewModel = viewModel()
) {
    val type by vm.type.collectAsState()
    val saving by vm.saving.collectAsState()
    val error by vm.error.collectAsState()
    val done by vm.done.collectAsState()
    val pageName by vm.pageName.collectAsState()

    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf<Long?>(vm.defaultScheduleMillis()) }
    val media = remember { mutableStateListOf<Uri>() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }
    var scheduleError by remember { mutableStateOf<String?>(null) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = schedule ?: System.currentTimeMillis()
    )
    val context = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = 0,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> media.addAll(uris) }

    if (done) {
        onDone()
        vm.resetDone()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Create", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Posting to", style = MaterialTheme.typography.labelMedium)
                    Text(
                        pageName ?: "No Page connected",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                TextButton(onClick = onConnectPage) {
                    Text(if (pageName == null) "Connect" else "Change")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Type", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
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
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = caption, onValueChange = { caption = it },
            label = { Text("Caption") }, modifier = Modifier.fillMaxWidth().height(140.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = hashtags, onValueChange = { hashtags = it },
            label = { Text("Hashtags (e.g. #travel #food)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Text("Media", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = {
            picker.launch(if (type == PostType.REEL) "video/*" else "image/*")
        }) { Text(if (type == PostType.REEL) "Pick video (MP4)" else "Pick photos") }
        if (media.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow {
                items(media, key = { it.toString() }) { uri ->
                    Box(
                        Modifier.size(96.dp).padding(end = 6.dp)
                    ) {
                        AsyncImage(
                            model = uri, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp))
                        )
                        IconButton(
                            onClick = { media.remove(uri) },
                            modifier = Modifier.align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo")
                        }
                    }
                }
            }
            Text(
                "${media.size} selected — tap × on a photo to remove it.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))

        Text("Schedule", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Scheduled: " + (schedule?.let {
                SimpleDateFormat("HH:mm, d MMM", Locale.getDefault()).format(Date(it))
            } ?: "Draft (no time)")
        )
        Spacer(Modifier.height(6.dp))
        Row {
            OutlinedButton(onClick = { showDatePicker = true }) { Text("Pick date & time") }
            OutlinedButton(onClick = { schedule = vm.defaultScheduleMillis() }, modifier = Modifier.padding(start = 8.dp)) { Text("Next hour") }
            OutlinedButton(onClick = { schedule = null }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Save as draft")
            }
        }
        scheduleError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { vm.save(caption, hashtags, media.toList(), schedule, publishNow = false) },
            enabled = !saving, modifier = Modifier.fillMaxWidth()
        ) { Text(if (schedule == null) "Save draft" else "Schedule") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { vm.save(caption, hashtags, media.toList(), null, publishNow = true) },
            enabled = !saving, modifier = Modifier.fillMaxWidth()
        ) { Text("Publish now") }
        Spacer(Modifier.height(8.dp))
        if (pageName == null) {
            Text(
                "Posts save locally without a Page, but nothing can publish until one is connected.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = dateState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dateState) }
    }
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val day = pickedDateMillis ?: System.currentTimeMillis()
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = day
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val picked = cal.timeInMillis
                    if (picked < System.currentTimeMillis() + 2 * 60 * 1000) {
                        scheduleError = "Pick a time at least 2 minutes ahead."
                    } else {
                        schedule = picked
                        scheduleError = null
                    }
                    showTimePicker = false
                }) { Text("Set time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
