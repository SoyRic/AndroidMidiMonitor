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

data class MidiDeviceItem(
    val info: MidiDeviceInfo,
    val label: String
)


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


    private var inputItems: List<MidiDeviceItem?> = emptyList()
    private var thruItems: List<MidiDeviceItem?> = emptyList()
    private var isRefreshingDevices = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔴 MUST be first UI init
        logView = findViewById(R.id.logView)

        inputSpinner = findViewById(R.id.inputSpinner)
        thruSpinner = findViewById(R.id.thruSpinner)

        midiController = MidiController(
            context = this,
            logger = ::log
        )

        setupDevicePickers()
    }
/*
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
*/

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
        refreshInputDevices()
        refreshThruDevices()
    }
/*
    private fun setupDevicePickers() {
        val devices = midiController.listDevices()

        val inputCandidates = devices.filter { it.outputPortCount > 0 }
        val thruCandidates = devices.filter { it.inputPortCount > 0 }

        val inputNames = mutableListOf("— Select INPUT device —")
        inputNames.addAll(
            midiController.inputDevices.map {
                it.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unnamed MIDI device"
            }
        )


        val thruNames = mutableListOf("— Select THRU device —")
        thruNames.addAll(
            midiController.thruDevices.map {
                it.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unnamed MIDI device"
            }
        )


        inputSpinner.setSelection(0, false)
        thruSpinner.setSelection(0, false)

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
                    // 0 = “— Select INPUT device —”
                    if (position == 0) {
                        midiController.disconnectInput()
                        log("Input disconnected")
                        return
                    }

                    val device = midiController.inputDevices[position - 1]
                    midiController.connectInput(device)
                    log(
                        "Input connected: ${
                            device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                ?: "Unnamed MIDI device"
                        }"
                    )

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
                    if (position == 0) {
                        midiController.disconnectThru()
                        log("Thru disconnected")
                        return
                    }

                    val device = midiController.thruDevices[position - 1]
                    midiController.connectThru(device)
                    log(
                        "Input connected: ${
                            device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                                ?: "Unnamed MIDI device"
                        }"
                    )

                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

    }
*/
private fun refreshInputDevices() {
    isRefreshingDevices = true

    inputItems = listOf(null) + midiController.inputDevices.map {
        MidiDeviceItem(
            info = it,
            label = it.properties.getString(
                MidiDeviceInfo.PROPERTY_NAME
            ) ?: "Unnamed MIDI device"
        )
    }

    val adapter = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_item,
        inputItems.map { it?.label ?: "— Select INPUT device —" }
    )

    adapter.setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item
    )

    inputSpinner.adapter = adapter
    inputSpinner.setSelection(0, false)

    inputSpinner.onItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isRefreshingDevices) return

                val item = inputItems[position]

                if (item == null) {
                    midiController.disconnectInput()
                    log("Input disconnected")
                    return
                }

                midiController.connectInput(item.info)
                log("Input connected: ${item.label}")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    inputSpinner.adapter = adapter
    inputSpinner.setSelection(0, false)

    isRefreshingDevices = false
}

    private fun refreshThruDevices() {
        isRefreshingDevices = true

            thruItems = listOf(null) + midiController.thruDevices.map {
            MidiDeviceItem(
                info = it,
                label = it.properties.getString(
                    MidiDeviceInfo.PROPERTY_NAME
                ) ?: "Unnamed MIDI device"
            )
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            thruItems.map { it?.label ?: "— Select THRU device —" }
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        thruSpinner.adapter = adapter
        thruSpinner.setSelection(0, false)

        thruSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isRefreshingDevices) return

                    val item = thruItems[position]

                    if (item == null) {
                        midiController.disconnectThru()
                        log("Thru disconnected")
                        return
                    }

                    midiController.connectThru(item.info)
                    log("Thru connected: ${item.label}")
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        thruSpinner.adapter = adapter
        thruSpinner.setSelection(0, false)

        isRefreshingDevices = false

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
            (logView.parent as ScrollView).post {
                (logView.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
            }
        }
    }

}
