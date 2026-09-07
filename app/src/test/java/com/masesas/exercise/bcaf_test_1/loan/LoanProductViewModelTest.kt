package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.data.loan.repository.LoanProductRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.presentation.viewmodel.loan.LoanProductViewModel
import com.masesas.exercise.bcaf_test_1.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val PAGE_SIZE = 20

@OptIn(ExperimentalCoroutinesApi::class)
class LoanProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeLoanProductDao()
    private val api = FakeLoanProductApi(totalElements = 55)

    private fun viewModel() = LoanProductViewModel(
        LoanProductRepositoryImpl(
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
        assertEquals(false, state.hasNextPage)
        assertEquals(2, api.lastPage)
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

    /** Dua tarikan pull-to-refresh beruntun hanya boleh menghasilkan satu panggilan server. */
    @Test
    fun `refresh beruntun hanya sekali memanggil server`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        api.requestedPages.clear()

        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(0), api.requestedPages)
    }

    /** Guard re-entrancy tidak boleh mengunci refresh selamanya setelah initial load selesai. */
    @Test
    fun `refresh tetap jalan setelah initial load selesai`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        api.requestedPages.clear()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(0), api.requestedPages)
    }

    @Test
    fun `offline saat cold start tetap menampilkan cache`() = runTest {
        viewModel().also { advanceUntilIdle() }
        api.failWith = FakeLoanProductApi.offline()

        val state = viewModel().let { advanceUntilIdle(); it.uiState.value }

        assertEquals(PAGE_SIZE, state.items.size)
        assertTrue(state.failure is CommonFailure.Network)
        assertNull(state.blockingFailure)
    }
}
