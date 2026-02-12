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
    private var thruInputPort: MidiInputPort? = null

    //private var lastThruMessage: ByteArray? = null
    private var synthDeviceId: Int? = null
    private var inputDevice: MidiDevice? = null
    private var inputPort: MidiOutputPort? = null

    private var thruDevice: MidiDevice? = null
    private var thruPort: MidiInputPort? = null

    private var selectedInputInfo: MidiDeviceInfo? = null
    private var selectedThruInfo: MidiDeviceInfo? = null


    private fun deviceLabel(info: MidiDeviceInfo): String {
        return info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: "MIDI Device ${info.id}"
    }

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
        //midiManager.devices.forEach { openDevice(it) }

        // openThruDevice()
/*
        val devices = midiManager.devices

        if (devices.size >= 2) {
            selectedInputInfo = devices[0]
            selectedThruInfo = devices[1]

            if (selectedInputInfo!!.id != selectedThruInfo!!.id) {
                openInputDevice(selectedInputInfo!!)
                openThruDevice(selectedThruInfo!!)
            } else {
                log("Input and Thru are same device — refused")
            }
        }

        val devices = midiManager.devices

        val inputCandidates = devices.filter { it.outputPortCount > 0 }
        val thruCandidates  = devices.filter { it.inputPortCount > 0 }

        if (inputCandidates.isEmpty()) {
            log("No MIDI input devices found (outputPortCount > 0)")
            return
        }

        selectedInputInfo = inputCandidates.first()
        openInputDevice(selectedInputInfo!!)

        if (thruCandidates.isNotEmpty()) {
            selectedThruInfo = thruCandidates.first()
            openThruDevice(selectedThruInfo!!)
        }
*/
        val devices = midiManager.devices

// ---- INPUT: prefer physical USB MIDI devices ----
        val inputCandidates = devices.filter { info ->
            info.outputPortCount > 0 &&
                    info.properties.containsKey(MidiDeviceInfo.PROPERTY_USB_DEVICE)
        }

        if (inputCandidates.isEmpty()) {
            log("No physical MIDI keyboard found")
            return
        }

        selectedInputInfo = inputCandidates.first()
        log("Selected INPUT: ${deviceLabel(selectedInputInfo!!)}")
        openInputDevice(selectedInputInfo!!)

// ---- THRU: any device that accepts MIDI, except the input ----
        val thruCandidates = devices.filter { info ->
            info.inputPortCount > 0 &&
                    info.id != selectedInputInfo!!.id
        }

        if (thruCandidates.isEmpty()) {
            log("No MIDI thru device found")
            return
        }

        selectedThruInfo = thruCandidates.first()
        log("Selected THRU: ${deviceLabel(selectedThruInfo!!)}")
        openThruDevice(selectedThruInfo!!)


    }

    private fun openInputDevice(info: MidiDeviceInfo) {
        closeInput()

        // 🔍 STEP 1 — LOG PORT COUNTS
        log(
            "Opening INPUT device: ${deviceLabel(info)} " +
                    "inputPorts=${info.inputPortCount} " +
                    "outputPorts=${info.outputPortCount}"
        )

        midiManager.openDevice(info, { device ->
            inputDevice = device

            if (info.outputPortCount == 0) {
                log("Device has NO output ports — cannot receive MIDI")
                return@openDevice
            }

            inputPort = device.openOutputPort(0)

            if (inputPort == null) {
                log("openOutputPort(0) returned NULL")
                return@openDevice
            }

            inputPort!!.connect(midiReceiver)
            log("Input connected successfully")

        }, Handler(Looper.getMainLooper()))
    }


    private fun openThruDevice(info: MidiDeviceInfo) {
        closeThru()

        midiManager.openDevice(info, { device ->
            thruDevice = device
            thruPort = device.openInputPort(0)
            log("Thru opened: ${deviceLabel(info)}")
        }, Handler(Looper.getMainLooper()))
    }

    private fun closeInput() {
        inputPort?.close()
        inputDevice?.close()
        inputPort = null
        inputDevice = null
    }

    private fun closeThru() {
        thruPort?.close()
        thruDevice?.close()
        thruPort = null
        thruDevice = null
    }

    // ---------------------------------------------------------------------
    // MIDI device management
    // ---------------------------------------------------------------------


    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            log("MIDI device added: ${deviceLabel(device)}")
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            log("MIDI device removed")
        }
    }


    private fun openThruDevice() {
        midiManager.devices.forEach { info ->
            val isSynth =
                info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?.contains("synth", ignoreCase = true) == true

            if (isSynth && info.inputPortCount > 0) {
                log("Opening THRU device: ${info.properties}")

                midiManager.openDevice(info, { device ->
                    if (device == null) return@openDevice

                    thruDevice = device
                    thruInputPort = device.openInputPort(0)

                    if (thruInputPort != null)
                        log("MIDI THRU output ready")
                    else
                        log("Failed to open THRU input port")

                }, Handler(Looper.getMainLooper()))
            }

            synthDeviceId = info.id

        }
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
            log("RX MIDI $count bytes")

            if (thruPort == null) {
                log("THRU port is NULL")
                return
            }

            log("Calling thruPort.send()")
            thruPort!!.send(data, offset, count)
            log("thruPort.send() done")
        }
    }

    /*
    private val midiReceiver = object : MidiReceiver() {

        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            log(
                "RAW MIDI: " + data.copyOfRange(offset, offset + count)
                    .joinToString(" ") { String.format("%02X", it) })

            parseMidi(data, offset, count)
        }


        private fun parseMidi(data: ByteArray, offset: Int, count: Int) {
            if (count < 1) return

            val status = data[offset].toInt() and 0xFF
            val type = status and 0xF0

            when (type) {
                0x90 -> { // NOTE ON
                    if (count < 3) return
                    val note = data[offset + 1].toInt() and 0x7F
                    val velocity = data[offset + 2].toInt() and 0x7F
                    if (velocity > 0)
                        log("NOTE ON  note=$note vel=$velocity")
                    else
                        log("NOTE OFF note=$note")
                }

                0x80 -> { // NOTE OFF
                    if (count < 3) return
                    val note = data[offset + 1].toInt() and 0x7F
                    log("NOTE OFF note=$note")
                }

                0xB0 -> { // CONTROL CHANGE
                    if (count < 3) return
                    val cc = data[offset + 1].toInt() and 0x7F
                    val value = data[offset + 2].toInt() and 0x7F
                    log("CC $cc = $value")
                }

                else -> {
                    log("Unhandled MIDI status: 0x${status.toString(16)}")
                }
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
    }
    */

    // ---------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------

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
        fun onDestroy() {
            super.onDestroy()
            thruInputPort?.close()
            thruDevice?.close()
            closeDevice()
        }
    }

}

