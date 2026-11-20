package com.facebookpostplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Horizontally scrolling row of square photo thumbnails for the composer:
 * each photo gets its own clearly bounded tile, so tall screenshots no
 * longer land at odd positions the way the collage laid them out. An
 * optional remove button sits on each tile. The remove button is a plain
 * clickable circle, not an IconButton — IconButton enforces a 48dp minimum
 * touch target that overflowed the tile and made the X look oversized.
 *
 * `dimAfter` dims every tile from that index on: a story uses one photo
 * only, so when the type is Story the extras are greyed out to show they
 * won't be used, while their remove buttons stay so the user can drop them.
 */
@Composable
fun MediaRow(
    uris: List<String>,
    isVideo: Boolean = false,
    onRemove: ((String) -> Unit)? = null,
    dimAfter: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    if (uris.isEmpty()) return
    val shape = RoundedCornerShape(12.dp)
    LazyRow(
        modifier = modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(uris) { index, uri ->
            Box(
                Modifier.size(120.dp).clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .alpha(if (index >= dimAfter) 0.35f else 1f)
            ) {
                AsyncImage(
                    model = uri, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
                if (isVideo) {
                    Box(
                        Modifier.align(Alignment.Center).size(44.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow, contentDescription = "Video",
                            tint = Color.White, modifier = Modifier.size(28.dp)
                        )
                    }
                }
                if (onRemove != null) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                CircleShape
                            )
                            .clickable { onRemove(uri) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close, contentDescription = "Remove photo",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
