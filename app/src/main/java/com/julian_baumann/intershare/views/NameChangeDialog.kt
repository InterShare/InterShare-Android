package com.julian_baumann.intershare.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julian_baumann.intershare.MainActivity
import com.julian_baumann.intershare.UserPreferencesManager
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun NameChangeDialog(userPreferencesManager: UserPreferencesManager, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var saveButtonEnabled by remember { mutableStateOf(false) }
    var initialDeviceNameSet by remember { mutableStateOf(false) }

    // Listen to userNameFlow to update userName state
    LaunchedEffect(key1 = true) {
        userPreferencesManager.deviceNameFlow.collect { name ->
            initialDeviceNameSet = name != null
            userName = name ?: ""
            saveButtonEnabled = userName.length >= 3

            if (userName.length < 3) {
                showDialog = true
            }
        }
    }

    FilledTonalButton(modifier = Modifier
        .padding(0.dp, 0.dp)
        .defaultMinSize(1.dp, 1.dp),
        contentPadding = PaddingValues(7.dp, 7.dp, 15.dp, 7.dp),
        enabled = enabled,
        onClick = {
            showDialog = true
        }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFFD32F2F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotEmpty()) userName.first().toString().uppercase(Locale.getDefault()) else "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
            },
            title = { Text(text = "Name this device") },
            text = {
                Column {
                    Text("Nearby devices will discover this device using this name. Must be at least three characters long.", modifier = Modifier.padding(
                        PaddingValues(bottom = 20.dp)
                    ))
                    TextField(
                        value = userName,
                        singleLine = true,
                        onValueChange = { newName ->
                            userName = newName
                            saveButtonEnabled = userName.length >= 3
                        },
                        label = { Text("Device Name") }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = saveButtonEnabled,
                    onClick = {
                        scope.launch {
                            userPreferencesManager.saveDeviceName(userName)

                            if (MainActivity.currentDevice != null) {
                                MainActivity.currentDevice!!.name = userName
                                MainActivity.nearbyServer?.changeDevice(MainActivity.currentDevice!!)

                                if (!initialDeviceNameSet) {
                                    MainActivity.startAdvertising()
                                }
                            }

                            showDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        )
    }
}
