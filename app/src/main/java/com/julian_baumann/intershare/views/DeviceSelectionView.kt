package com.julian_baumann.intershare.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.julian_baumann.data_rct.Device
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.julian_baumann.data_rct.NearbyServer
import com.julian_baumann.data_rct.ReceiveProgressDelegate
import com.julian_baumann.data_rct.ReceiveProgressState
import com.julian_baumann.intershare.MainActivity
import com.julian_baumann.intershare.SendProgress
import com.julian_baumann.intershare.ui.theme.Purple40
import com.julian_baumann.intershare.ui.theme.Purple80
import com.julian_baumann.intershare.ui.theme.PurpleGrey40
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

@Composable
fun DeviceSelectionView(devices: List<Device>, nearbyServer: NearbyServer, selectedFileUri: String) {
    LazyRow(
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp)
            .background(Color.Transparent)
    ) {
        items(devices) { device ->
            val progress  by remember { mutableStateOf( SendProgress()) }

            TextButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        nearbyServer.sendFile(device, selectedFileUri, progress)
                    }
                },
                modifier = Modifier.background(Color.Transparent)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedCircularProgressIndicator(
                        progress = progress,
                        progressIndicatorColor = Purple80,
                        showProgress = true,
                        completedColor = Color(0xFF50C878)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color.LightGray, CircleShape)
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = device.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp,
                                color = Color.Black
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = device.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 1.em
                        )
                        Text(
                            text = getReadableDeviceType(device),
                            fontSize = 13.sp,
                            modifier = Modifier.alpha(0.6F),
                            lineHeight = 1.em
                        )
                    }
                }
            }
        }
    }
}
