package com.julian_baumann.intershare.views

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

fun getPathFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme.equals("file")) {
        return uri.path
    } else if (uri.scheme.equals("content")) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    val fileName = it.getString(index)
                    val directory = context.cacheDir
                    val file = File(directory, fileName)
                    if (!file.exists()) {
                        context.contentResolver.openInputStream(uri).use { input ->
                            FileOutputStream(file).use { output ->
                                input?.copyTo(output)
                            }
                        }
                    }
                    return file.absolutePath
                }
            }
        }
    }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartView(userPreferencesManager: UserPreferencesManager, discovery: Discovery, devices: List<Device>) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showDeviceSelectionSheet by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "InterShare",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(PaddingValues(top = 0.dp, start = 18.dp))) {
                NameChangeDialog(userPreferencesManager = userPreferencesManager)
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
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
                ) { imageUri ->
                    if (imageUri != null) {
                        println(imageUri)
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
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Image or Video")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { pickFileLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("File")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) },
                    modifier = Modifier.fillMaxWidth().height(60.dp)
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
                        MainActivity.nearbyServer!!,
                        selectedFileUri!!
                    )
                }
            }
        }
    }
}
