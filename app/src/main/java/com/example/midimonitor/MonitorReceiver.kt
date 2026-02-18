package com.example.midimonitor

import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver
import java.io.IOException

// --- Receiver Class ---
class MonitorReceiver(
    private val log: (String) -> Unit,
    private val isDebugEnabled: () -> Boolean
) : MidiReceiver() {

    @Volatile
    private var thruInputPort: MidiInputPort? = null
    @Volatile private var filterEnabled = false

    @Volatile private var sustainMode = false
    //@Volatile private var debugEnabled = false

    fun setThruPort(port: MidiInputPort?) {
        thruInputPort = port
        log("THRU port set")
    }

    /*
    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }

     */

    fun setSustainMode(enabled: Boolean) {
        sustainMode = enabled
    }

    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {

        if (count > 0) {
            if (isDebugEnabled()) {
                val msg = parseMidi(data, offset, count)
                log("Received MIDI: $msg")
            }
        }

        val status = data[offset].toInt() and 0xFF
        val type   = status and 0xF0

        val velocity: Int? =
            if (count >= 3) data[offset + 2].toInt() and 0xFF
            else null

        val isNoteOff =
            type == 0x80 ||
                    (type == 0x90 && count >= 3 && data[offset + 2] == 0.toByte())

        if (sustainMode && isNoteOff) {
            if (isDebugEnabled()) {
                log("Filtered NoteOff")
            }
            return
        }

        //if (filterEnabled && isNoteOff(status, velocity)) {
        //    return // ❌ discard NoteOff
        //}

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

    /**/
    internal fun sendAllNotesOff() {
        val thru = thruInputPort ?: return

        for (channel in 0..15) {
            val msg = byteArrayOf(
                (0xB0 or channel).toByte(),
                123.toByte(), // All Notes Off
                0
            )

            try {
                thru.send(msg, 0, msg.size)
            } catch (e: IOException) {
                if (isDebugEnabled()) {
                    log("Failed to send AllNotesOff ch=$channel: ${e.message}")
                }
            }
        }

        if (isDebugEnabled()) {
            log("All Notes Off sent")
        }
    }

     /**/

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
