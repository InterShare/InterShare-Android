package com.julian_baumann.intershare.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.julian_baumann.intershare_sdk.Device
import com.julian_baumann.intershare_sdk.SendProgressState
import com.julian_baumann.intershare.SendProgress
import com.julian_baumann.intershare.ui.theme.Purple80
import com.julian_baumann.intershare_sdk.ConnectionMedium
import com.julian_baumann.intershare_sdk.ShareStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun getCurrentStateText(progress: SendProgress): String {
    return when (progress.state) {
        SendProgressState.Cancelled -> "Cancelled"
        SendProgressState.Connecting -> "Connecting"
        SendProgressState.Declined -> "Declined"
        SendProgressState.Finished -> "Finished"
        SendProgressState.Requesting -> "Requesting"
        is SendProgressState.Transferring -> "Sending"
        else -> ""
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceSelectionView(devices: List<Device>, shareStore: ShareStore?) {
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

            val progress by remember { mutableStateOf(SendProgress()) }

            TextButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            shareStore?.sendTo(device, progress)
                        } catch (error: Exception) {
                            println(error)
                        }
                    }
                },
                enabled = shareStore != null,
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.textButtonColors().copy(contentColor = LocalContentColor.current),
                modifier = Modifier.background(Color.Transparent)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp)
                ) {
                    Box() {
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
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .width(25.dp)
                                            .height(25.dp)
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

                        if (progress.medium != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .background(Color.Blue, shape = CircleShape)
                                    .padding(5.dp)
                            ) {
                                if (progress.medium == ConnectionMedium.WI_FI) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "WiFi",
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White
                                    )
                                } else if (progress.medium == ConnectionMedium.BLE) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = "Bluetooth",
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White
                                    )
                                }
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

                        Text(
                            text = getCurrentStateText(progress),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            lineHeight = 1.em,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DeviceSelectionViewPreview() {
    DeviceSelectionView(devices = listOf(Device(id =  "test", name = "Test Device", deviceType = 0)), shareStore = null)
}