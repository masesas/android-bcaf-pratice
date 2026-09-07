package com.masesas.exercise.bcaf_test_1.loan

import com.masesas.exercise.bcaf_test_1.data.loan.repository.LoanProductRepositoryImpl
import com.masesas.exercise.bcaf_test_1.domain.common.AppResult
import com.masesas.exercise.bcaf_test_1.domain.common.CommonFailure
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProductQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoanProductRepositoryImplTest {

    private val dao = FakeLoanProductDao()
    private val api = FakeLoanProductApi(totalElements = 25)

    private val repository = LoanProductRepositoryImpl(
        dao = dao,
        api = api,
        json = Json,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `refresh halaman pertama menulis cache dengan posisi berurutan`() = runTest {
        val result = repository.refresh(LoanProductQuery(page = 0, size = 10))

        assertTrue(result is AppResult.Success)
        assertEquals(10, dao.current.size)
        assertEquals(List(10) { it }, dao.current.map { it.position })
    }

    @Test
    fun `refresh halaman kedua menambah cache tanpa menghapus halaman pertama`() = runTest {
        repository.refresh(LoanProductQuery(page = 0, size = 10))
        repository.refresh(LoanProductQuery(page = 1, size = 10))

        assertEquals(20, dao.current.size)
        assertEquals(List(20) { it }, dao.current.map { it.position })
    }

    @Test
    fun `refresh halaman pertama mengganti cache lama`() = runTest {
        repository.refresh(LoanProductQuery(page = 0, size = 10))
        repository.refresh(LoanProductQuery(page = 1, size = 10))
        repository.refresh(LoanProductQuery(page = 0, size = 10))

        assertEquals(10, dao.current.size)
    }

    @Test
    fun `observe membatasi hasil sesuai limit`() = runTest {
        repository.refresh(LoanProductQuery(page = 0, size = 10))
        repository.refresh(LoanProductQuery(page = 1, size = 10))

        assertEquals(10, repository.observeLoanProducts(limit = 10).first().size)
        assertEquals(20, repository.observeLoanProducts(limit = 20).first().size)
    }

    @Test
    fun `hasNextPage benar pada halaman terakhir`() = runTest {
        val last = repository.refresh(LoanProductQuery(page = 2, size = 10))

        assertTrue(last is AppResult.Success)
        assertEquals(false, (last as AppResult.Success).data.hasNextPage)
    }

    @Test
    fun `kegagalan jaringan tidak menghapus cache yang sudah ada`() = runTest {
        repository.refresh(LoanProductQuery(page = 0, size = 10))
        api.failWith = FakeLoanProductApi.offline()

        val result = repository.refresh(LoanProductQuery(page = 0, size = 10))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).failure is CommonFailure.Network)
        assertEquals(10, dao.current.size)
    }
}
