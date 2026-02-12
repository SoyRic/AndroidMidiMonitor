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

    private val handler = Handler(Looper.getMainLooper())
    private val midiManager =
        context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    private var inputDevice: MidiDevice? = null
    private var thruDevice: MidiDevice? = null

    private var inputPort: MidiOutputPort? = null

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

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(
            data: ByteArray,
            offset: Int,
            count: Int,
            timestamp: Long
        ) {
            // Log raw MIDI (hex or parsed elsewhere)
            logger("MIDI IN: ${data.copyOfRange(offset, offset + count).joinToString { "%02X".format(it) }}")

            // MIDI THRU
            thruPort?.send(data, offset, count)
        }
    }

    private val deviceCallback = object : MidiManager.DeviceCallback() {

        override fun onDeviceAdded(device: MidiDeviceInfo) {
            tryOpen(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            logger("MIDI device removed")
        }
    }

    /*
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
    */

    private fun tryOpen(info: MidiDeviceInfo) {
        val inputs = info.inputPortCount
        val outputs = info.outputPortCount
        val name = deviceName(info)

        logger("Found $name in=$inputs out=$outputs type=${info.type}")

        // INPUT: prefer USB devices that OUTPUT MIDI
        if (isUsbDevice(info) && outputs > 0 && inputDevice == null) {
            logger("→ Selected as INPUT: $name")
            openInput(info)
            return
        }

        // THRU: any device that ACCEPTS MIDI
        if (inputs > 0 && thruDevice == null) {
            logger("→ Selected as THRU: $name")
            openThru(info)
            return
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


    fun listDevices(): List<MidiDeviceInfo> {
        return midiManager.devices.toList()
    }


    fun openInputDevice(info: MidiDeviceInfo) {
        closeInput()

        midiManager.openDevice(info, { device ->
            inputDevice = device
            inputPort = device.openOutputPort(0) // READ FROM HERE
            inputPort?.connect(midiReceiver)
            logger("Input device opened: ${info.properties}")
        }, handler)
    }

    fun openThruDevice(info: MidiDeviceInfo) {
        closeThru()

        midiManager.openDevice(info, { device ->
            thruDevice = device
            thruPort = device.openInputPort(0) // WRITE TO HERE
            logger("Thru device opened: ${info.properties}")
        }, handler)
    }

    fun getDevices(): Array<MidiDeviceInfo> =
        midiManager.devices

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

    private fun isUsbDevice(info: MidiDeviceInfo): Boolean {
        return info.type == MidiDeviceInfo.TYPE_USB
    }

    private fun deviceName(info: MidiDeviceInfo): String =
        info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown"

}

