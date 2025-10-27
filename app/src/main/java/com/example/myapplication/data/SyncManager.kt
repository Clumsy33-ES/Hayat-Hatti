package com.example.myapplication.data

import android.util.Log
//Bu sınıf internet durumunu izler ve bağlantı gelince verileri senkronize eder.
class SyncManager (
    private val dbHelper: DatabaseHelper,
    private val remoteRepo: RemoteRepository
    ){
    fun syncData() {
        val cachedData = dbHelper.getLocalData()

        if (cachedData.isNotEmpty()) {
            cachedData.forEach { message ->
                remoteRepo.sendToRemote(message)
            }
            dbHelper.clearLocalData()
            Log.d("SYNC", "All cached data sent to MongoDB.")
        } else {
            Log.d("SYNC", "No local data to sync.")
        }
    }




}

