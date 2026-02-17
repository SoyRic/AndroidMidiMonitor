package com.example.midimonitor

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
//import androidx.compose.ui.graphics.colorspace.connect
import java.io.IOException

class MidiController(
    context: Context,
    private val logger: (String) -> Unit
) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    val inputDevices: List<MidiDeviceInfo>
        get() = midiManager.devices.filter { it.inputPortCount > 0 }

    val thruDevices: List<MidiDeviceInfo>
        get() = midiManager.devices.filter { it.outputPortCount > 0 }

    private val midiReceiver = MonitorReceiver(logger)
    private var connectedOutputPort: android.media.midi.MidiOutputPort? = null
    private var thruDevice: MidiDeviceInfo? = null

    fun setFilter(filter: MidiFilterType) {
        midiReceiver.filter = filter
        logger("Filter set to: ${filter.label}")
    }

    fun connectInput(deviceInfo: MidiDeviceInfo) {
        logger("Attempting to connect INPUT: ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")

        disconnectInput() // Close previous connection

        midiManager.openDevice(deviceInfo, { device ->
            if (device == null) {
                logger("Error: Could not open INPUT device.")
                return@openDevice
            }

            val outputPort = device.openOutputPort(0)
            if (outputPort == null) {
                logger("Error: Could not open output port 0 on INPUT device.")
                return@openDevice
            }

            outputPort.connect(midiReceiver)
            connectedOutputPort = outputPort
            logger("INPUT successfully connected to ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        }, mainHandler)
    }

    fun connectThru(deviceInfo: MidiDeviceInfo) {
        logger("Attempting to connect THRU: ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")

        disconnectThru()

        midiManager.openDevice(deviceInfo, { device ->
            if (device == null) {
                logger("Error: Could not open THRU device.")
                return@openDevice
            }

            val thruPort = device.openInputPort(0)
            if (thruPort == null) {
                logger("Error: Could not open input port 0 on THRU device.")
                return@openDevice
            }

            midiReceiver.setThruPort(thruPort)
            thruDevice = deviceInfo
            logger("THRU successfully connected to ${deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
        }, mainHandler)
    }

    fun disconnectInput() {
        logger("Disconnecting INPUT")
        try {
            connectedOutputPort?.disconnect(midiReceiver)
            connectedOutputPort?.close()
        } catch (e: IOException) {
            logger("Error disconnecting INPUT: ${e.message}")
        }
        connectedOutputPort = null
    }

    fun disconnectThru() {
        logger("Disconnecting THRU")
        midiReceiver.setThruPort(null)
        thruDevice = null
    }

    fun stop() {
        disconnectInput()
        disconnectThru()
        logger("MIDI Controller stopped and resources released.")
    }
}
