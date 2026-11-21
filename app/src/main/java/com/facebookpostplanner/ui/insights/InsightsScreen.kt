package com.facebookpostplanner.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Insights: approx total earnings for the current month only.
 * No per-post earnings, no performance, no shares/likes/comments — the
 * backend makes just 2 Graph API calls for this, so it costs almost no
 * rate limit. If the Page is not enrolled in Content Monetization, the
 * card shows one 'Not monetized' message with the criteria.
 */
@Composable
fun InsightsScreen(vm: InsightsViewModel = viewModel()) {
    val monetized by vm.monetized.collectAsState()
    val criteria by vm.criteria.collectAsState()
    val totalEarnings by vm.totalEarnings.collectAsState()

    // Re-fetch every time the tab is opened, so switching Pages shows the
    // newly selected Page's earnings instead of a stale total.
    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Insights", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { vm.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Approx. total earnings this month",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                when (monetized) {
                    null -> Text("Checking monetization status…")
                    false -> {
                        Text(
                            "Not monetized",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        criteria?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    true -> Text(
                        "\$${"%.2f".format(totalEarnings ?: 0.0)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
