//4️⃣ MidiProcessor (this is the heart)
//
//This is where all future work happens.

package com.example.midimonitor

import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver

class MidiProcessor(
    private val logger: (String) -> Unit
) {

    private var thruPort: MidiInputPort? = null   // ✅ MUST be MidiInputPort

    fun attachThru(port: MidiInputPort) {
        thruPort = port
        logger("THRU port attached")
    }

    val receiver = object : MidiReceiver() {

        override fun onSend(
            data: ByteArray,
            offset: Int,
            count: Int,
            timestamp: Long
        ) {
            logger("RX MIDI $count bytes")

            thruPort?.send(data, offset, count)   // ✅ send() now resolves
        }
    }
}

//✔ One receiver
//✔ One send path
//✔ Zero loopback ambiguity