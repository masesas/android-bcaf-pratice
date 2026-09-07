package com.masesas.exercise.bcaf_test_1.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationDao
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationEntity
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductDao
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductEntity

@Database(
    entities = [LoanApplicationEntity::class, LoanProductEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(BigDecimalConverter::class)
abstract class BcafDatabase : RoomDatabase() {

    abstract fun loanApplicationDao(): LoanApplicationDao

    abstract fun loanProductDao(): LoanProductDao

    companion object {
        const val NAME = "bcaf.db"
    }
}
