package com.masesas.exercise.bcaf_test_1.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.masesas.exercise.bcaf_test_1.core.sync.SyncDefaults.MIN_DOWNSTREAM_KBPS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Constraint WorkManager hanya menjamin "ada jaringan", bukan "internet tembus dan layak pakai".
 * Pemeriksaan ini menutup celah captive portal, jaringan tersuspensi, dan bandwidth terlalu rendah.
 */
@Singleton
class NetworkQualityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val connectivityManager: ConnectivityManager?
        get() = context.getSystemService(ConnectivityManager::class.java)

    fun current(): NetworkQuality {
        val manager = connectivityManager
            ?: return NetworkQuality.Degraded(NetworkQuality.Reason.OFFLINE, downstreamKbps = 0)

        val network = manager.activeNetwork
            ?: return NetworkQuality.Degraded(NetworkQuality.Reason.OFFLINE, downstreamKbps = 0)

        val capabilities = manager.getNetworkCapabilities(network)
            ?: return NetworkQuality.Degraded(NetworkQuality.Reason.OFFLINE, downstreamKbps = 0)

        return capabilities.evaluate()
    }

    private fun NetworkCapabilities.evaluate(): NetworkQuality {
        val downstreamKbps = linkDownstreamBandwidthKbps

        val reason = when {
            !hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ->
                NetworkQuality.Reason.NO_INTERNET_CAPABILITY

            !hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ->
                NetworkQuality.Reason.NOT_VALIDATED

            !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED) ->
                NetworkQuality.Reason.SUSPENDED

            // 0 = bandwidth tidak diketahui, bukan berarti lambat.
            downstreamKbps in 1 until MIN_DOWNSTREAM_KBPS ->
                NetworkQuality.Reason.TOO_SLOW

            else -> null
        }

        return reason
            ?.let { NetworkQuality.Degraded(it, downstreamKbps) }
            ?: NetworkQuality.Good(downstreamKbps)
    }
}
