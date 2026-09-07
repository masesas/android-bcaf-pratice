package com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationPage
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationQuery
import com.masesas.exercise.bcaf_test_1.domain.loan.repository.LoanApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel daftar pengajuan pinjaman (transaksi).
 *
 * Bukan state bersama: di-scope ke NavBackStackEntry milik TransactionScreen lewat
 * `hiltViewModel()`, jadi hidup dan matinya mengikuti layar itu saja. Berbeda dari
 * [LoanProductViewModel] yang sengaja di-scope ke Activity agar dipakai bersama stack legacy.
 *
 * `observeLoanApplications()` tidak menerima limit, jadi cache Room langsung memancarkan
 * seluruh baris dan tidak perlu jendela paging.
 */
@HiltViewModel
class LoanApplicationViewModel @Inject constructor(
    private val repository: LoanApplicationRepository,
) : ViewModel() {

    private val loadedPage = MutableStateFlow(FIRST_PAGE)

    private val _uiState = MutableStateFlow(LoanApplicationUiState())
    val uiState: StateFlow<LoanApplicationUiState> = _uiState.asStateFlow()

    init {
        observeCache()
        refresh()
    }

    /** Memuat ulang dari halaman pertama; repository mengganti seluruh cache dengan hasil server. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, failure = null) }

        viewModelScope.launch {
            when (val result = repository.refresh(queryOf(FIRST_PAGE))) {
                is AppResult.Success -> {
                    loadedPage.value = FIRST_PAGE
                    onPageLoaded(result.data) { it.copy(isRefreshing = false) }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isRefreshing = false, failure = result.failure)
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isRefreshing || state.isLoadingMore || !state.hasNextPage) return

        val nextPage = loadedPage.value + 1
        _uiState.update { it.copy(isLoadingMore = true, failure = null) }

        viewModelScope.launch {
            when (val result = repository.refresh(queryOf(nextPage))) {
                is AppResult.Success -> {
                    loadedPage.value = nextPage
                    onPageLoaded(result.data) { it.copy(isLoadingMore = false) }
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

    private fun onPageLoaded(
        page: LoanApplicationPage,
        clearIndicator: (LoanApplicationUiState) -> LoanApplicationUiState,
    ) {
        _uiState.update { clearIndicator(it).copy(hasNextPage = page.hasNextPage) }
    }

    /** Room adalah satu-satunya sumber baca; hasil refresh sampai ke UI lewat flow ini. */
    private fun observeCache() {
        viewModelScope.launch {
            repository.observeLoanApplications()
                .collect { applications -> _uiState.update { it.copy(items = applications) } }
        }
    }

    private fun queryOf(page: Int) = LoanApplicationQuery(page = page, size = PAGE_SIZE)

    private companion object {
        const val FIRST_PAGE = LoanApplicationQuery.DEFAULT_PAGE
        const val PAGE_SIZE = LoanApplicationQuery.DEFAULT_SIZE
    }
}
