package com.novastream.app.data.integrations

import com.novastream.app.data.ext.NexusExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of probing one extension source: reachability, round-trip latency and item count. */
data class SourceProbe(
    val id: String,
    val ok: Boolean,
    val latencyMs: Long,
    val itemCount: Int,
    val error: String?,
)

/**
 * "Tests" a source extension by running its catalogue call and timing it. A successful probe with
 * a sensible item count proves the extension loads, its network path works and its parser returns.
 */
object SourceTester {
    suspend fun probe(ext: NexusExtension): SourceProbe = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        runCatching { ext.catalog(null) }.fold(
            onSuccess = { SourceProbe(ext.id, true, System.currentTimeMillis() - start, it.size, null) },
            onFailure = { SourceProbe(ext.id, false, System.currentTimeMillis() - start, 0, it.message ?: "Failed") },
        )
    }
}
