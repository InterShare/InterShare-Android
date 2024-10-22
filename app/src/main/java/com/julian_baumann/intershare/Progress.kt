package com.julian_baumann.intershare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.julian_baumann.intershare_sdk.ReceiveProgressDelegate
import com.julian_baumann.intershare_sdk.ReceiveProgressState
import com.julian_baumann.intershare_sdk.SendProgressDelegate
import com.julian_baumann.intershare_sdk.SendProgressState

class ReceiveProgress : ReceiveProgressDelegate {
    var state: ReceiveProgressState by mutableStateOf(ReceiveProgressState.Unknown)

    override fun progressChanged(progress: ReceiveProgressState) {
        state = progress
    }
}

class SendProgress : SendProgressDelegate {
    var state: SendProgressState by mutableStateOf(SendProgressState.Unknown)

    override fun progressChanged(progress: SendProgressState) {
        state = progress
    }
}
