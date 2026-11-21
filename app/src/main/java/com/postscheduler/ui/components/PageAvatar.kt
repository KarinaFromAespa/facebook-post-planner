package com.postscheduler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * A Page's profile picture, cached in memory only until the app closes.
 * The picture URL comes from /me/accounts (no extra API call) and the image
 * itself is a CDN download, not a Graph API call, so it costs no rate
 * limit. Coil's disk cache is disabled here so the image lives only in
 * memory; without a picture URL it falls back to the initial letter.
 */
@Composable
fun PageAvatar(
    pictureUrl: String?,
    name: String,
    size: Dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    if (pictureUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(pictureUrl)
                .diskCachePolicy(CachePolicy.DISABLED)
                .crossfade(true)
                .build(),
            contentDescription = "$name profile picture",
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(size).background(containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (name.firstOrNull() ?: '?').toString(),
                color = contentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
