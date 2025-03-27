package com.julian_baumann.intershare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.julian_baumann.intershare_sdk.*

class ReceiveProgress : ReceiveProgressDelegate {
    var state: ReceiveProgressState by mutableStateOf(ReceiveProgressState.Unknown)

    override fun progressChanged(progress: ReceiveProgressState) {
        state = progress
    }
}

class SendProgress : SendProgressDelegate {
    var state: SendProgressState by mutableStateOf(SendProgressState.Unknown)
    var medium: ConnectionMedium? by mutableStateOf(null)

    override fun progressChanged(progress: SendProgressState) {
        state = progress
        
        if (state is SendProgressState.ConnectionMediumUpdate) {
            medium = (state as SendProgressState.ConnectionMediumUpdate).medium
        }
    }
}

class ShareProgress : ShareProgressDelegate {
    var state: ShareProgressState by mutableStateOf(ShareProgressState.Unknown)

    override fun progressChanged(progress: ShareProgressState) {
        state = progress
    }
}