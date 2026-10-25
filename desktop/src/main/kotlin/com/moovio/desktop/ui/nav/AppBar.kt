package com.moovio.desktop.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.AccentViolet
import com.moovio.desktop.ui.theme.MoovioSurface
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors

val AppBarHeight = 56.dp

@Composable
fun AppBar(
    onMenuClick: () -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldQuery by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppBarHeight)
            .background(MoovioSurface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Menu,
            contentDescription = "Menu",
            modifier = Modifier.size(20.dp).clickable { onMenuClick() },
            tint = MoovioTextPrimary,
        )

        Text(
            text = "Moovio",
            style = MaterialTheme.typography.h4,
            color = MoovioTextPrimary,
            modifier = Modifier.padding(start = AppSpacing.s),
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        TextField(
            value = fieldQuery,
            onValueChange = { fieldQuery = it },
            onValueCommit = {
                if (fieldQuery.isNotBlank()) onSearch(fieldQuery)
                fieldQuery = ""
            },
            modifier = Modifier.width(340.dp),
            colors = TextFieldDefaults.colors(
                backgroundColor = Color(0x1BFFFFFF),
                focusedBackgroundColor = Color(0x2BFFFFFF),
                cursorColor = AccentViolet,
            ),
            placeholder = {
                Text("Search movies, shows, anime…", color = MoovioTextSecondary)
            },
            leadingIcon = {
                Icon(Icons.Search, contentDescription = null, tint = MoovioTextSecondary)
            },
            singleLine = true,
        )
    }
}


