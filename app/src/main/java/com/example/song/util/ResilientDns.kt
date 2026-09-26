package com.example.song.util

import android.util.Log
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Resilient DNS lookup implementation with fallback mechanisms for devices
 * with aggressive background network throttling (e.g., Nothing Phone, Pixel).
 */
object ResilientDns : Dns {

    private const val TAG = "ResilientDns"

    override fun lookup(hostname: String): List<InetAddress> {
        // 1. Primary: System DNS lookup
        try {
            val systemAddresses = Dns.SYSTEM.lookup(hostname)
            if (systemAddresses.isNotEmpty()) {
                return systemAddresses
            }
        } catch (e: UnknownHostException) {
            Log.w(TAG, "System DNS lookup failed for $hostname, attempting InetAddress fallback: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "System DNS error for $hostname: ${e.message}")
        }

        // 2. Secondary: Fallback to direct InetAddress Resolution
        try {
            val addresses = InetAddress.getAllByName(hostname).toList()
            if (addresses.isNotEmpty()) {
                return addresses
            }
        } catch (e: Exception) {
            Log.w(TAG, "Secondary InetAddress lookup failed for $hostname", e)
        }

        throw UnknownHostException("Unable to resolve host '$hostname' via system or fallback DNS")
    }
}
