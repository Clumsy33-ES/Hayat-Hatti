package com.example.myapplication.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class LocalRepository(
    private val context: Context,
    private val dbHelper: DatabaseHelper,
    private val remoteRepo: RemoteRepository
) {

    fun saveMessage(message: String) {
        if (isOnline(context)) {
            remoteRepo.sendToRemote(message)
        } else {
            dbHelper.saveToLocal(message)
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

}