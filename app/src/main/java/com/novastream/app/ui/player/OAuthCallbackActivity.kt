package com.novastream.app.ui.player

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.novastream.app.NovaApp
import com.novastream.app.data.integrations.OAuthProvider
import com.novastream.app.data.integrations.parseAccessToken
import com.novastream.app.data.integrations.tokenExchange
import com.novastream.app.data.remote.httpPostForm
import com.novastream.app.data.remote.httpPostJson
import kotlinx.coroutines.launch

/**
 * Tracker OAuth redirect target (Phase 30): handles `novastream://oauth?code=…&state=…`.
 *
 * The provider rides in `state` as `"trakt:<nonce>"` (see the Settings connect buttons); the
 * authorization code is exchanged for a token with the user's own stored app credentials and the
 * token is saved exactly like a pasted one. Every failure degrades to a toast — the Activity is
 * transparent and always finishes.
 */
class OAuthCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data
        val code = uri?.getQueryParameter("code")
        val provider = OAuthProvider.fromKey(uri?.getQueryParameter("state")?.substringBefore(':'))
        val error = uri?.getQueryParameter("error")
        if (code.isNullOrBlank() || provider == null) {
            Toast.makeText(
                this,
                if (!error.isNullOrBlank()) "OAuth cancelled: $error" else "OAuth failed: bad redirect",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val message = runCatching { exchange(provider, code) }.fold(
                onSuccess = { "Connected to ${provider.key}" },
                onFailure = { "OAuth failed: ${it.message}" },
            )
            Toast.makeText(this@OAuthCallbackActivity, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private suspend fun exchange(provider: OAuthProvider, code: String) {
        val container = (application as NovaApp).container
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
