package com.example.midimonitor

import android.media.midi.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {

    private lateinit var midiManager: MidiManager
    private lateinit var logView: TextView

    private var midiDevice: MidiDevice? = null
    private var outputPort: MidiOutputPort? = null

    // 1. Add state for the filters
    private var filterState by mutableStateOf(MidiFilterState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.logView)
        midiManager = getSystemService(MIDI_SERVICE) as MidiManager

        // 2. Find the ComposeView and set its content
        findViewById<ComposeView>(R.id.compose_view).setContent {
            // Use MaterialTheme for proper styling
            MaterialTheme {
                MidiFilterUi(
                    filterState = filterState,
                    onFilterChanged = { newState:MidiFilterState  ->
                        filterState = newState
                    }
                )
            }
        }

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

    // 3. Update parseMidi to use the filter state
    private fun parseMidi(data: ByteArray, offset: Int, count: Int) {
        val status = data[offset].toInt() and 0xFF

        when (status and 0xF0) {
            0x90 -> { // NOTE ON
                if (!filterState.noteOn) return // Check filter
                val note = data[offset + 1].toInt() and 0x7F
                val velocity = data[offset + 2].toInt() and 0x7F
                if (velocity > 0)
                    log("NOTE ON  note=$note vel=$velocity")
                else {
                    if (!filterState.noteOff) return // Also check Note Off for 0-velocity notes
                    log("NOTE OFF note=$note")
                }
            }
            0x80 -> { // NOTE OFF
                if (!filterState.noteOff) return // Check filter
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

/*
// 4. Paste the Composable functions into your file (or a new file)
@Composable
fun MidiFilterUi(
    filterState: MidiFilterState,
    onFilterChanged: (MidiFilterState) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Filter MIDI Events", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        FilterCheckbox(
            label = "Note On",
            checked = filterState.noteOn,
            onCheckedChange = { onFilterChanged(filterState.copy(noteOn = it)) }
        )
        FilterCheckbox(
            label = "Note Off",
            checked = filterState.noteOff,
            onCheckedChange = { onFilterChanged(filterState.copy(noteOff = it)) }
        )
    }
}
*/
@Composable
private fun FilterCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

