package com.masesas.exercise.bcaf_test_1.data.loan.mapper

import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationEntity
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanApplicationDto
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplication
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanApplicationStatus
import java.time.Instant

fun LoanApplicationDto.toEntity(): LoanApplicationEntity = LoanApplicationEntity(
    id = id,
    customerId = idCustomer,
    customerName = namaCustomer,
    loanProductId = idLoanProduct,
    loanProductCode = kodeLoanProduct,
    branchId = idBranch,
    branchCode = kodeBranch,
    submittedAmount = jumlahPengajuan,
    tenorMonths = tenorBulan,
    status = status,
    note = catatan,
    version = version,
    createdDate = createdDate,
    updatedDate = updatedDate,
)

fun LoanApplicationEntity.toDomain(): LoanApplication = LoanApplication(
    id = id,
    customerId = customerId,
    customerName = customerName,
    loanProductId = loanProductId,
    loanProductCode = loanProductCode,
    branchId = branchId,
    branchCode = branchCode,
    submittedAmount = submittedAmount,
    tenorMonths = tenorMonths,
    status = LoanApplicationStatus.fromApi(status),
    note = note,
    version = version,
    createdAt = createdDate.toInstantOrNull(),
    updatedAt = updatedDate.toInstantOrNull(),
)

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }
