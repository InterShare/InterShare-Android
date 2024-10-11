package com.julian_baumann.intershare.views

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("MissingPermission")
@Composable
fun BluetoothStateView(context: Context) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f),
        ),
        modifier = Modifier
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier.size(40.dp),
                    tint = Color.Red,
                    imageVector = Icons.Default.BluetoothDisabled,
                    contentDescription = "Bluetooth disabled"
                )

                Text(
                    fontSize = 15.sp,
                    text = "Enable Bluetooth to use InterShare"
                )
            }

            Button(
                onClick = {
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    context.startActivity(enableIntent)
                }) {
                Text("Enable Bluetooth")
            }
        }
    }
}
