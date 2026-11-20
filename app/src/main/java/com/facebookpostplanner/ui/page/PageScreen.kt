package com.facebookpostplanner.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Connect a Facebook Page.
 * The Page access token lives on the backend (env var) — the app never sees
 * it. This screen only picks which Page posts go to. If the backend has no
 * Page configured yet, a Page can be added here by ID; the backend still
 * needs its token before anything can publish.
 */
@Composable
fun PageScreen(vm: PageViewModel = viewModel()) {
    val pages by vm.pages.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val selectedId by vm.selectedPageId.collectAsState()
    val selectedName by vm.selectedPageName.collectAsState()
    val backendUrl by vm.backendUrl.collectAsState()

    var manualId by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var url by remember(backendUrl) { mutableStateOf(backendUrl) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Connect Page", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Posting to", style = MaterialTheme.typography.titleMedium)
                if (selectedId != null) {
                    Text(selectedName ?: selectedId!!, style = MaterialTheme.typography.bodyLarge)
                    Text("Page ID: $selectedId", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.disconnect() }) { Text("Disconnect") }
                } else {
                    Text("No Page connected yet. Pick one below, or add it by ID.")
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Pages on the backend", style = MaterialTheme.typography.titleMedium)
        if (loading) {
            Text("Loading…")
        } else if (pages.isEmpty()) {
            Text(
                "No Pages returned by the backend. Either the backend has no Page token configured yet, or it is unreachable — check the backend URL below, or add the Page by ID.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            pages.forEach { page ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = page.id == selectedId,
                        onClick = { vm.selectPage(page) },
                        role = Role.RadioButton
                    ).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = page.id == selectedId,
                        onClick = { vm.selectPage(page) }
                    )
                    Column {
                        Text(page.name)
                        Text("ID: ${page.id}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.refresh() }) { Text("Refresh pages") }
        Spacer(Modifier.height(16.dp))

        Text("Add a Page by ID", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = manualId, onValueChange = { manualId = it },
            label = { Text("Page ID") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = manualName, onValueChange = { manualName = it },
            label = { Text("Page name (optional)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            vm.addPageManually(manualId, manualName)
            manualId = ""
            manualName = ""
        }) { Text("Connect this Page") }
        Spacer(Modifier.height(16.dp))

        Text("Backend", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.saveBackendUrl(url) }) { Text("Save backend URL") }
        Spacer(Modifier.height(12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
