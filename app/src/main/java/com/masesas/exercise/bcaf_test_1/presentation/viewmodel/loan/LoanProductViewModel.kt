package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProductQuery
import com.masesas.exercise.bcaf_test_1.domain.loan.repository.LoanProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel bersama daftar produk pinjaman.
 *
 * Dipakai lintas UI: fragment legacy lewat `activityViewModels()`, Compose lewat
 * `hiltViewModel(activity)`. Keduanya menunjuk ViewModelStore milik Activity sehingga
 * state paging dan cache-nya sama.
 */
@HiltViewModel
class LoanProductViewModel @Inject constructor(
    private val repository: LoanProductRepository,
) : ViewModel() {

    private val loadedPages = MutableStateFlow(1)

    private val _uiState = MutableStateFlow(LoanProductUiState())
    val uiState: StateFlow<LoanProductUiState> = _uiState.asStateFlow()

    init {
        observeCache()
        restoreCachedPages()
    }

    /** Memuat ulang dari halaman pertama; cache lama digantikan hasil server. */
    fun refresh(initialLoad: Boolean = false) {
        val state = _uiState.value
        if (state.isRefreshing || state.InitialLoading) return

        // Penanda dipasang sinkron sebelum launch; kalau di dalam launch, dua panggilan
        // beruntun sama-sama lolos guard dan halaman yang sama diminta dua kali.
        _uiState.update {
            it.copy(isRefreshing = !initialLoad, InitialLoading = initialLoad, failure = null)
        }

        viewModelScope.launch {
            when (val result = repository.refresh(queryOf(FIRST_PAGE))) {
                is AppResult.Success -> {
                    loadedPages.value = 1
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            InitialLoading = false,
                            hasNextPage = result.data.hasNextPage,
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isRefreshing = false, InitialLoading = false, failure = result.failure)
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isRefreshing || state.InitialLoading || state.isLoadingMore) return
        if (!state.hasNextPage) return

        val nextPage = loadedPages.value
        _uiState.update { it.copy(isLoadingMore = true, failure = null) }

        viewModelScope.launch {
            when (val result = repository.refresh(queryOf(nextPage))) {
                is AppResult.Success -> {
                    loadedPages.value = nextPage + 1
                    _uiState.update {
                        it.copy(isLoadingMore = false, hasNextPage = result.data.hasNextPage)
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoadingMore = false, failure = result.failure)
                }
            }
        }
    }

    fun clearFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCache() {
        viewModelScope.launch {
            loadedPages
                .flatMapLatest { pages -> repository.observeLoanProducts(pages * PAGE_SIZE) }
                .collect { products -> _uiState.update { it.copy(items = products) } }
        }
    }

    /** Cold start offline: tampilkan semua halaman yang sudah tersimpan sebelum refresh. */
    private fun restoreCachedPages() {
        viewModelScope.launch {
            val cached = repository.cachedCount()
            if (cached > 0) loadedPages.value = (cached + PAGE_SIZE - 1) / PAGE_SIZE
            refresh(initialLoad = true)
        }
    }

    private fun queryOf(page: Int) = LoanProductQuery(page = page, size = PAGE_SIZE)

    private companion object {
        const val FIRST_PAGE = LoanProductQuery.DEFAULT_PAGE
        const val PAGE_SIZE = LoanProductQuery.DEFAULT_SIZE
    }
}
