package com.masesas.exercise.bcaf_test_1.core.sync

sealed interface NetworkQuality {

    data class Good(val downstreamKbps: Int) : NetworkQuality

    data class Degraded(val reason: Reason, val downstreamKbps: Int) : NetworkQuality

    enum class Reason { OFFLINE, NO_INTERNET_CAPABILITY, NOT_VALIDATED, SUSPENDED, TOO_SLOW }
}
