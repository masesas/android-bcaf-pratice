package com.masesas.exercise.bcaf_test_1.data.loan.mapper

import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductEntity
import com.masesas.exercise.bcaf_test_1.data.loan.remote.LoanProductDto
import com.masesas.exercise.bcaf_test_1.domain.loan.model.LoanProduct
import java.time.Instant

fun LoanProductDto.toEntity(position: Int): LoanProductEntity = LoanProductEntity(
    id = id,
    position = position,
    code = kode,
    name = nama,
    interestPercent = bungaPersen,
    tenorMinMonths = tenorMin,
    tenorMaxMonths = tenorMax,
    plafondMin = plafondMin,
    plafondMax = plafondMax,
    isActive = aktif,
    createdDate = createdDate,
    updatedDate = updatedDate,
)

fun LoanProductEntity.toDomain(): LoanProduct = LoanProduct(
    id = id,
    code = code,
    name = name,
    interestPercent = interestPercent,
    tenorMinMonths = tenorMinMonths,
    tenorMaxMonths = tenorMaxMonths,
    plafondMin = plafondMin,
    plafondMax = plafondMax,
    isActive = isActive,
    createdAt = createdDate.toInstantOrNull(),
    updatedAt = updatedDate.toInstantOrNull(),
)

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }
