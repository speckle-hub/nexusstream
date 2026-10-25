package com.moovio.desktop.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.ui.theme.AppSpacing
import com.moovio.desktop.ui.theme.AccentViolet
import com.moovio.desktop.ui.theme.MoovioTextPrimary
import com.moovio.desktop.ui.theme.MoovioTextSecondary
import com.moovio.desktop.ui.theme.moovioColors

/**
 * PIN gate for the NSFW section. On first use there is no PIN yet, so the user creates one;
 * afterwards the stored PIN is verified before unlocking.
 */
@Composable
fun NsfwLockScreen(container: AppContainer, onUnlock: () -> Unit) {
    val settings = container.settings
    var pin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(AppSpacing.xxl),
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.Start,
    ) {
        Text(
            text = if (settings.nsfwPin == null) "Set NSFW PIN" else "NSFW — PIN required",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppSpacing.m),
        )
        Text(
            text = if (settings.nsfwPin == null) "Create a PIN to lock the NSFW section."
            else "Enter your PIN to unlock NSFW content.",
            style = MaterialTheme.typography.body1,
            color = MoovioTextSecondary,
            modifier = Modifier.padding(bottom = AppSpacing.xl),
        )

        TextField(
            value = pin,
            onValueChange = { pin = it },
            placeholder = { Text("PIN (4-6 digits)") },
            modifier = Modifier.width(240.dp),
            visualTransformation = androidx.compose.ui.text.input.VisualTransformation.Password,
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        )

        if (settings.nsfwPin != null) {
            Button(
                onClick = {
                    if (pin == settings.nsfwPin) {
                        settings.nsfwUnlockedAt = System.currentTimeMillis()
                        onUnlock()
                    } else {
                        status = "Incorrect PIN"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                modifier = Modifier.padding(top = AppSpacing.m),
            ) {
                Text(text = "Unlock", color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Button(
                onClick = {
                    if (pin.length in 4..6) {
                        settings.nsfwPin = pin
                        settings.nsfwUnlockedAt = System.currentTimeMillis()
                        onUnlock()
                    } else {
                        status = "Choose a 4-6 digit PIN"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                modifier = Modifier.padding(top = AppSpacing.m),
            ) {
                Text(text = "Create PIN & Continue", color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        status?.let {
            Text(text = it, style = MaterialTheme.typography.body2, color = moovioColors.accent,
                modifier = Modifier.padding(top = AppSpacing.s))
        }
    }
}


