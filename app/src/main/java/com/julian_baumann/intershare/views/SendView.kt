package com.julian_baumann.intershare.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.julian_baumann.intershare.MainActivity
import com.julian_baumann.intershare.ShareProgress
import com.julian_baumann.intershare_sdk.Device
import com.julian_baumann.intershare_sdk.Discovery
import com.julian_baumann.intershare_sdk.ShareProgressState
import com.julian_baumann.intershare_sdk.ShareStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendView(devices: List<Device>, selectedFileUris: List<String>, clipboard: String?, shouldTerminate: Boolean, navController: NavHostController, bluetoothEnabled: Boolean, discovery: Discovery, finishActivity: () -> Unit) {
    BackHandler {
        discovery.stopScanning()

        if (shouldTerminate) {
            finishActivity()
        } else {
            navController.popBackStack()
        }
    }

    val context = LocalContext.current
    val shareProgress by remember { mutableStateOf(ShareProgress()) }
    var shareStore by remember { mutableStateOf<ShareStore?>(null) }
    var hasExecuted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasExecuted) {
            hasExecuted = true

            withContext(Dispatchers.IO) {
                if (clipboard != null) {
                    shareStore = MainActivity.nearbyServer!!.shareText(clipboard, false)
                    shareProgress.state = ShareProgressState.Finished;
                } else {
                    shareProgress.state = ShareProgressState.Unknown;
                    shareStore = MainActivity.nearbyServer!!.shareFiles(selectedFileUris, false, shareProgress)
                }
            }
        }
    }

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
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                    }
                },
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
            .padding(10.dp, innerPadding.calculateTopPadding(), 10.dp, innerPadding.calculateBottomPadding())
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
                shareStore
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
                    if (shareProgress.state is ShareProgressState.Compressing) {
                        val compressionProgress = (shareProgress.state as ShareProgressState.Compressing).progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    modifier = Modifier.alpha(0.8f),
                                    fontWeight = FontWeight.Bold,
                                    text = "Preparing files..."
                                )
                                Text(
                                    modifier = Modifier.alpha(0.5f),
                                    text = String.format("Compressing: %.1f%%", compressionProgress * 100)
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    modifier = Modifier.alpha(0.8f),
                                    fontWeight = FontWeight.Bold,
                                    text = "Looking for nearby devices"
                                )
                                Text(
                                    modifier = Modifier.alpha(0.5f),
                                    text = "Make sure the receiver has the InterShare app open on their device."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
