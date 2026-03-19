package bypass.whitelist

import android.util.Log
import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import mobile.Mobile
import mobile.LogCallback

class MainActivity : AppCompatActivity() {

    companion object {
        private const val VK_CALL_LINK = "https://vk.com/call/join/6kGvQofFiOyjuK0w_BeQvAx8gTsUg0PKUVzWt0VmkIg" // Open call page on app start
    }

    private lateinit var webView: WebView
    private lateinit var logView: TextView
    private lateinit var urlInput: EditText

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            appendLog("VPN permission denied")
        }
    }

    private val hookJs by lazy {
        assets.open("hook.js").bufferedReader().readText()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logView = findViewById(R.id.logView)
        urlInput = findViewById(R.id.urlInput)
        webView = findViewById(R.id.webView)

        setupWebView()
        startRelay()

        val goButton = findViewById<Button>(R.id.goButton)
        goButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                appendLog("Loading: $url")
                webView.loadUrl(url)
            }
        }

        if (VK_CALL_LINK.isNotEmpty()) {
            urlInput.setText(VK_CALL_LINK)
            goButton.performClick()
        }
    }

    private fun updateVpnStatus(status: VpnStatus) {
        TunnelVpnService.instance?.updateStatus(status)
    }

    private fun startRelay() {
        val cb = LogCallback { msg ->
            appendLog(msg)
            if (msg.contains("browser connected")) updateVpnStatus(VpnStatus.TUNNEL_ACTIVE)
            else if (msg.contains("ws read error")) updateVpnStatus(VpnStatus.TUNNEL_LOST)
        }
        Thread {
            try {
                Mobile.startJoiner(9000, 1080, cb)
            } catch (e: Exception) {
                appendLog("Relay error: ${e.message}")
            }
        }.start()
        appendLog("Relay started (SOCKS5 :1080, WS :9000)")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowContentAccess = true
            allowFileAccess = true
            databaseEnabled = true
            setSupportMultipleWindows(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.addJavascriptInterface(JsBridge(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }

            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                val text = msg.message()
                Log.d("VK-HOOK", text)
                if (text.contains("[VK-HOOK]")) {
                    appendLog(text)
                    if (text.contains("CALL CONNECTED")) updateVpnStatus(VpnStatus.CALL_CONNECTED)
                    else if (text.contains("DataChannel open")) updateVpnStatus(VpnStatus.DATACHANNEL_OPEN)
                    else if (text.contains("DataChannel closed")) updateVpnStatus(VpnStatus.DATACHANNEL_LOST)
                    else if (text.contains("WebSocket connected")) updateVpnStatus(VpnStatus.TUNNEL_ACTIVE)
                    else if (text.contains("WebSocket disconnected")) updateVpnStatus(VpnStatus.TUNNEL_LOST)
                    else if (text.contains("Connection state: connecting")) updateVpnStatus(VpnStatus.CONNECTING)
                    else if (text.contains("Connection state: disconnected")) updateVpnStatus(VpnStatus.CALL_DISCONNECTED)
                    else if (text.contains("Connection state: failed")) updateVpnStatus(VpnStatus.CALL_FAILED)
                }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                appendLog("Page loaded, injecting hook...")
                view.evaluateJavascript("(function(){var o=HTMLMediaElement.prototype.play;HTMLMediaElement.prototype.play=function(){this.muted=true;this.volume=0;return o.call(this)}})()", null)
                view.evaluateJavascript(hookJs, null)
            }
        }
    }

    private fun appendLog(msg: String) {
        runOnUiThread {
            val clean = msg.replace("[VK-HOOK] ", "")
            logView.append("$clean\n")
            val scrollAmount = logView.layout?.let {
                it.getLineTop(logView.lineCount) - logView.height
            } ?: 0
            if (scrollAmount > 0) logView.scrollTo(0, scrollAmount)
        }
    }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, TunnelVpnService::class.java)
        startService(intent)
        appendLog("VPN started")
        updateVpnStatus(VpnStatus.TUNNEL_ACTIVE)
    }


    inner class JsBridge {
        @JavascriptInterface
        fun log(msg: String) {
            appendLog(msg)
        }

        @JavascriptInterface
        fun onTunnelReady() {
            appendLog("Tunnel ready, starting VPN...")
            updateVpnStatus(VpnStatus.TUNNEL_ACTIVE)
            runOnUiThread { requestVpn() }
        }
    }
}
