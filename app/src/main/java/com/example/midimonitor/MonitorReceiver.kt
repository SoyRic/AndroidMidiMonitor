package com.example.midimonitor

import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver
import java.io.IOException

// --- Receiver Class ---

class MonitorReceiver(
    private val log: (String) -> Unit
) : MidiReceiver() {

    //@Volatile
    //private var thruPort: MidiInputPort? = null
    @Volatile
    private var thruInputPort: MidiInputPort? = null

    fun setThruPort(port: MidiInputPort?) {
        thruInputPort = port
        log("THRU port set")
    }

    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
        if (count > 0) {
            val msg = parseMidi(data, offset, count)
            log("Received MIDI: $msg")
        }

        try {
            if (thruInputPort != null) {
                thruInputPort?.send(data, offset, count)
                log("Forwarded MIDI to THRU")
            } else {
                log("THRU port is null, not forwarding")
            }
        } catch (e: IOException) {
            log("Error sending to THRU port: ${e.message}")
        }
    }
/*
    override fun onSend(
        data: ByteArray,
        offset: Int,
        count: Int,
        timestamp: Long
    ) {
        // Use the top-level parser function
        if (count > 0) log(parseMidi(data, offset, count))

        // Forward MIDI to synth if connected, with error handling
        try {
            thruInputPort?.send(data, offset, count)
        } catch (e: Exception) {
            log("Error sending to THRU port: ${e.message}")
            // You might want to nullify the port here
            // thruPort = null
        }
    }

 */
}

// --- Top-Level Utility Function ---
fun parseMidi(data: ByteArray, offset: Int = 0, count: Int = data.size): String {
    if (count <= 0) return ""

    val sb = StringBuilder()
    var i = offset
    val end = offset + count

    while (i < end) {
        val b = data[i].toInt() and 0xFF
        sb.append("0x${b.toString(16).padStart(2, '0')} ")

        // Optional: decode simple Note On / Note Off
        when {
            b in 0x80..0x8F && i + 2 < end -> {
                sb.append("(NoteOff ch=${b and 0x0F}, note=${data[i+1]}, vel=${data[i+2]}) ")
                i += 2
            }
            b in 0x90..0x9F && i + 2 < end -> {
                sb.append("(NoteOn ch=${b and 0x0F}, note=${data[i+1]}, vel=${data[i+2]}) ")
                i += 2
            }
        }

        i++
    }

    return sb.toString().trim()
}
