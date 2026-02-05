package com.example.midimonitor

import android.media.midi.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var midiManager: MidiManager
    private lateinit var logView: TextView

    private var midiDevice: MidiDevice? = null
    private var outputPort: MidiOutputPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        midiManager = getSystemService(MIDI_SERVICE) as MidiManager

        // Listen for hot-plug
        midiManager.registerDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))

        // Try already-connected devices
        midiManager.devices.forEach { openDevice(it) }
    }

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

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            parseMidi(data, offset, count)
        }
    }

    private fun parseMidi(data: ByteArray, offset: Int, count: Int) {
        val status = data[offset].toInt() and 0xFF

        when (status and 0xF0) {
            0x90 -> { // NOTE ON
                val note = data[offset + 1].toInt() and 0x7F
                val velocity = data[offset + 2].toInt() and 0x7F
                if (velocity > 0)
                    log("NOTE ON  note=$note vel=$velocity")
                else
                    log("NOTE OFF note=$note")
            }
            0x80 -> { // NOTE OFF
                val note = data[offset + 1].toInt() and 0x7F
                log("NOTE OFF note=$note")
            }
        }
    }

    private fun log(msg: String) {
        runOnUiThread {
            logView.append("$msg\n")
        }
    }

    private fun closeDevice() {
        outputPort?.close()
        midiDevice?.close()
        outputPort = null
        midiDevice = null
    }

    override fun onDestroy() {
        super.onDestroy()
        closeDevice()
    }
}
