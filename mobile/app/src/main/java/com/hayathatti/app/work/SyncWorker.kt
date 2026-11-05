package com.hayathatti.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hayathatti.app.HayatHattiApp
import com.hayathatti.app.data.ApiResult
import com.hayathatti.app.data.ErrKind
import com.hayathatti.app.data.OutboxRequest
import com.hayathatti.app.data.OutboxRouting
import com.hayathatti.app.data.apiCall

/** Kuyruktaki kayıtları sunucuya gönderir. Ulaşılamazsa WorkManager artan aralıklarla yeniden dener. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as HayatHattiApp).container
        val dao = c.db.outbox()
        var retry = false
        for (item in dao.pending()) {
            val request = OutboxRouting.route(item, c.prefs.session.value?.user?.id, c.prefs.deviceId)
            val result: ApiResult<*> = apiCall {
                when (request) {
                    is OutboxRequest.ToSignals -> c.api.createSignal(request.body)
                    is OutboxRequest.ToBle -> c.api.createBle(request.body)
                }
            }
            when (result) {
                is ApiResult.Ok -> dao.markSent(item.sid, request.path)
                is ApiResult.Err -> when (result.kind) {
                    ErrKind.NETWORK, ErrKind.SERVER -> {
                        dao.markRetry(item.sid, result.message)
                        retry = true
                    }
                    ErrKind.UNAUTHORIZED, ErrKind.REJECTED -> dao.markFailed(item.sid, result.message)
                }
            }
        }
        return if (retry) Result.retry() else Result.success()
    }

    companion object {
        const val NAME = "outbox-sync"
    }
}
