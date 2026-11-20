package com.facebookpostplanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.facebookpostplanner.data.local.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun PostCard(
    post: Post,
    onPublishNow: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Text(timeFmt.format(Date(post.scheduledAtMillis ?: post.createdAtMillis)))
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text(post.type.name) })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text(post.status.name) })
            }
            Spacer(Modifier.height(6.dp))
            Text(post.caption, maxLines = 3)
            if (post.hashtags.isNotBlank()) {
                Text(post.hashtags, maxLines = 1)
            }
            if (post.mediaList.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow {
                    items(post.mediaList) { uri ->
                        AsyncImage(
                            model = uri, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(96.dp)
                                .height(96.dp)
                                .padding(end = 6.dp)
                        )
                    }
                }
            }
            post.failureReason?.let {
                Text("Failed: $it", maxLines = 2)
                onRetry?.let { retry -> TextButton(onClick = retry) { Text("Retry") } }
            }
            Row {
                onPublishNow?.let {
                    TextButton(onClick = it) { Text("Publish now") }
                }
                onDelete?.let {
                    TextButton(onClick = it) { Text("Delete") }
                }
            }
        }
    }
}
