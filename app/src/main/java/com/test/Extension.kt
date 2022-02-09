package com.test

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SplashActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 700)
    }

    override fun onPause() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        super.onPause()
    }
}

fun Int.toPx() = this * Resources.getSystem().displayMetrics.density

fun Context.toast(msg: String?) {
    Toast.makeText(this, msg ?: return, Toast.LENGTH_SHORT).show()
}