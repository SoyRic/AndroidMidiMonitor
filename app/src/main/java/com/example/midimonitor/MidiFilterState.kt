package com.example.midimonitor

// In app/src/main/java/com/example/midimonitor/MidiFilterState.kt

data class MidiFilterState(
    val noteOn: Boolean = true,
    val noteOff: Boolean = true,
    val controlChange: Boolean = true,
    val systemExclusive: Boolean = true
    // Add other filter properties here as needed
)
    