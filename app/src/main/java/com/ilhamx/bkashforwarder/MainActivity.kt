package com.ilhamx.bkashforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val askSms = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    private lateinit var tvStatus: TextView
    private lateinit var etUrl: EditText
    private lateinit var etSecret: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        etUrl = findViewById(R.id.etUrl)
        etSecret = findViewById(R.id.etSecret)
        etUrl.setText(Prefs.endpoint(this))
        etSecret.setText(Prefs.secret(this))

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            Prefs.save(this, etUrl.text.toString(), etSecret.text.toString())
            toast("সেভ হয়েছে")
        }
        findViewById<Button>(R.id.btnPerm).setOnClickListener {
            askSms.launch(Manifest.permission.RECEIVE_SMS)
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener { testServer() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        tvStatus.text = if (ok) "SMS পারমিশন: চালু ✓" else "SMS পারমিশন: বন্ধ ✗ (নিচের বাটনে চাপুন)"
    }

    /** ভুল (amount=0) ডেটা সাইন করে পাঠায়: সার্ভার কোনো রেকর্ড তৈরি করে না, শুধু সিগনেচার যাচাই করে। */
    private fun testServer() {
        Prefs.save(this, etUrl.text.toString(), etSecret.text.toString())
        val url = Prefs.endpoint(this)
        val secret = Prefs.secret(this)
        if (secret.isEmpty()) { toast("আগে সিক্রেট কী লিখুন"); return }
        Thread {
            val json = JSONObject().put("trx_id", "TEST").put("amount", "0")
                .put("sender", "0").put("sms_ts", 0).toString()
            val code = Api.post(url, secret, json)
            val msg = when (code) {
                400 -> "✓ সংযোগ ও সিক্রেট ঠিক আছে"
                401 -> "✗ সিক্রেট ভুল অথবা ফোনের তারিখ-সময় ভুল"
                404 -> "✗ ঠিকানা ভুল অথবা প্লাগইন চালু নেই"
                -1 -> "✗ ইন্টারনেট বা ঠিকানায় সমস্যা"
                else -> "সার্ভারের উত্তর: $code"
            }
            runOnUiThread { toast(msg) }
        }.start()
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
}
