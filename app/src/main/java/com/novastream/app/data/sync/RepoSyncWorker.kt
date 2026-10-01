package com.novastream.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novastream.app.NovaApp

/**
 * Periodically refreshes the extension repository indexes (CloudStream / Aniyomi / Keiyoushi).
 *
 * The remote clients cache their responses through [com.novastream.app.data.remote.httpGetCached],
 * so warming them here means the Add-on Manager opens with fresh data and no network wait.
 */
class RepoSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NovaApp ?: return Result.success()
        return runCatching { app.container.addonRepository.syncAllRepos() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }

    companion object {
        const val UNIQUE_NAME = "nexus_repo_sync"
    }
}
