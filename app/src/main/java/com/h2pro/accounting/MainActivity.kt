package com.h2pro.accounting

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = TextView(this).apply {
            text = "H2pro\n\nنظام محاسبي عربي\n\nمرحباً بك"
            textSize = 24f
            setPadding(32, 48, 32, 32)
        }
        setContentView(view)
    }
}
