package com.moovio.app.ui.player

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.moovio.app.MoovioApp
import com.moovio.app.data.integrations.OAuthProvider
import com.moovio.app.data.integrations.constantTimeEquals
import com.moovio.app.data.integrations.parseAccessToken
import com.moovio.app.data.integrations.parseOAuthState
import com.moovio.app.data.integrations.tokenExchange
import com.moovio.app.data.remote.httpPostForm
import com.moovio.app.data.remote.httpPostJson
import kotlinx.coroutines.launch

/**
 * Tracker OAuth redirect target (Phase 30): handles `moovio://oauth?code=…&state=…`.
 *
 * The provider rides in `state` as `"trakt:<nonce>"` (see the Settings connect buttons) and the
 * nonce is compared against the one this app stashed before the browser hop — a redirect without a
 * matching nonce is rejected, so another app on the device cannot inject a foreign authorization
 * code and have its token written into the user's tracker list. The authorization code is exchanged
 * for a token with the user's own stored app credentials and the token is saved exactly like a
 * pasted one. Every failure degrades to a toast — the Activity is transparent and always finishes.
 */
class OAuthCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        val code = uri?.getQueryParameter("code")
        val state = parseOAuthState(uri?.getQueryParameter("state"))
        val error = uri?.getQueryParameter("error")
        if (code.isNullOrBlank() || state == null) {
            Toast.makeText(
                this,
                if (!error.isNullOrBlank()) "OAuth cancelled: $error" else "OAuth failed: bad redirect",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val settings = (application as MoovioApp).container.settings
            // Consume the nonce exactly once, whether or not it matches: a second callback (or a
            // replayed one) must never be able to reuse it.
            val expected = settings.takeOAuthNonce()
            if (expected == null || !constantTimeEquals(expected, state.nonce)) {
                Toast.makeText(
                    this@OAuthCallbackActivity,
                    "OAuth failed: this redirect did not come from Moovio",
                    Toast.LENGTH_LONG,
                ).show()
                finish()
                return@launch
            }
            val message = runCatching { exchange(state.provider, code) }.fold(
                onSuccess = { "Connected to ${state.provider.key}" },
                onFailure = { "OAuth failed: ${it.message}" },
            )
            Toast.makeText(this@OAuthCallbackActivity, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private suspend fun exchange(provider: OAuthProvider, code: String) {
        val container = (application as MoovioApp).container
        val settings = container.settings
        val req = tokenExchange(
            provider = provider,
            code = code,
            clientId = settings.oauthClientIdOnce(),
            clientSecret = settings.oauthClientSecretOnce().ifBlank { null },
            codeVerifier = if (provider == OAuthProvider.MAL) settings.takeOAuthVerifier() else null,
        )
        val response = if (req.formFields != null) {
            httpPostForm(req.url, req.formFields)
        } else {
            httpPostJson(req.url, req.jsonBody.orEmpty())
        }
        val token = parseAccessToken(response) ?: throw IllegalStateException("no access token")
        settings.setToken(provider.key, token)
    }
}
