package com.example.midimonitor

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.media.midi.MidiDeviceInfo   // ✅ Add this

data class MidiDeviceItem(
    val info: MidiDeviceInfo,
    val label: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var inputSpinner: Spinner
    private lateinit var thruSpinner: Spinner
    private lateinit var midiController: MidiController

    private var isRefreshingDevices = false
    private var inputItems: List<MidiDeviceItem?> = emptyList()
    private var thruItems: List<MidiDeviceItem?> = emptyList()
    private lateinit var filterSpinner: Spinner
    private var selectedFilter: MidiFilterType = MidiFilterType.NONE

    private fun setupFilterSpinner() {
        filterSpinner = findViewById(R.id.filterSpinner)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            MidiFilterType.values().map { it.label }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                //selectedFilter = MidiFilterType.values()[position]
                val filter = MidiFilterType.values()[position]
                midiController.setFilter(filter)
                log("MIDI Filter set to: ${selectedFilter.label}")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        inputSpinner = findViewById(R.id.inputSpinner)
        thruSpinner = findViewById(R.id.thruSpinner)

        // Initialize MidiController with logger
        midiController = MidiController(
            context = this,
            logger = ::log,
        )

        log("\n======================\n       OnCreate running\n======================\n")

        setupDevicePickers()

        setupFilterSpinner()

    }

    private fun setupDevicePickers() {
        refreshInputDevices()
        refreshThruDevices()
    }

    private fun refreshInputDevices() {
        isRefreshingDevices = true

        inputItems = listOf(null) + midiController.inputDevices.map { device ->
            MidiDeviceItem(
                info = device,
                label = device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unnamed INPUT device"
            )
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            inputItems.map { it?.label ?: "— Select INPUT device —" }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        inputSpinner.adapter = adapter
        inputSpinner.setSelection(0, false)

        inputSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                if (isRefreshingDevices) return

                val item = inputItems[position]
                if (item == null) {
                    midiController.disconnectInput()
                    log("Input disconnected")
                } else {
                    midiController.connectInput(item.info)
                    log("Input connected: ${item.label}")
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        isRefreshingDevices = false
    }

    private fun refreshThruDevices() {
        isRefreshingDevices = true

        thruItems = listOf(null) + midiController.thruDevices.map { device ->
            MidiDeviceItem(
                info = device,
                label = device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unnamed THRU device"
            )
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            thruItems.map { it?.label ?: "— Select THRU device —" }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        thruSpinner.adapter = adapter
        thruSpinner.setSelection(0, false)

        thruSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                if (isRefreshingDevices) return

                val item = thruItems[position]
                if (item == null) {
                    midiController.disconnectThru()
                    log("Thru disconnected")
                } else {
                    midiController.connectThru(item.info)
                    log("Thru connected: ${item.label}")
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        isRefreshingDevices = false
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

enum class MidiFilterType(val label: String) {
    NONE("None"),
    NOTE_ON("Note On"),
    NOTE_OFF("Note Off"),
    CONTROL_CHANGE("Control Change"),
    PROGRAM_CHANGE("Program Change")
}
