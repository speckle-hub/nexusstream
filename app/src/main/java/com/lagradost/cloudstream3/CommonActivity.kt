package com.lagradost.cloudstream3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

/**
 * Minimal re-implementation of CloudStream's app-module `CommonActivity`.
 *
 * Some providers call `CommonActivity.showToast(...)` to report progress or errors. That class
 * lives in the CloudStream app, not the library we depend on, so we provide a behaviour-compatible
 * holder here (real Toasts when an Activity is attached, a silent no-op otherwise).
 */
object CommonActivity {

    private var _activity: WeakReference<Activity>? = null

    var activity: Activity?
        get() = _activity?.get()
        private set(value) {
            _activity = value?.let { WeakReference(it) }
        }

    val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics

    @MainThread
    fun setActivityInstance(newActivity: Activity?) {
        activity = newActivity
    }

    private var currentToast: Toast? = null

    @SuppressLint("ShowToast")
    fun showToast(act: Activity?, message: String?, duration: Int? = null) {
        if (message.isNullOrBlank()) return
        val target = act ?: activity ?: return
        target.runOnUiThread {
            runCatching {
                currentToast?.cancel()
                currentToast = Toast.makeText(
                    target,
                    message,
                    duration ?: Toast.LENGTH_SHORT,
                ).also { it.show() }
            }
        }
    }

    fun showToast(@StringRes message: Int, duration: Int? = null) {
        val act = activity ?: return
        showToast(act, act.getString(message), duration)
    }

    fun showToast(act: Activity?, @StringRes message: Int, duration: Int? = null) {
        val target = act ?: activity ?: return
        showToast(target, target.getString(message), duration)
    }

    fun showToast(message: String?, duration: Int? = null) {
        showToast(activity, message, duration)
    }
}
