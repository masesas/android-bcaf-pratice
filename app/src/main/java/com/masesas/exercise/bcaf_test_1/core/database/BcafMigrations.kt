package com.masesas.exercise.bcaf_test_1.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** submittedAmount pindah dari INTEGER ke TEXT; SQLite tak bisa ubah tipe kolom, jadi tabel direkonstruksi. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loan_application_new` (" +
                "`id` INTEGER NOT NULL, `customerId` INTEGER NOT NULL, `customerName` TEXT NOT NULL, " +
                "`loanProductId` INTEGER NOT NULL, `loanProductCode` TEXT NOT NULL, " +
                "`branchId` INTEGER NOT NULL, `branchCode` TEXT NOT NULL, " +
                "`submittedAmount` TEXT NOT NULL, `tenorMonths` INTEGER NOT NULL, " +
                "`status` TEXT, `note` TEXT, `version` INTEGER NOT NULL, " +
                "`createdDate` TEXT, `updatedDate` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `loan_application_new` (" +
                "`id`, `customerId`, `customerName`, `loanProductId`, `loanProductCode`, " +
                "`branchId`, `branchCode`, `submittedAmount`, `tenorMonths`, `status`, `note`, " +
                "`version`, `createdDate`, `updatedDate`" +
                ") SELECT `id`, `customerId`, `customerName`, `loanProductId`, `loanProductCode`, " +
                "`branchId`, `branchCode`, CAST(`submittedAmount` AS TEXT), `tenorMonths`, " +
                "`status`, `note`, `version`, `createdDate`, `updatedDate` FROM `loan_application`"
        )
        db.execSQL("DROP TABLE `loan_application`")
        db.execSQL("ALTER TABLE `loan_application_new` RENAME TO `loan_application`")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loan_product` (" +
                "`id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `code` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `interestPercent` REAL NOT NULL, " +
                "`tenorMinMonths` INTEGER NOT NULL, `tenorMaxMonths` INTEGER NOT NULL, " +
                "`plafondMin` TEXT NOT NULL, `plafondMax` TEXT NOT NULL, " +
                "`isActive` INTEGER NOT NULL, `createdDate` TEXT, `updatedDate` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

val BCAF_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
