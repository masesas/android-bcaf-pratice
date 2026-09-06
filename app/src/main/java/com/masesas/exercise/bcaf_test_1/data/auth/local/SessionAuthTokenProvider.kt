package com.masesas.exercise.bcaf_test_1.data.auth.local

import com.masesas.exercise.bcaf_test_1.core.network.AuthTokenProvider
import kotlinx.coroutines.flow.first

class SessionAuthTokenProvider(
    private val localDataSource: AuthSessionLocalDataSource,
) : AuthTokenProvider {

    override suspend fun currentToken(): String? =
        localDataSource.observe().first()?.accessToken
}
