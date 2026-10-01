package com.novastream.app

import com.novastream.app.ui.vm.runCatchingCancellable
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The whole-app search bug where results \"glitched\" and showed a different query's results came
 * from `runCatching` swallowing [CancellationException]: a cancelled search kept running and could
 * overwrite a newer query. [runCatchingCancellable] must never swallow cancellation.
 */
class SearchCancellationTest {

    @Test
    fun `rethrows cancellation so a superseded search actually stops`() {
        var cancelled = false
        try {
            runCatchingCancellable<Unit> { throw CancellationException("stop") }
        } catch (e: CancellationException) {
            cancelled = true
        }
        assertTrue("CancellationException must propagate", cancelled)
    }

    @Test
    fun `wraps ordinary failures and passes successes through`() {
        val failure = runCatchingCancellable<Int> { throw IllegalStateException("boom") }
        assertTrue(failure.isFailure)
        assertEquals("boom", failure.exceptionOrNull()?.message)
        assertEquals("ok", runCatchingCancellable { "ok" }.getOrNull())
    }
}
