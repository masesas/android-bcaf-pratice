package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.data.loan.repository.LoanApplicationRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanApplicationViewModel
import com.masesas.exercise.bcaf_test_1.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val PAGE_SIZE = 20

@OptIn(ExperimentalCoroutinesApi::class)
class LoanApplicationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeLoanApplicationDao()
    private val api = FakeLoanApplicationApi(totalElements = 55)

    private fun viewModel() = LoanApplicationViewModel(
        LoanApplicationRepositoryImpl(
            dao = dao,
            api = api,
            json = Json,
            ioDispatcher = UnconfinedTestDispatcher(mainDispatcherRule.dispatcher.scheduler),
        )
    )

    @Test
    fun `init memuat halaman pertama`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PAGE_SIZE, state.items.size)
        assertTrue(state.hasNextPage)
        assertFalse(state.isRefreshing)
        assertNull(state.failure)
    }

    @Test
    fun `loadMore menambah satu halaman`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(PAGE_SIZE * 2, viewModel.uiState.value.items.size)
        assertEquals(1, api.lastPage)
    }

    @Test
    fun `loadMore berhenti setelah halaman terakhir`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        repeat(5) {
            viewModel.loadMore()
            advanceUntilIdle()
        }

        val state = viewModel.uiState.value
        assertEquals(55, state.items.size)
        assertFalse(state.hasNextPage)
        assertEquals(2, api.lastPage)
    }

    /** Scroll cepat memanggil loadMore beruntun; halaman yang sama tidak boleh diminta dua kali. */
    @Test
    fun `loadMore beruntun hanya meminta satu halaman`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        api.requestedPages.clear()

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1), api.requestedPages)
        assertEquals(PAGE_SIZE * 2, viewModel.uiState.value.items.size)
    }

    @Test
    fun `refresh kembali ke halaman pertama`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(PAGE_SIZE, viewModel.uiState.value.items.size)
    }

    /** Setelah refresh, cursor paging harus balik ke 0 agar loadMore tidak melompati halaman 1. */
    @Test
    fun `loadMore setelah refresh melanjutkan dari halaman satu`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        api.requestedPages.clear()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1), api.requestedPages)
    }

    @Test
    fun `offline saat cold start tetap menampilkan cache`() = runTest {
        viewModel().also { advanceUntilIdle() }
        api.failWith = FakeLoanApplicationApi.offline()

        val state = viewModel().let { advanceUntilIdle(); it.uiState.value }

        assertEquals(PAGE_SIZE, state.items.size)
        assertTrue(state.failure is CommonFailure.Network)
        assertNull(state.blockingFailure)
    }

    @Test
    fun `clearFailure menghapus kegagalan tanpa menyentuh data`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        api.failWith = FakeLoanApplicationApi.offline()
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.clearFailure()

        val state = viewModel.uiState.value
        assertNull(state.failure)
        assertEquals(PAGE_SIZE, state.items.size)
    }
}
