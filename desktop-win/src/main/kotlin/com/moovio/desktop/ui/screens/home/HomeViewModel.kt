package com.moovio.desktop.ui.screens.home

import com.moovio.desktop.data.repo.CatalogRow
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class HomeUiState(
    val hero: List<MediaItem> = emptyList(),
    val rows: List<CatalogRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !loading && hero.isEmpty() && rows.isEmpty()
}

class HomeViewModel(private val container: AppContainer) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load(force: Boolean = false) {
        if (!force && loadJob?.isActive == true) return
        if (!force && _state.value.rows.isNotEmpty()) return
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.value = HomeUiState(loading = true)
            launch {
                val hero = runCatching {
                    withTimeoutOrNull(15_000) { container.catalog.hero() } ?: emptyList()
                }.getOrDefault(emptyList())
                _state.update { it.copy(hero = hero) }
            }

            val specs = container.catalog.curatedRows()
            val jobs = specs.map { spec ->
                launch {
                    val items = runCatching {
                        withTimeoutOrNull(20_000) { spec.loader() } ?: emptyList()
                    }.getOrDefault(emptyList())
                    if (items.isNotEmpty()) {
                        _state.update { current ->
                            if (current.rows.any { it.id == spec.id }) current
                            else current.copy(rows = current.rows + CatalogRow(spec.id, spec.title, items))
                        }
                    }
                }
            }
            jobs.joinAll()

            launch {
                val addonRows = runCatching {
                    withTimeoutOrNull(18_000) { container.catalog.addonRows() } ?: emptyList()
                }.getOrDefault(emptyList())
                _state.update { current ->
                    val known = current.rows.map { it.id }.toSet()
                    current.copy(rows = current.rows + addonRows.filterNot { it.id in known })
                }
            }

            val failed = _state.value.hero.isEmpty()
            _state.update { it.copy(loading = false, error = if (failed && it.rows.isEmpty()) "Could not reach metadata services" else null) }
        }
    }

    fun refresh() = load(force = true)

    fun dispose() {
        scope.cancel()
    }
}
