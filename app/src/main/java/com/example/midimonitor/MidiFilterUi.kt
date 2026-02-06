package com.example.midimonitor

// In your MainActivity.ktimport androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.midimonitor.MidiFilterState // Make sure to import your new data class

@Composable
fun MidiFilterUi(
    filterState: MidiFilterState,
    onFilterChanged: (MidiFilterState) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Filter MIDI Events", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // Create a Checkbox for each filter type
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
        FilterCheckbox(
            label = "Control Change",
            checked = filterState.controlChange,
            onCheckedChange = { onFilterChanged(filterState.copy(controlChange = it)) }
        )
        FilterCheckbox(
            label = "System Exclusive (SysEx)",
            checked = filterState.systemExclusive,
            onCheckedChange = { onFilterChanged(filterState.copy(systemExclusive = it)) }
        )
        // Add other checkboxes as needed...
    }
}

@Composable
private fun FilterCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}