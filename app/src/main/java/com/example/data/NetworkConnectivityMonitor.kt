package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkConnectivityMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isCurrentlyOnline(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    val isOnline: Flow<Boolean> = callbackFlow {
        val sendCurrent = {
            trySend(isCurrentlyOnline())
        }

        sendCurrent()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                sendCurrent()
            }

            override fun onLost(network: Network) {
                sendCurrent()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                sendCurrent()
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(request, callback)
        }.onFailure {
            trySend(isCurrentlyOnline())
        }

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()
}
