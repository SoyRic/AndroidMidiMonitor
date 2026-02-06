package com.example.midimonitor

import android.media.midi.*
import android.os.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box



class MainActivity : AppCompatActivity() {

    private lateinit var midiManager: MidiManager
    private lateinit var logView: TextView

    private var midiDevice: MidiDevice? = null
    private var outputPort: MidiOutputPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)

        log("=== MidiMonitor STARTED ===")

        midiManager = getSystemService(MIDI_SERVICE) as MidiManager

        log("Enumerating MIDI devices...")
        midiManager.devices.forEach { info ->
            log("Found MIDI device: ${info.properties}")
        }

        midiManager.registerDeviceCallback(
            deviceCallback,
            Handler(Looper.getMainLooper())
        )

        // Connect already-present devices
        midiManager.devices.forEach { openDevice(it) }
    }

    // ---------------------------------------------------------------------
    // MIDI device management
    // ---------------------------------------------------------------------

    private val deviceCallback = object : MidiManager.DeviceCallback() {

        override fun onDeviceAdded(device: MidiDeviceInfo) {
            log("MIDI device added")
            openDevice(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            log("MIDI device removed")
            closeDevice()
        }
    }

    private fun openDevice(info: MidiDeviceInfo) {
        midiManager.openDevice(info, { device ->
            if (device == null) return@openDevice

            midiDevice = device
            log("Opened MIDI device")

            outputPort = device.openOutputPort(0)
            outputPort?.connect(midiReceiver)

        }, Handler(Looper.getMainLooper()))
    }

    private fun closeDevice() {
        outputPort?.close()
        midiDevice?.close()
        outputPort = null
        midiDevice = null
    }

    // ---------------------------------------------------------------------
    // MIDI input
    // ---------------------------------------------------------------------

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(
            data: ByteArray,
            offset: Int,
            count: Int,
            timestamp: Long
        ) {
            handleRawMidi(data, offset, count)
        }
    }

    private fun handleRawMidi(data: ByteArray, offset: Int, count: Int) {
        if (count < 1) return

        val status = data[offset].toInt() and 0xFF
        val type = status and 0xF0
        val channel = status and 0x0F

        processMidiMessage(type, channel, data, offset, count)
    }

    // ---------------------------------------------------------------------
    // MIDI processing (FILTER / MODIFY / ADD)
    // ---------------------------------------------------------------------

    private fun processMidiMessage(
        type: Int,
        channel: Int,
        data: ByteArray,
        offset: Int,
        count: Int
    ) {
        when (type) {

            0x90 -> { // NOTE ON
                if (count < 3) return

                var note = data[offset + 1].toInt() and 0x7F
                val velocity = data[offset + 2].toInt() and 0x7F

                // ---- FILTER example ----
                // Ignore zero-velocity Note On (treat as Note Off)
                if (velocity == 0) {
                    log("NOTE OFF ch=$channel note=$note")
                    return
                }

                // ---- MODIFY example ----
                // Transpose up one octave
                note = (note + 12).coerceAtMost(127)

                log("NOTE ON  ch=$channel note=$note vel=$velocity")
            }

            0x80 -> { // NOTE OFF
                if (count < 3) return

                val note = data[offset + 1].toInt() and 0x7F
                log("NOTE OFF ch=$channel note=$note")
            }

            // ---- FILTER example ----
            0xD0 -> {
                // Channel aftertouch — intentionally ignored
            }

            else -> {
                // Other MIDI messages (CC, pitch bend, etc.)
                // Keep silent for now
            }
        }
    }

    // ---------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------

    private fun log(msg: String) {
        runOnUiThread {
            logView.append("$msg\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeDevice()
    }
}
