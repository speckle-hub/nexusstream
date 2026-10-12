package com.lagradost.cloudstream3

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.DataStore.removeKey
import com.lagradost.cloudstream3.utils.DataStore.removeKeys
import com.lagradost.cloudstream3.utils.DataStore.setKey
import java.lang.ref.WeakReference

/**
 * Minimal re-implementation of CloudStream's app-module `CloudStreamApp`.
 *
 * Providers are compiled against the CloudStream app, so `CloudStreamApp.getKey(...)` /
 * `setKey(...)` calls are inlined into their dex and resolve members of this class at runtime.
 * Returning a null-ish but *present* implementation means a provider that persists its own
 * settings keeps working instead of dying with `NoClassDefFoundError`.
 *
 * Call [initialize] once at app start (`NovaApp`) to give the inlined preference helpers a context.
 */
class CloudStreamApp {

    companion object {
        var exceptionHandler: Any? = null

        /** Walk up [ContextWrapper]s to find the hosting [Activity], if any. */
        tailrec fun Context.getActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.getActivity()
            else -> null
        }

        private var _context: WeakReference<Context>? = null

        var context: Context?
            get() = _context?.get()
            set(value) {
                _context = value?.let { WeakReference(it) }
            }

        /** Bind the process context so provider preference reads/writes have somewhere to go. */
        fun initialize(ctx: Context) {
            context = ctx.applicationContext
        }

        fun <T : Any> getKeyClass(path: String, valueType: Class<T>): T? =
            context?.getKey(path, valueType)

        fun <T : Any> setKeyClass(path: String, value: T) {
            context?.setKey(path, value)
        }

        fun <T> setKey(path: String, value: T) {
            context?.setKey(path, value)
        }

        fun <T> setKey(folder: String, path: String, value: T) {
            context?.setKey(folder, path, value)
        }

        inline fun <reified T : Any> getKey(path: String, defVal: T?): T? =
            context?.getKey(path, defVal)

        inline fun <reified T : Any> getKey(path: String): T? = context?.getKey(path)

        inline fun <reified T : Any> getKey(folder: String, path: String): T? =
            context?.getKey(folder, path)

        inline fun <reified T : Any> getKey(folder: String, path: String, defVal: T?): T? =
            context?.getKey(folder, path, defVal)

        fun getKeys(folder: String): List<String>? = context?.getKeys(folder)

        fun removeKey(folder: String, path: String) {
            context?.removeKey(folder, path)
        }

        fun removeKey(path: String) {
            context?.removeKey(path)
        }

        fun removeKeys(folder: String): Int? = context?.removeKeys(folder)
    }
}
