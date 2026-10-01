package com.novastream.app.ui.screens.nsfw

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.novastream.app.ui.theme.LocalNovaColors
import java.security.MessageDigest

fun hashPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
}

@Composable
fun NsfwLockScreen(
    storedPinHash: String?,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    onSetPin: (String) -> Unit,
) {
    val nova = LocalNovaColors.current
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSetPin by remember { mutableStateOf(false) }

    fun tryBiometric() {
        val activity = context as? FragmentActivity ?: return
        val manager = BiometricManager.from(context)
        val can = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            error = "Biometric not available on this device"
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                error = errString.toString()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock NSFW")
            .setSubtitle("Confirm your identity")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Lock, null, tint = nova.accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("NSFW is locked", style = MaterialTheme.typography.headlineSmall, color = nova.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Enter your PIN to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = nova.textSecondary,
            )
            Spacer(Modifier.height(20.dp))

            if (storedPinHash != null) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8); error = null },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (hashPin(pin) == storedPinHash) onUnlocked() else error = "Incorrect PIN"
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Unlock") }
            } else {
                // Lock is on but no PIN exists yet — never strand the user. Let them set one here.
                Text(
                    "No PIN is set yet. Set one to unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = nova.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showSetPin = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Set PIN & unlock") }
            }

            if (biometricEnabled) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { tryBiometric() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use biometrics")
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onSave = { newPin -> showSetPin = false; onSetPin(newPin) },
        )
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set NSFW PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
                label = { Text("PIN (4\u20138 digits)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = { TextButton(onClick = { if (pin.length >= 4) onSave(pin) }) { Text("Save & unlock") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
