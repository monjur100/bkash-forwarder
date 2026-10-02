package com.ilhamx.bkashforwarder

import android.content.Context

object Prefs {
    private const val FILE = "cfg"
    const val DEFAULT_URL = "https://ilhamx.com/wp-json/bksg/v1/payment"

    fun endpoint(c: Context): String =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("url", DEFAULT_URL) ?: DEFAULT_URL

    fun secret(c: Context): String =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("secret", "") ?: ""

    fun save(c: Context, url: String, secret: String) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("url", url.trim()).putString("secret", secret.trim()).apply()
    }
}
