package com.masesas.exercise.bcaf_test_1.di

import android.content.Context
import androidx.room.Room
import com.masesas.exercise.bcaf_test_1.core.database.BCAF_MIGRATIONS
import com.masesas.exercise.bcaf_test_1.core.database.BcafDatabase
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanApplicationDao
import com.masesas.exercise.bcaf_test_1.data.loan.local.LoanProductDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BcafDatabase =
        Room.databaseBuilder(context, BcafDatabase::class.java, BcafDatabase.NAME)
            .addMigrations(*BCAF_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideLoanApplicationDao(database: BcafDatabase): LoanApplicationDao =
        database.loanApplicationDao()

    @Provides
    @Singleton
    fun provideLoanProductDao(database: BcafDatabase): LoanProductDao =
        database.loanProductDao()
}
