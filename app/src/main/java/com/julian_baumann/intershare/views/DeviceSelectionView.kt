package com.julian_baumann.intershare.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.julian_baumann.data_rct.Device
import com.julian_baumann.data_rct.SendProgressState
import com.julian_baumann.intershare.MainActivity
import com.julian_baumann.intershare.SendProgress
import com.julian_baumann.intershare.ui.theme.Purple80
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun getReadableDeviceType(device: Device): String {
    when (device.deviceType) {
        1 -> {
            return "Phone"
        }
        2 -> {
            return "Tablet"
        }
        3 -> {
            return "Computer"
        }
    }

    return "Unknown"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceSelectionView(devices: List<Device>, selectedFileUri: String) {
    val displayedDevices = remember { mutableSetOf<String>() }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 600.dp)
            .background(Color.Transparent)
    ) {
        devices.forEach { device ->
            if (!displayedDevices.contains(device.id)) {
                displayedDevices.add(device.id)
                LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            val progress by remember { mutableStateOf( SendProgress()) }

            TextButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            MainActivity.nearbyServer?.sendFile(device, selectedFileUri, progress)
                        } catch (error: Exception) {
                            println(error)
                        }
                    }
                },
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.textButtonColors().copy(contentColor = LocalContentColor.current),
                modifier = Modifier.background(Color.Transparent)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp)
                ) {
                    AnimatedCircularProgressIndicator(
                        progress = progress,
                        progressIndicatorColor = Purple80,
                        showProgress = true,
                        completedColor = Color(0xFF50C878)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(57.dp)
                                .background(Color.LightGray, CircleShape)
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (progress.state == SendProgressState.Requesting || progress.state == SendProgressState.Connecting) {
                                CircularProgressIndicator(
                                    color = Color.Black
                                )
                            } else {
                                Text(
                                    text = device.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 25.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = device.name,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            lineHeight = 1.em,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
