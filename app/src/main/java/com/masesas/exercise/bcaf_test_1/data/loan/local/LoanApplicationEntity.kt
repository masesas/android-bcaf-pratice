package com.masesas.exercise.bcaf_test_1.data.loan.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "loan_application")
data class LoanApplicationEntity(
    @PrimaryKey val id: Long,
    val customerId: Long,
    val customerName: String,
    val loanProductId: Long,
    val loanProductCode: String,
    val branchId: Long,
    val branchCode: String,
    val submittedAmount: BigDecimal,
    val tenorMonths: Int,
    val status: String?,
    val note: String?,
    val version: Long,
    val createdDate: String?,
    val updatedDate: String?,
)
