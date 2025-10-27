package com.example.myapplication.data

import android.util.Log

class RemoteRepository {
    fun sendToRemote(message: String) {
        // Burada gerçek HTTP isteği olacak (örnek)
        Log.d("REMOTE", "Sent to MongoDB Atlas: $message")
    }
}