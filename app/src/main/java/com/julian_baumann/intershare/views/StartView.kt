package com.julian_baumann.intershare.views

import android.app.DownloadManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julian_baumann.data_rct.Device
import com.julian_baumann.data_rct.Discovery
import com.julian_baumann.intershare.MainActivity
import com.julian_baumann.intershare.UserPreferencesManager
import com.julian_baumann.intershare.getPathFromUri
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat.getSystemService

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartView(userPreferencesManager: UserPreferencesManager, discovery: Discovery, devices: List<Device>, sharedFilePath: String?) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showDeviceSelectionSheet by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showMenu by remember { mutableStateOf(false) }

    sharedFilePath?.let { path ->
        selectedFileUri = path

        scope.launch { MainActivity.nearbyServer?.stop() }.invokeOnCompletion {
            discovery.startScanning()
            showDeviceSelectionSheet = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        "InterShare",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        modifier = Modifier.widthIn(min = 250.dp),
                        onDismissRequest = { showMenu = false }
                    ) {
                        NameChangeDialog(userPreferencesManager)
//                        HorizontalDivider()
                        Text(
                            text = "App Version: ${getAppVersion(context)}",
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Transparent)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
//            Column {
//                ListItem(
//                    headlineContent = { Text("Device Name") },
//                    supportingContent = { Text("Julians Nothing") },
//                    leadingContent = {
//                        Icon(
//                            Icons.Filled.Smartphone,
//                            contentDescription = "Phone",
//                        )
//                    },
//                    modifier = Modifier.clickable {
//
//                    }
//                )
//            }

            Column {
//                PulseAnimation()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Share",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .alpha(0.8F)
                )

                val pickFileLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { fileUri ->
                    if (fileUri != null) {
                        selectedFileUri = getPathFromUri(context, fileUri)
                        scope.launch { MainActivity.nearbyServer?.stop() }.invokeOnCompletion {
                            discovery.startScanning()
                            showDeviceSelectionSheet = true
                        }
                    }
                }
                val pickVisualMediaLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { imageUri ->
                    if (imageUri != null) {
                        selectedFileUri = getPathFromUri(context, imageUri)
                        scope.launch { MainActivity.nearbyServer?.stop() }.invokeOnCompletion {
                            discovery.startScanning()
                            showDeviceSelectionSheet = true
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Image or Video")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { pickFileLauncher.launch("*/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("File")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text("Show received files")
                }
            }

            if (showDeviceSelectionSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showDeviceSelectionSheet = false
                    },
                    sheetState = sheetState
                ) {
                    DeviceSelectionView(
                        devices,
                        selectedFileUri!!
                    )
                }
            }
        }
    }
}
