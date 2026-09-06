package com.masesas.exercise.bcaf_test_1.core

import com.masesas.exercise.bcaf_test_1.domain.auth.repository.AuthRepository

// Perakitan manual digantikan Hilt (di/AuthModule).
//import android.content.Context
//import com.masesas.exercise.bcaf_test_1.data.auth.local.AuthSessionLocalDataSource
//import com.masesas.exercise.bcaf_test_1.data.auth.mock.MockAuthApi
//import com.masesas.exercise.bcaf_test_1.data.auth.repository.AuthRepositoryImpl

interface AppContainer {
    val authRepository: AuthRepository
}

//class DefaultAppContainer(context: Context) : AppContainer {
//
//    private val appContext: Context = context.applicationContext
//
//    override val authRepository: AuthRepository by lazy {
//        AuthRepositoryImpl(
//            localDataSource = AuthSessionLocalDataSource(appContext),
//            remoteDataSource = MockAuthApi(),
//        )
//    }
//}
