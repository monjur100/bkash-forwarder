package com.ilhamx.bkashforwarder

import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Api {
    /** সাইন করা JSON পাঠায়। HTTP কোড ফেরত দেয়; নেটওয়ার্ক সমস্যা হলে -1। */
    fun post(endpoint: String, secret: String, json: String): Int {
        val ts = (System.currentTimeMillis() / 1000).toString()
        val sig = hmacSha256(secret, "$ts.$json")
        return try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Timestamp", ts)
                setRequestProperty("X-Signature", sig)
            }
            conn.outputStream.use { it.write(json.toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            code
        } catch (e: Exception) {
            -1
        }
    }

    private fun hmacSha256(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
