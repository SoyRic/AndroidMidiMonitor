package com.example.midimonitor

import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver
import java.io.IOException

// --- Receiver Class ---
class MonitorReceiver(
    private val log: (String) -> Unit
) : MidiReceiver() {

    @Volatile
    var debug: Boolean = false   // 👈 NEW
    @Volatile
    private var thruInputPort: MidiInputPort? = null
    //@Volatile
    //var filter: MidiFilterType = MidiFilterType.NONE   // ✅ NEW
    @Volatile
    var filterNoteOff: Boolean = false

    @Volatile private var filterEnabled = false
    @Volatile private var debugEnabled = false



    fun setThruPort(port: MidiInputPort?) {
        thruInputPort = port
        log("THRU port set")
    }

    fun setFilterEnabled(enabled: Boolean) {
        filterEnabled = enabled
    }

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }

    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {

        if (count > 0) {
            if (debug) {
                val msg = parseMidi(data, offset, count)
                log("Received MIDI: $msg")
            }
        }

        val status = data[offset].toInt() and 0xFF
        val type   = status and 0xF0

        val velocity: Int? =
            if (count >= 3) data[offset + 2].toInt() and 0xFF
            else null

        //if (filterEnabled && isNoteOff(data, offset)) {
        //    return // ❌ discard NoteOff
        //}


        if (filterNoteOff && isNoteOff(status, velocity)) {
            return   // 🚫 DROP NOTE OFF
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

    private fun isNoteOff(status: Int, velocity: Int?): Boolean {
        val type = status and 0xF0
        return type == 0x80 || (type == 0x90 && velocity == 0)
    }
/*
    fun shouldDiscard(
        status: Int,
        velocity: Int?
    ): Boolean {

        val type = status and 0xF0

        return when (filter) {

            MidiFilterType.NONE ->
                false   // discard nothing

            MidiFilterType.NOTE_ON ->
                type == 0x90 && velocity != null && velocity > 0

            MidiFilterType.NOTE_OFF ->
                type == 0x80 ||
                        (type == 0x90 && velocity != null && velocity == 0)

            MidiFilterType.CONTROL_CHANGE ->
                type == 0xB0

            MidiFilterType.PROGRAM_CHANGE ->
                type == 0xC0
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
