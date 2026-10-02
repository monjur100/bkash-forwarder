package com.ilhamx.bkashforwarder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject

class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val json = JSONObject()
            .put("trx_id", inputData.getString("trx"))
            .put("amount", inputData.getString("amount"))
            .put("sender", inputData.getString("sender"))
            .put("sms_ts", inputData.getLong("sms_ts", 0))
            .toString()

        val code = Api.post(Prefs.endpoint(applicationContext), Prefs.secret(applicationContext), json)
        return when {
            code in 200..299 -> Result.success()
            code == 400 -> Result.failure() // ডেটাই ভুল, রিট্রাই করে লাভ নেই
            else -> Result.retry()          // নেটওয়ার্ক, 401, 5xx ইত্যাদি
        }
    }
}
