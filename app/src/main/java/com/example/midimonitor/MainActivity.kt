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
import android.widget.ScrollView
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import android.content.Context


class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var midiController: MidiController
    private lateinit var inputSpinner: Spinner
    private lateinit var thruSpinner: Spinner
    private lateinit var midiManager: MidiManager
    private var selectedInputDevice: MidiDeviceInfo? = null
    private var selectedThruDevice: MidiDeviceInfo? = null
    private val inputDevices = mutableListOf<MidiDeviceInfo>()
    private val thruDevices  = mutableListOf<MidiDeviceInfo>()


/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1️⃣ Bind views FIRST
        inputSpinner = findViewById(R.id.inputSpinner)
        thruSpinner = findViewById(R.id.thruSpinner)
        logView = findViewById(R.id.logView)

        // 2️⃣ Init controller
        midiController = MidiController(this, ::log)

        refreshDeviceLists()
        setupDevicePickers()


        // 3️⃣ Wire UI logic
        setupDevicePickers()
    }
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1️⃣ Get MIDI manager
        midiManager = getSystemService(Context.MIDI_SERVICE) as MidiManager

        // 2️⃣ Find views
        inputSpinner = findViewById(R.id.inputSpinner)
        thruSpinner = findViewById(R.id.thruSpinner)
        logView = findViewById(R.id.logView)

        midiController = MidiController(
            context = this,
            logger = ::log
        )

    // 3️⃣ Setup spinners & listeners
        setupDevicePickers()

        // 4️⃣ Initial population
        refreshDeviceLists()

        // 5️⃣ REGISTER DEVICE CALLBACK  ← 🔴 THIS IS THE PLACE
        midiManager.registerDeviceCallback(
            object : MidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo) {
                    refreshDeviceLists()
                }

                override fun onDeviceRemoved(device: MidiDeviceInfo) {
                    refreshDeviceLists()
                }
            },
            Handler(Looper.getMainLooper())
        )
    }


    private fun refreshDeviceLists() {
        inputDevices.clear()
        thruDevices.clear()

        val devices = midiController.getDevices()

        for (device in devices) {
            val outputCount = device.outputPortCount
            val inputCount  = device.inputPortCount

            if (outputCount > 0) {
                inputDevices.add(device)   // devices that SEND MIDI
            }

            if (inputCount > 0) {
                thruDevices.add(device)    // devices that RECEIVE MIDI
            }
        }

        log("Devices refreshed: inputs=${inputDevices.size}, thru=${thruDevices.size}")
    }

    private fun setupDevicePickers() {
        val devices = midiController.listDevices()

        val inputCandidates = devices.filter { it.outputPortCount > 0 }
        val thruCandidates = devices.filter { it.inputPortCount > 0 }

        inputSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item,
                inputCandidates.map { deviceLabel(it) })

        thruSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item,
                thruCandidates.map { deviceLabel(it) })


        inputSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedInputDevice = inputDevices[position]

                    if (selectedInputDevice == selectedThruDevice) {
                        log("Input and Thru cannot be the same device")
                        return
                    }

                    midiController.openInputDevice(selectedInputDevice!!)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // no-op
                }
            }

        thruSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedThruDevice = thruDevices[position]

                    if (selectedThruDevice == selectedInputDevice) {
                        log("Thru and Input cannot be the same device")
                        return
                    }

                    midiController.openThruDevice(selectedThruDevice!!)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

    }

    private fun deviceLabel(info: MidiDeviceInfo): String {
        val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown"
        return "$name (in=${info.inputPortCount}, out=${info.outputPortCount})"
    }


    override fun onDestroy() {
        super.onDestroy()
        midiController.stop()
    }

    private fun log(msg: String) {
        runOnUiThread {
            logView.append(msg + "\n")

            val parent = logView.parent as ScrollView
            parent.post {
                parent.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

}
