package com.facebookpostplanner.ui.insights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Insights, earnings-first.
 * If the Page is not enrolled in Content Monetization, the whole earnings
 * section shows one 'Not monetized' message with the criteria — never
 * per-post $0. Engagement / best-performer ranking still shows.
 */
@Composable
fun InsightsScreen(vm: InsightsViewModel = viewModel()) {
    val insights by vm.localInsights.collectAsState(initial = emptyList())
    val published by vm.published.collectAsState(initial = emptyList())
    val monetized by vm.monetized.collectAsState()
    val criteria by vm.criteria.collectAsState()
    val totalEarnings by vm.totalEarnings.collectAsState()

    val ranked = insights.sortedByDescending { it.earningsUsd ?: -1.0 }
    val bestByEngagement = insights.sortedByDescending { it.engagement }.firstOrNull()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Card(Modifier.padding(bottom = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Earnings", style = MaterialTheme.typography.titleMedium)
                when (monetized) {
                    null -> Text("Checking monetization status…")
                    false -> {
                        Text("Not monetized")
                        criteria?.let { Text(it) }
                    }
                    true -> Text("Total approx. earnings: \$${"%.2f".format(totalEarnings ?: 0.0)}")
                }
            }
        }

        Text("Best performing", style = MaterialTheme.typography.titleMedium)
        bestByEngagement?.let {
            val post = published.firstOrNull { p -> p.id == it.postId }
            Text("${post?.caption?.take(60) ?: it.postId} — ${it.engagement} engagements, ${it.views} views")
        } ?: Text("No published posts with insights yet.")
        Spacer(Modifier.height(12.dp))

        Text("Per-post", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(ranked) { row ->
                val post = published.firstOrNull { p -> p.id == row.postId }
                Card(Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(post?.caption?.take(80) ?: row.postId, maxLines = 2)
                        Text("Likes ${row.likes} · Comments ${row.comments} · Shares ${row.shares} · Views ${row.views}")
                        // Earnings per post only when monetized; otherwise omit, don't show $0.
                        if (monetized == true) {
                            Text("Approx. earnings: \$${"%.2f".format(row.earningsUsd ?: 0.0)}")
                        }
                    }
                }
            }
        }
    }
}
