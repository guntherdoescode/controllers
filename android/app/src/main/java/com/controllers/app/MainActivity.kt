package com.controllers.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// this is the main activity, where all the stuff starts
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var addressInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectBtn: Button
    private lateinit var gamepadView: GamepadView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init the ui stuff
        statusText = findViewById(R.id.statusText)
        addressInput = findViewById(R.id.addressInput)
        portInput = findViewById(R.id.portInput)
        connectBtn = findViewById(R.id.connectBtn)
        gamepadView = findViewById(R.id.gamepadView)

        addressInput.setText("192.168.1.100")

        connectBtn.setOnClickListener {
            toggleConnection()
        }
    }

    // toggle connections
    private fun toggleConnection() {
        if (NativeBridge.isConnected()) {
            disconnect()
        } else {
            connect()
        }
    }

    // connect to the server, probably works
    private fun connect() {
        val addr = addressInput.text.toString().trim()
        val port = portInput.text.toString().trim().toIntOrNull() ?: 42069

        CoroutineScope(Dispatchers.IO).launch {
            val success = NativeBridge.connect(addr, port)
            runOnUiThread {
                if (success) {
                    statusText.text = "Connected to $addr:$port"
                    connectBtn.text = "Disconnect"
                    gamepadView.setConnected(true)
                } else {
                    statusText.text = "Connection failed"
                }
            }
        }
    }

    // disconnect, clean up
    private fun disconnect() {
        NativeBridge.disconnect()
        statusText.text = "Disconnected"
        connectBtn.text = "Connect"
        gamepadView.setConnected(false)
    }

    // cleanup when activity is destroyed
    override fun onDestroy() {
        if (NativeBridge.isConnected()) {
            NativeBridge.disconnect()
        }
        super.onDestroy()
    }
}
