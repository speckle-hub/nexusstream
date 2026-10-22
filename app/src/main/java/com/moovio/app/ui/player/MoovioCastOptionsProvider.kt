package com.moovio.app.ui.player

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Cast sender options (Phase 30).
 *
 * Uses the Default Media Receiver ([CAST_RECEIVER_APP_ID]) so no custom receiver registration is
 * needed — any Chromecast / Cast-enabled TV can play the handed-over stream URL. Declared via the
 * `OPTIONS_PROVIDER_CLASS_NAME` manifest metadata the Cast framework discovers at startup.
 */
class MoovioCastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(CAST_RECEIVER_APP_ID)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}

/** Default Media Receiver app id — plays plain video URLs with no custom receiver. */
const val CAST_RECEIVER_APP_ID = "CC1AD845"
