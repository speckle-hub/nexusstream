package com.novastream.app.ui.screens.sources

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.novastream.app.data.ext.ExtensionHost
import com.novastream.app.data.integrations.SourceProbe
import com.novastream.app.data.integrations.SourceTester
import com.novastream.app.ui.components.EmptyState
import com.novastream.app.ui.components.GlassSurface
import com.novastream.app.ui.theme.AppSpacing
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.vm.LocalContainer
import com.novastream.app.ui.vm.collectAsStateSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Extension Source dashboard (Phase 12).
 *
 * Lists every installed dynamic extension with its version, an enable/disable switch and a "Test"
 * button that runs the extension's catalogue call and reports latency + item count, so a broken or
 * slow source is diagnosable in-app instead of only by reading logs.
 */
@Composable
fun SourcesScreen(nav: NavHostController) {
    val nova = LocalNovaColors.current
    val container = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val disabled by container.settings.disabledSources.collectAsStateSafe()
    // Never let a bad descriptor snapshot take the screen down on entry. `installed()` already
    // sanitizes per element through ExtensionCodec (and never throws); the extra filter + de-dupe
    // here is belt-and-braces so the lazy list can never receive a blank/duplicate key, and any
    // unexpected failure degrades to the empty state instead of an uncaught exception.
    val installed = remember {
        runCatching { container.extensionRepository.installed() }
            .getOrDefault(emptyList())
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
    }
    var probes by remember { mutableStateOf<Map<String, SourceProbe>>(emptyMap()) }
    var testing by remember { mutableStateOf<String?>(null) }

    fun setEnabled(id: String, enabled: Boolean) {
        scope.launch {
            val descriptor = installed.firstOrNull { it.id == id } ?: return@launch
            val list = disabled.toMutableList()
            if (enabled) {
                list.remove(id)
                withContext(Dispatchers.IO) { runCatching { ExtensionHost.load(descriptor, context.filesDir) } }
            } else {
                if (id !in list) list.add(id)
                ExtensionHost.unload(id)
            }
            container.settings.setDisabledSources(list)
        }
    }

    fun test(id: String) {
        scope.launch {
            testing = id
            val descriptor = installed.firstOrNull { it.id == id }
            val ext = ExtensionHost.get(id)
                ?: descriptor?.let { withContext(Dispatchers.IO) { runCatching { ExtensionHost.load(it, context.filesDir).getOrNull() }.getOrNull() } }
            val probe = if (ext == null) SourceProbe(id, false, 0, 0, "Extension is not loaded")
            else SourceTester.probe(ext)
            probes = probes + (id to probe)
            testing = null
            Toast.makeText(
                context,
                if (probe.ok) "${descriptor?.name ?: id}: ${probe.latencyMs} ms · ${probe.itemCount} items"
                else "${descriptor?.name ?: id}: ${probe.error}",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = AppSpacing.screen, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = nova.textPrimary)
            }
            Text("Sources", style = MaterialTheme.typography.headlineSmall, color = nova.textPrimary)
        }

        if (installed.isEmpty()) {
            EmptyState(
                "No extensions installed",
                "Install a source extension from the Add-on Manager to see it here.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = AppSpacing.screen, end = AppSpacing.screen, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(installed, key = { it.id }) { desc ->
                    val enabled = desc.id !in disabled
                    val probe = probes[desc.id]
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(desc.name, style = MaterialTheme.typography.titleMedium, color = nova.textPrimary)
                                    Text(
                                        "v${desc.version ?: "?"} · ${desc.id}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = nova.textTertiary,
                                    )
                                }
                                Switch(checked = enabled, onCheckedChange = { setEnabled(desc.id, it) })
                            }
                            Spacer(Modifier.padding(top = 6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                probe?.let { p ->
                                    val color = if (p.ok) Color(0xFF35E0A1) else MaterialTheme.colorScheme.error
                                    Icon(Icons.Filled.Speed, null, tint = color, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (p.ok) "${p.latencyMs} ms · ${p.itemCount} items" else "Unavailable — ${p.error}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = color,
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Button(onClick = { test(desc.id) }, enabled = testing != desc.id) {
                                    Text(if (testing == desc.id) "Testing…" else "Test")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
