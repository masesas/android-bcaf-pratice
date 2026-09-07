package com.masesas.exercise.bcaf_test_1.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.masesas.exercise.bcaf_test_1.core.database.BCAF_MIGRATIONS
import com.masesas.exercise.bcaf_test_1.core.database.BcafDatabase
import com.masesas.exercise.bcaf_test_1.core.database.MIGRATION_1_2
import com.masesas.exercise.bcaf_test_1.core.database.MIGRATION_2_3
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcafDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BcafDatabase::class.java,
    )

    @Test
    fun migrate1To2_convertsSubmittedAmountToTextWithoutLosingValue() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO loan_application VALUES " +
                    "(1, 10, 'Budi', 5, 'KTA', 2, 'JKT', 50000000, 12, 'DRAFT', NULL, 1, NULL, NULL)"
            )
            db.execSQL(
                "INSERT INTO loan_application VALUES " +
                    "(2, 11, 'Siti', 6, 'KMG', 2, 'JKT', 1250000000, 36, 'SUBMITTED', 'catatan', 2, NULL, NULL)"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT id, submittedAmount, typeof(submittedAmount) FROM loan_application ORDER BY id").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("50000000", c.getString(1))
            assertEquals("text", c.getString(2))
            assertTrue(c.moveToNext())
            assertEquals("1250000000", c.getString(1))
            assertEquals("text", c.getString(2))
        }
    }

    @Test
    fun migrate2To3_createsLoanProductTable() {
        helper.createDatabase(TEST_DB, 2).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT COUNT(*) FROM loan_product").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
    }

    @Test
    fun migrate1To3_thenRoomReadsDataThroughDao() = runBlocking {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO loan_application VALUES " +
                    "(1, 10, 'Budi', 5, 'KTA', 2, 'JKT', 50000000, 12, 'DRAFT', NULL, 1, NULL, NULL)"
            )
        }
        helper.runMigrationsAndValidate(TEST_DB, 3, true, *BCAF_MIGRATIONS)

        val database = openMigratedDatabase()
        val applications = database.loanApplicationDao().observeAll().first()

        assertEquals(1, applications.size)
        assertEquals(BigDecimal("50000000"), applications.first().submittedAmount)
        assertEquals(0, database.loanProductDao().count())
    }

    private fun openMigratedDatabase(): BcafDatabase =
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BcafDatabase::class.java,
            TEST_DB,
        ).addMigrations(*BCAF_MIGRATIONS).build().also(helper::closeWhenFinished)

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
