package com.moovio.desktop.ui.screens.detail

import com.moovio.desktop.data.repo.AddonFailure
import com.moovio.desktop.data.repo.StreamResult
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.MetaDetail
import com.moovio.desktop.domain.model.StreamSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val detail: MetaDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

data class StreamsUiState(
    val loading: Boolean = false,
    val sources: List<StreamSource> = emptyList(),
    val failures: List<AddonFailure> = emptyList(),
    val includeAdult: Boolean = false,
)

class DetailViewModel(
    private val container: AppContainer,
    val item: MediaItem,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private val _streams = MutableStateFlow(StreamsUiState())
    val streams: StateFlow<StreamsUiState> = _streams.asStateFlow()

    private var streamsJob: Job? = null

    val includeAdult: Boolean get() = item.adult && container.isNsfwUnlocked

    fun load() {
        scope.launch {
            _state.value = DetailUiState(loading = true)
            val detail = runCatching { container.metadata.detail(item, includeAdult) }.getOrNull()
            _state.value = DetailUiState(
                detail = detail,
                loading = false,
                error = if (detail == null) "Couldn't load metadata for “${item.name}”." else null,
            )
        }
    }

    fun loadStreams(season: Int?, episode: Int?) {
        streamsJob?.cancel()
        streamsJob = scope.launch {
            _streams.value = StreamsUiState(loading = true, includeAdult = includeAdult)
            val detail = _state.value.detail
            val result: StreamResult = runCatching {
                container.streams.resolve(
                    media = detail?.toMediaItem() ?: item,
                    detail = detail,
                    season = season,
                    episode = episode,
                    includeAdult = includeAdult,
                )
            }.getOrElse { StreamResult(emptyList(), listOf(AddonFailure("Add-ons", it.message ?: "failed"))) }
            _streams.value = StreamsUiState(
                loading = false,
                sources = result.sources,
                failures = result.failures,
                includeAdult = includeAdult,
            )
        }
    }

    fun dispose() {
        streamsJob?.cancel()
        scope.cancel()
    }
}
