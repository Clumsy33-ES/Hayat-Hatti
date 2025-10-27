package com.example.myapplication.data

import android.util.Log

class DatabaseHelper {
    // Yerel kaydı simüle etmek için (PostgreSQL) bağlantısını sağlar.
    private val localCache = mutableListOf<String>()

    fun saveToLocal(message: String) {
        localCache.add(message)
        Log.d("DB", "Saved locally (PostgreSQL mock): $message")
    }
    fun getLocalData(): List<String> = localCache

    fun clearLocalData() {
        localCache.clear()
    }
}