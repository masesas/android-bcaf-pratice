package com.masesas.exercise.bcaf_test_1.data.loan.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "loan_product")
data class LoanProductEntity(
    @PrimaryKey val id: Long,
    /** Urutan global dari server (page * size + index) agar urutan cache sama dengan API. */
    val position: Int,
    val code: String,
    val name: String,
    val interestPercent: Double,
    val tenorMinMonths: Int,
    val tenorMaxMonths: Int,
    val plafondMin: BigDecimal,
    val plafondMax: BigDecimal,
    val isActive: Boolean,
    val createdDate: String?,
    val updatedDate: String?,
)
