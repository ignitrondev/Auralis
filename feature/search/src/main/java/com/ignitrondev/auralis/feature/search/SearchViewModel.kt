/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.auralis.core.ui.CatalogUiState
import com.ignitrondev.auralis.core.ui.catalogState
import com.ignitrondev.auralis.domain.model.SearchResults
import com.ignitrondev.auralis.domain.repository.CatalogRepository
import com.ignitrondev.auralis.domain.repository.SearchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Results for the current query. [Idle] means the query is blank: show recent searches instead. */
sealed interface SearchResultsState {
    data object Idle : SearchResultsState

    data class Query(val query: String, val state: CatalogUiState<SearchResults>) : SearchResultsState
}

data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val results: SearchResultsState = SearchResultsState.Idle,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val history: SearchHistoryRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val retries = MutableStateFlow(0)

    /** Debounced so typing does not fire a request per key; only the latest query runs. */
    private val results = combine(query.debounce(DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(), retries) { q, _ -> q }
        .transformLatest { q ->
            if (q.isEmpty()) {
                emit(SearchResultsState.Idle)
            } else {
                emit(SearchResultsState.Query(q, CatalogUiState.Loading))
                emit(SearchResultsState.Query(q, catalogState { catalog.search(q) }))
            }
        }

    val state: StateFlow<SearchUiState> = combine(query, history.recentSearches(), results) { q, recent, res ->
        SearchUiState(query = q, recentSearches = recent, results = if (q.isBlank()) SearchResultsState.Idle else res)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Keyboard search action or a tapped recent search: remember it and show its results. */
    fun onSearch(value: String) {
        query.value = value
        viewModelScope.launch { history.add(value) }
    }

    /** Remembers the query when the user opens one of its results. */
    fun onResultOpened() {
        val current = query.value
        viewModelScope.launch { history.add(current) }
    }

    fun onRemoveRecent(value: String) {
        viewModelScope.launch { history.remove(value) }
    }

    fun retry() {
        retries.value++
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
    }
}
