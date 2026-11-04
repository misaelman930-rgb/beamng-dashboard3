package com.beamng.dashboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Оболочка для панели BeamNG Radio на ПК.
 * Открывает http://<IP ПК>:8765/digital во весь экран.
 * Адрес ПК запоминается; кнопка ⚙ в углу меняет его.
 */
class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("dashboard", Context.MODE_PRIVATE) }
    private lateinit var web: WebView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Экран не гаснет, пока открыта панель.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = WebView(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webViewClient = object : WebViewClient() {
                // Ссылки остаются внутри панели.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) showError()
                }
            }
        }

        status = TextView(this).apply {
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF000000.toInt())
            visibility = View.GONE
        }

        val gear = Button(this).apply {
            text = "⚙"
            alpha = 0.35f
            setOnClickListener { askHost() }
        }

        val root = FrameLayout(this)
        root.addView(web, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(status, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(gear, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.TOP))
        setContentView(root)

        val host = savedHost()
        if (host.isEmpty()) askHost() else load(host)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Полноэкранный режим: прячем панели системы.
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private fun savedHost(): String = prefs.getString("host", "") ?: ""

    private fun load(host: String) {
        status.visibility = View.GONE
        web.loadUrl("http://$host:8765/digital")
    }

    private fun showError() {
        status.text = "Нет связи с ПК.\nПроверьте Wi-Fi, что BeamNG Radio запущено\nи адрес ПК (кнопка ⚙).\nПовтор через 3 секунды…"
        status.visibility = View.VISIBLE
        val host = savedHost()
        if (host.isNotEmpty()) {
            web.postDelayed({ load(host) }, 3000)
        }
    }

    private fun askHost() {
        val input = EditText(this).apply {
            hint = "192.168.0.197"
            setText(savedHost())
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
        }
        AlertDialog.Builder(this)
            .setTitle("Адрес ПК с BeamNG Radio")
            .setMessage("IP из строки состояния приложения. Порт 8765 подставляется сам.")
            .setView(input)
            .setPositiveButton("Подключиться") { _, _ ->
                val host = input.text.toString().trim()
                if (host.isNotEmpty()) {
                    prefs.edit().putString("host", host).apply()
                    load(host)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
