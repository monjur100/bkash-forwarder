package com.ilhamx.bkashforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

data class BkashPayment(val trx: String, val amount: String, val sender: String)

object BkashParser {
    // নমুনা SMS: "You have received Tk 500.00 from 01712345678. Fee Tk 0.00.
    //              Balance Tk 1,234.00. TrxID ABC123XYZ at 02/10/2026 14:30"
    // আপনার ফোনে আসা আসল SMS দেখে প্রয়োজনে regex মিলিয়ে নিন।
    private val amountRe = Regex("""received\s+Tk\s*([\d,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val senderRe = Regex("""from\s+(01\d{9})""", RegexOption.IGNORE_CASE)
    private val trxRe = Regex("""TrxID\s+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)

    fun parse(body: String): BkashPayment? {
        val amount = amountRe.find(body)?.groupValues?.get(1)?.replace(",", "") ?: return null
        val sender = senderRe.find(body)?.groupValues?.get(1) ?: return null
        val trx = trxRe.find(body)?.groupValues?.get(1)?.uppercase() ?: return null
        return BkashPayment(trx, amount, sender)
    }
}

class BkashSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val from = msgs.firstOrNull()?.originatingAddress ?: return
        if (!from.equals("bKash", ignoreCase = true)) return

        // লম্বা SMS একাধিক অংশে আসতে পারে, তাই জোড়া দেওয়া হচ্ছে
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        val p = BkashParser.parse(body) ?: return

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    "trx" to p.trx,
                    "amount" to p.amount,
                    "sender" to p.sender,
                    "sms_ts" to System.currentTimeMillis() / 1000
                )
            )
            .build()

        // একই TrxID এর জন্য একবারই কাজ তৈরি হবে
        WorkManager.getInstance(context)
            .enqueueUniqueWork("trx-${p.trx}", ExistingWorkPolicy.KEEP, request)
    }
}
