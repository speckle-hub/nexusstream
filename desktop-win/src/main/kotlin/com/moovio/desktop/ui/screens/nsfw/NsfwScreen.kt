package com.moovio.desktop.ui.screens.nsfw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moovio.desktop.data.repo.CatalogRow
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.PlayTarget
import com.moovio.desktop.security.PinHasher
import com.moovio.desktop.ui.components.EmptyState
import com.moovio.desktop.ui.components.GhostButton
import com.moovio.desktop.ui.components.LoadingIndicator
import com.moovio.desktop.ui.components.MediaRow
import com.moovio.desktop.ui.components.PosterCard
import com.moovio.desktop.ui.components.PrimaryButton
import com.moovio.desktop.ui.components.SearchField
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.nav.Screen
import com.moovio.desktop.ui.screens.settings.DarkTextField
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun NsfwScreen(app: AppState, container: AppContainer, onOpen: (MediaItem) -> Unit) {
    val settings by container.settings.state.collectAsState()
    val unlocked = container.isNsfwUnlocked

    when {
        !settings.hasNsfwPin -> NsfwPinSetup(app, container)
        !unlocked -> NsfwPinGate(app, container)
        else -> NsfwContent(app, container, onOpen)
    }
}

@Composable
private fun NsfwPinGate(app: AppState, container: AppContainer) {
    val settings by container.settings.state.collectAsState()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }

    GateLayout(
        icon = Icons.Rounded.Lock,
        title = "Private area",
        message = "This section is PIN-locked and kept out of search, home and your library. " +
            "Enter your PIN to continue.",
    ) {
        DarkTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
            placeholder = "PIN",
            password = true,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.height(10.dp))
        error?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = Palette.danger)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Unlock", Icons.Rounded.LockOpen, enabled = pin.length >= 4, onClick = {
            val valid = PinHasher.verify(pin, settings.nsfwPinSalt, settings.nsfwPinHash)
            if (valid) {
                container.unlockNsfw()
                app.showToast("Private area unlocked for this session")
            } else {
                attempts += 1
                error = "Incorrect PIN.${if (attempts >= 3) " Take your time." else ""}"
                pin = ""
            }
        })
        Spacer(Modifier.height(14.dp))
        Text(
            "Locked again automatically when Moovio closes.",
            style = MaterialTheme.typography.labelSmall,
            color = Palette.textTertiary,
        )
    }
}

@Composable
private fun NsfwPinSetup(app: AppState, container: AppContainer) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = PinHasher.isValidPin(pin) && pin == confirm

    GateLayout(
        icon = Icons.Rounded.VisibilityOff,
        title = "Set up the private area",
        message = "Create a 4–8 digit PIN. It is salted and hashed with PBKDF2 before being written " +
            "to disk, and the NSFW section stays hidden until it is unlocked.",
    ) {
        DarkTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
            placeholder = "New PIN",
            password = true,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.height(10.dp))
        DarkTextField(
            value = confirm,
            onValueChange = { confirm = it.filter(Char::isDigit).take(8); error = null },
            placeholder = "Confirm PIN",
            password = true,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.height(10.dp))
        when {
            pin.isNotEmpty() && !PinHasher.isValidPin(pin) ->
                Text("Use 4–8 digits.", style = MaterialTheme.typography.labelSmall, color = Palette.warning)
            confirm.isNotEmpty() && pin != confirm ->
                Text("PINs do not match.", style = MaterialTheme.typography.labelSmall, color = Palette.danger)
            error != null ->
                Text(error!!, style = MaterialTheme.typography.labelSmall, color = Palette.danger)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Set PIN & unlock", Icons.Rounded.LockOpen, enabled = valid, onClick = {
            runCatching {
                val salt = PinHasher.newSalt()
                val hash = PinHasher.hash(pin, salt)
                container.settings.update { it.copy(nsfwPinHash = hash, nsfwPinSalt = salt) }
                container.unlockNsfw()
            }.onFailure { error = it.message ?: "Could not save PIN" }
        })
        Spacer(Modifier.height(14.dp))
        Text(
            "You can change or remove the PIN later in Settings → Privacy.",
            style = MaterialTheme.typography.labelSmall,
            color = Palette.textTertiary,
        )
    }
}

@Composable
private fun GateLayout(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        listOf(AccentColor.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .glass(RoundedCornerShape(24.dp), alpha = 0.07f)
                .background(Palette.surface.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
                .padding(38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .background(AccentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(25.dp), tint = AccentColor)
            }
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Palette.textPrimary)
            Spacer(Modifier.height(9.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            content()
        }
    }
}

@Composable
private fun NsfwContent(app: AppState, container: AppContainer, onOpen: (MediaItem) -> Unit) {
    val addons by container.addonStore.state.collectAsState()
    val adultAddons = addons.addons.filter { it.adult && it.enabled }
    var rows by remember { mutableStateOf<List<CatalogRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(adultAddons.map { it.id }) {
        loading = true
        rows = withContext(Dispatchers.IO) {
            runCatching { container.catalog.addonRows(includeAdult = true) }
                .getOrDefault(emptyList())
                .filter { row -> row.items.any { it.adult } }
        }
        loading = false
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(340)
        results = withContext(Dispatchers.IO) {
            runCatching {
                container.catalog.search(query, includeAdult = true, adultOnly = true).first
            }.getOrDefault(emptyList())
        }
        searching = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 48.dp, end = 40.dp, top = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = AccentColor)
                    Text(
                        "PRIVATE AREA",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("NSFW", style = MaterialTheme.typography.headlineLarge, color = Palette.textPrimary)
                Text(
                    "Isolated from the rest of Moovio · unlocked for this session only",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.textTertiary,
                )
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search private add-ons…",
                modifier = Modifier.width(280.dp),
            )
            Spacer(Modifier.width(10.dp))
            GhostButton("Lock", Icons.Rounded.Lock, onClick = {
                container.lockNsfw()
                app.showToast("Private area locked")
            })
        }

        Spacer(Modifier.height(20.dp))

        when {
            query.isNotBlank() -> {
                if (searching && results.isEmpty()) {
                    LoadingIndicator(Modifier.fillMaxSize())
                } else if (results.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Nothing found",
                        message = "No private add-on returned a result for “$query”.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(156.dp),
                        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.key }) { item ->
                            PosterCard(
                                item = item,
                                width = 150.dp,
                                onClick = { onOpen(item) },
                                onPlay = { app.requestPlay(PlayTarget(media = item)) },
                            )
                        }
                    }
                }
            }

            loading -> LoadingIndicator(Modifier.fillMaxSize())

            adultAddons.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Lock,
                title = "No private add-ons yet",
                message = "The private area only shows add-ons you explicitly mark as NSFW. " +
                    "Add one in Settings → Add-ons, then switch on “NSFW-only” for it.",
                actionLabel = "Open Add-ons",
                onAction = { app.navigateTab(Screen.Settings) },
                modifier = Modifier.fillMaxSize(),
            )

            rows.isEmpty() -> EmptyState(
                icon = Icons.Rounded.ArrowForward,
                title = "Nothing to show",
                message = "Your private add-ons didn't return any catalogs. Try searching above, or check the add-on in Settings.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 56.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(rows, key = { it.id }) { row ->
                    MediaRow(
                        title = row.title,
                        items = row.items,
                        onOpen = onOpen,
                        onPlay = { app.requestPlay(PlayTarget(media = it)) },
                        cardWidth = 160.dp,
                        modifier = Modifier.padding(bottom = 26.dp),
                    )
                }
            }
        }
    }
}
