//3️⃣ MidiController (device + port management)
//
//This class owns everything Android-MIDI-related.

package com.example.midimonitor

import android.content.Context
import android.media.midi.*
import android.media.midi.MidiInputPort
import android.os.Handler
import android.os.Looper

class MidiController(
    private val context: Context,
    private val logger: (String) -> Unit
) {

    private val midiManager =
        context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    private var inputDevice: MidiDevice? = null
    private var thruDevice: MidiDevice? = null

    private var inputPort: MidiInputPort? = null
    private var thruPort: MidiInputPort? = null


    private val processor = MidiProcessor(logger)

    fun start() {
        midiManager.registerDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        midiManager.devices.forEach { tryOpen(it) }
    }

    fun stop() {
        inputPort?.close()
        thruPort?.close()
        inputDevice?.close()
        thruDevice?.close()
    }

    private val deviceCallback = object : MidiManager.DeviceCallback() {

        override fun onDeviceAdded(device: MidiDeviceInfo) {
            tryOpen(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            logger("MIDI device removed")
        }
    }

    private fun tryOpen(info: MidiDeviceInfo) {
        val inputs = info.inputPortCount
        val outputs = info.outputPortCount

        logger("Found ${info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)} " +
                "in=$inputs out=$outputs")

        when {
            inputs > 0 && inputDevice == null -> openInput(info)
            outputs > 0 && thruDevice == null -> openThru(info)
        }
    }

    private fun openInput(info: MidiDeviceInfo) {
        midiManager.openDevice(info, { device ->
            inputDevice = device

            val outputPort = device.openOutputPort(0)
            outputPort?.connect(processor.receiver)

            logger("INPUT connected (device output → receiver)")
        }, Handler(Looper.getMainLooper()))
    }

    private fun openThru(info: MidiDeviceInfo) {
        midiManager.openDevice(info, { device ->
            thruDevice = device
            thruPort = device.openInputPort(0)      // ✅ InputPort
            processor.attachThru(thruPort!!)
            logger("THRU connected")
        }, Handler(Looper.getMainLooper()))
    }

}

