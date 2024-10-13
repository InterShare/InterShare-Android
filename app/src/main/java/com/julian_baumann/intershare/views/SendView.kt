package com.julian_baumann.intershare.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.julian_baumann.data_rct.Device
import com.julian_baumann.data_rct.Discovery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendView(devices: List<Device>, selectedFileUri: String, shouldTerminate: Boolean, navController: NavHostController, bluetoothEnabled: Boolean, discovery: Discovery, finishActivity: () -> Unit) {
    BackHandler {
        discovery.stopScanning()

        if (shouldTerminate) {
            finishActivity()
        } else {
            navController.popBackStack()
        }
    }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .background(Color.Transparent)
            .zIndex(2f),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.secondary
                ),
                title = {
                    Text(
                        "Send to",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
    ) { innerPadding ->
        Column {
            DynamicBackgroundGradient(400.dp, DynamicBackgroundGradientColors.Send)
        }

        Column(modifier = Modifier
            .padding(20.dp, innerPadding.calculateTopPadding(), 20.dp, innerPadding.calculateBottomPadding())
        ) {
            if (!bluetoothEnabled) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BluetoothStateView(context)
                }
            }

            DeviceSelectionView(
                devices,
                selectedFileUri
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp, 10.dp, 10.dp, 40.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (bluetoothEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.padding(20.dp).fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(10.dp)
                                .size(30.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Text(
                            modifier = Modifier.alpha(0.5f),
                            fontWeight = FontWeight.Bold,
                            text = "Scanning for nearby devices"
                        )
                    }
                }
            }
        }
    }
}
