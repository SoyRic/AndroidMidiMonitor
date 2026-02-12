//2️⃣ What stays in MainActivity
//
//MainActivity should not deal with ports or receivers.
//
//It should only:
//
//display logs
//
//react to lifecycle
//
//select input / thru devices
//
//✔ No MIDI logic here
//✔ No Android MIDI APIs leaking into UI
package com.example.midimonitor

import android.media.midi.*
import android.os.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box


class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var midiController: MidiController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)

        midiController = MidiController(
            context = this,
            logger = ::log
        )

        midiController.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        midiController.stop()
    }

    private fun log(msg: String) {
        runOnUiThread {
            logView.append(msg + "\n")
        }
    }
}
