package com.facebookpostplanner.ui.page

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.facebook.CallbackManager

/**
 * Connect a Facebook Page — the standard PostPlanner/Buffer way: sign in
 * with Facebook, grant the Page permissions, and pick one of the Pages the
 * account manages. The Page's token goes to the backend so the server can
 * publish as that Page.
 */
@Composable
fun PageScreen(
    vm: PageViewModel = viewModel(),
    callbackManager: CallbackManager? = null
) {
    val pages by vm.pages.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val selectedId by vm.selectedPageId.collectAsState()
    val selectedName by vm.selectedPageName.collectAsState()
    val backendUrl by vm.backendUrl.collectAsState()
    val facebookPages by vm.facebookPages.collectAsState()
    val fbStatus by vm.fbStatus.collectAsState()
    val fbLoggingIn by vm.fbLoggingIn.collectAsState()

    var url by remember(backendUrl) { mutableStateOf(backendUrl) }
    val activity = LocalContext.current as Activity
    val hasConnectedPage = selectedId != null || pages.isNotEmpty()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Connect Page", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).background(
                        MaterialTheme.colorScheme.primary, CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (selectedName?.firstOrNull() ?: '?').toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Posting to", style = MaterialTheme.typography.labelMedium)
                    if (selectedId != null) {
                        Text(
                            selectedName ?: selectedId!!,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Page ID: $selectedId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("No Page connected yet.")
                    }
                }
                if (selectedId != null) {
                    OutlinedButton(onClick = { vm.disconnect() }) { Text("Disconnect") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Text(if (hasConnectedPage) "Connect more pages" else "Connect with Facebook", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Sign in with the Facebook account that manages the Page. The app lists your Pages — pick one and its token goes to the backend.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (callbackManager != null) vm.startFacebookLogin(activity, callbackManager)
            },
            enabled = callbackManager != null && !fbLoggingIn,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (fbLoggingIn) "Signing in…" else if (hasConnectedPage) "Connect more pages" else "Connect with Facebook") }
        fbStatus?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        if (facebookPages.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            facebookPages.forEach { page ->
                PageRow(
                    name = page.name,
                    id = page.id,
                    selected = page.id == selectedId,
                    onClick = { vm.selectFacebookPage(page) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Connected Pages", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (loading) {
            Text("Loading…")
        } else if (pages.isEmpty()) {
            Text(
                "No Pages returned by the backend. Either the backend has no Page token configured yet, or it is unreachable — check the backend URL below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            pages.forEach { page ->
                PageRow(
                    name = page.name,
                    id = page.id,
                    selected = page.id == selectedId,
                    onClick = { vm.selectPage(page) },
                    onRemove = { vm.removeBackendPage(page.id) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.refresh() }) { Text("Refresh pages") }
        Spacer(Modifier.height(20.dp))

        Text("Backend", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.saveBackendUrl(url) }) { Text("Save backend URL") }
        Spacer(Modifier.height(12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PageRow(
    name: String,
    id: String,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().selectable(
            selected = selected, onClick = onClick, role = Role.RadioButton
        ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.size(36.dp).background(
                MaterialTheme.colorScheme.primaryContainer, CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium)
            Text(
                "ID: $id", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Remove this Page from the backend. Without it, a backend Page
        // can only be removed by selecting it first and using Disconnect
        // — and after clearing the app's data there is no selected Page
        // at all, so the backend Pages just keep coming back.
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove $name from the backend"
                )
            }
        }
    }
}
