package com.example.midimonitor

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Button
import android.widget.ToggleButton
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
    private var debugEnabled = true
    private lateinit var sustainToggle: ToggleButton
    private lateinit var retriggerButton: Button
    private lateinit var debugToggle: ToggleButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sustainToggle = findViewById(R.id.sustainToggle)
        retriggerButton = findViewById(R.id.sustainPulse)
        debugToggle = findViewById(R.id.debugToggle)
        logView = findViewById(R.id.logView)
        inputSpinner = findViewById(R.id.inputSpinner)
        thruSpinner = findViewById(R.id.thruSpinner)

        // Initialize MidiController with logger
        midiController = MidiController(
            context = this,
            logger = ::log,
            isDebugEnabled = { debugEnabled }
        )

        sustainToggle.setOnCheckedChangeListener { _, enabled ->
            midiController.setSustainMode(enabled)
            log("Sustain mode: ${if (enabled) "ON" else "OFF"}")
        }
        retriggerButton.setOnClickListener {
            midiController.retriggerSustain()
            sustainToggle.setText("Sustain ON")
            sustainToggle.setChecked(true)
            log("Sustain retriggered")
        }
        debugToggle.setOnCheckedChangeListener { _, enabled ->
            debugEnabled = enabled
            //midiController.setDebugEnabled(enabled)
            log("Debug ${if (enabled) "enabled" else "disabled"}")
        }

        setupDevicePickers()

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
