package com.julian_baumann.intershare.views

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.julian_baumann.intershare.UserPreferencesManager
import com.julian_baumann.intershare.getPathFromUri
import com.julian_baumann.intershare.saveLogsToTxtFile
import com.julian_baumann.intershare_sdk.Device
import com.julian_baumann.intershare_sdk.Discovery

fun getAppVersion(context: Context): String {
    return try {
        val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

fun getClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return if (clipboard.hasPrimaryClip()) {
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).text?.toString()
        } else {
            null
        }
    } else {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartView(
    userPreferencesManager: UserPreferencesManager,
    discovery: Discovery,
    navController: NavHostController,
    shareFiles: (List<String>) -> Unit,
    shareText: (String) -> Unit,
    bluetoothEnabled: Boolean,
    serverStarted: Boolean
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(Color.Transparent)
            .zIndex(2f),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
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
                        Icon(imageVector = Icons.Default.QuestionMark, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        modifier = Modifier.widthIn(min = 250.dp),
                        onDismissRequest = { showMenu = false }
                    ) {
                        Text(
                            text = "App Version: ${getAppVersion(context)}",
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Transparent)
                        )

                        HorizontalDivider()

                        DropdownMenuItem(onClick = {
                            saveLogsToTxtFile(context)
                        }, text = {
                            Text("Share Logs")
                        })
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                DynamicBackgroundGradient(
                    300.dp,
                    if (bluetoothEnabled) DynamicBackgroundGradientColors.Start else DynamicBackgroundGradientColors.Error
                )
            }

            Column(modifier = Modifier.padding(15.dp, innerPadding.calculateTopPadding() - 10.dp, 15.dp, 0.dp)) {
                NameChangeDialog(userPreferencesManager, bluetoothEnabled, serverStarted)
            }

            if (!bluetoothEnabled) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BluetoothStateView(context)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp, 20.dp, 20.dp, 20.dp + innerPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bluetoothEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            Text(
                                color = Color(0xFF23BF04),
                                text = "Ready to receive",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp).alpha(0.6f)
                            )
                        }
                    }
                }

                Text(
                    text = "Share",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .alpha(0.8F)
                )

                fun processUris(uris: List<Uri>, context: Context, discovery: Discovery, navController: NavHostController) {
                    val filePaths = uris.mapNotNull { getPathFromUri(context, it) }

                    if (filePaths.isNotEmpty()) {
                        shareFiles(filePaths)
                    }
                }

                val pickFileLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetMultipleContents()
                ) { fileUris ->
                    processUris(fileUris, context, discovery, navController)
                }
                val pickVisualMediaLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickMultipleVisualMedia()
                ) { imageUris ->
                    processUris(imageUris, context, discovery, navController)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        enabled = bluetoothEnabled,
                        shape = RoundedCornerShape(35),
                        onClick = { pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Photo Library"
                            )
                            
                            Text("Photos")
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        enabled = bluetoothEnabled,
                        shape = RoundedCornerShape(35),
                        onClick = { pickFileLauncher.launch("*/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = "Files"
                            )

                            Text("Files")
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Button(
                        enabled = bluetoothEnabled,
                        shape = RoundedCornerShape(35),
                        onClick = {
                            val copiedString = getClipboardText(context)

                            if (copiedString != null) {
                                shareText(copiedString)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = "Files"
                            )

                            Text("Clipboard")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text("Received Files")
                }
            }
        }
    }
}
