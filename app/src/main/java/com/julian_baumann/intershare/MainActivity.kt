package com.julian_baumann.intershare

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.julian_baumann.intershare.ui.theme.InterShareTheme
import com.julian_baumann.intershare.views.ReceiveContentView
import com.julian_baumann.intershare.views.SendView
import com.julian_baumann.intershare.views.StartView
import com.julian_baumann.intershare_sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity(), NearbyConnectionDelegate, DiscoveryDelegate {
    companion object {
        var nearbyServer: NearbyServer? = null
        var currentDevice: Device? = null
        var serverStarted = false

        private var currentConnectionRequest: ConnectionRequest? = null
        private var showConnectionRequest by mutableStateOf(false)
        private var showReceivingSheet by mutableStateOf(false)
        private var receiveProgress: ReceiveProgress? = null
        private var devices = mutableStateListOf<Device>()
        private var userPreferencesManager by mutableStateOf<UserPreferencesManager?>(null)
        private var selectedFileUris = mutableStateOf<List<String>>(listOf())

        private var bluetoothConnectPermissionGranted = false
        private var bluetoothAdvertisePermissionGranted = false
        private var bluetoothScanPermissionGranted = false
        private var accessNearbyDevicesPermissionGranted = false
        private var bluetoothManager: BluetoothManager? = null
        private var appWasPaused = false
        private var scanning = false

        fun startAdvertising() {
            CoroutineScope(Dispatchers.Main).launch {
                val isBluetoothEnabled = bluetoothManager!!.adapter.isEnabled

                if (isBluetoothEnabled) {
                    Log.i("InterShare", "Starting advertisement")
                    nearbyServer?.start()
                    serverStarted = true
                }
            }
        }
    }
    private lateinit var navController: NavHostController

    private val bleConnectPermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        bluetoothConnectPermissionGranted = isGranted
        bleAdvertisePermissionActivity.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    private val accessNearbyDevicesPermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        accessNearbyDevicesPermissionGranted = isGranted
        startServer()
    }

    private val bleAdvertisePermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        bluetoothAdvertisePermissionGranted = isGranted
        accessNearbyDevicesPermissionActivity.launch(Manifest.permission.BLUETOOTH_SCAN)
    }

    private val bleScanPermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        bluetoothScanPermissionGranted = isGranted
        bleConnectPermissionActivity.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun startServer() {
        if (!bluetoothConnectPermissionGranted
            || !bluetoothAdvertisePermissionGranted
            || !bluetoothScanPermissionGranted
            || !accessNearbyDevicesPermissionGranted) {
            return
        }

        if (serverStarted) {
            return
        }

        var deviceId = runBlocking { userPreferencesManager!!.deviceIdFlow.first() }

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            runBlocking { userPreferencesManager!!.saveDeviceId(deviceId) }
        }

        val deviceName = runBlocking { userPreferencesManager!!.deviceNameFlow.first() }

        currentDevice = Device(
            id = deviceId,
            name = deviceName ?: "",
            deviceType = 1
        )

        nearbyServer = NearbyServer(baseContext, currentDevice!!, this)

        if (deviceName != null) {
            startAdvertising()
        }
    }

    override fun onStop() {
        super.onStop()

        CoroutineScope(Dispatchers.Main).launch {
            nearbyServer?.stop()
            appWasPaused = true
        }
    }

    override fun onResume() {
        super.onResume()

        CoroutineScope(Dispatchers.Main).launch {
            if (serverStarted && appWasPaused) {
                startAdvertising()
                appWasPaused = false
            }
        }
    }

    override fun deviceAdded(value: Device) {
        val indexOfExistingDevice = devices.indexOfFirst { it.id == value.id }

        if (indexOfExistingDevice >= 0) {
            devices[indexOfExistingDevice] = value
        } else {
            devices.add(value)
        }
    }

    override fun deviceRemoved(deviceId: String) {
        TODO("Not yet implemented")
    }

    override fun receivedConnectionRequest(request: ConnectionRequest) {
        currentConnectionRequest = request
        showConnectionRequest = true
    }

    fun registerFilesWithSystem(files: List<String>) {
        val filePaths = mutableListOf<String>()

        for (file in files) {
            filePaths.add(File(file).absolutePath)
        }

        MediaScannerConnection.scanFile(
            baseContext,
            filePaths.toTypedArray(),
            null
        ) { path, uri ->
            Log.i("MediaScanner", "Scanned $path:")
            Log.i("MediaScanner", "-> uri=$uri")
        }
    }

    private fun openUrlInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        userPreferencesManager = UserPreferencesManager(baseContext)
        bleScanPermissionActivity.launch(Manifest.permission.BLUETOOTH_SCAN)

        setContent {
            navController = rememberNavController()
            var discovery = remember { Discovery(baseContext, this) }
            val isExternalShare = remember { mutableStateOf(false) }
            var startDestination = remember { "start" }
            val bluetoothAdapter = bluetoothManager!!.adapter
            var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter.isEnabled) }

            var clipboard by remember { mutableStateOf<String?>(null) }
            
            fun shareFiles(urls: List<String>, setRoot: Boolean = false) {
                if (isBluetoothEnabled && serverStarted) {
                    discovery.stopScanning()
                    discovery = Discovery(baseContext, this)
                }
                
                selectedFileUris.value = urls
                clipboard = null
                devices.clear()
                devices.addAll(discovery.getDevices())

                if (isBluetoothEnabled && serverStarted) {
                    discovery.startScanning()
                }
                
                if (setRoot) {
                    startDestination = "send"
                } else {
                    navController.navigate("send")
                }
            }
            
            fun shareText(content: String, setRoot: Boolean = false) {
                if (isBluetoothEnabled && serverStarted) {
                    discovery.stopScanning()
                    discovery = Discovery(baseContext, this)
                }
                
                selectedFileUris.value = listOf()
                clipboard = content
                devices.clear()
                devices.addAll(discovery.getDevices())

                if (isBluetoothEnabled && serverStarted) {
                    discovery.startScanning()
                }

                if (setRoot) {
                    startDestination = "send"
                } else {
                    navController.navigate("send")
                }
            }

            // Update the state dynamically whenever BLE is enabled or disabled
            LaunchedEffect(Unit) {
                val bluetoothStateReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            isBluetoothEnabled = (state == BluetoothAdapter.STATE_ON)

                            if (isBluetoothEnabled) {
                                CoroutineScope(Dispatchers.Default).launch {
                                    if (navController.currentDestination?.route == "send") {
                                        if (serverStarted) {
                                            nearbyServer?.stop()
                                        }

                                        discovery.startScanning()
                                    } else {
                                        startAdvertising()
                                    }
                                }
                            } else {
                                CoroutineScope(Dispatchers.Default).launch {
                                    nearbyServer?.stop()
                                    serverStarted = false
                                }
                            }
                        }
                    }
                }

                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                baseContext.registerReceiver(bluetoothStateReceiver, filter)
            }

            InterShareTheme {
                when(intent?.action) {
                    Intent.ACTION_SEND -> {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java) as? Uri
                        } else {
                            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                        }

                        uri?.let {
                            isExternalShare.value = true
                            shareFiles(listOf(getPathFromUri(baseContext, it)!!), true)
                        }
                    }

                    Intent.ACTION_SEND_MULTIPLE -> {
                        val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Parcelable::class.java)?.filterIsInstance<Uri>()
                        } else {
                            intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>()
                        }

                        uris?.let {
                            val listOrUris = mutableListOf<String>()

                            it.forEach { uri ->
                                val path = getPathFromUri(baseContext, uri)

                                if (path != null) {
                                    listOrUris.add(path)
                                }
                            }

                            isExternalShare.value = true
                            shareFiles(listOrUris.toList(), true)
                        }
                    }
                }

                if (userPreferencesManager != null) {
                    NavHost(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        startDestination = startDestination
                    ) {

                        composable(route = "start") {
                            scanning = false

                            StartView(userPreferencesManager!!, discovery, navController, ::shareFiles, ::shareText, isBluetoothEnabled, serverStarted)
                        }

                        composable(
                            route = "send",
                        ) {
                            scanning = true

                            SendView(devices, selectedFileUris.value!!, clipboard = clipboard, isExternalShare.value, navController, isBluetoothEnabled, discovery, {
                                finish()
                            })
                        }

                        composable(route = "receive") {
                            scanning = false
                            if (receiveProgress != null && currentConnectionRequest != null) {
                                ReceiveContentView(receiveProgress!!, navController, currentConnectionRequest)
                            }
                        }
                    }
                }

                if (showConnectionRequest && currentConnectionRequest != null) {
                    val fileTransferIntent = remember { currentConnectionRequest?.getFileTransferIntent() }
                    val clipboardTransferIntent = remember { currentConnectionRequest?.getClipboardIntent() }
                    val isClipboard = remember { currentConnectionRequest?.getIntentType() == ConnectionIntentType.CLIPBOARD }

                    AlertDialog(
                        title = {
                            if (isClipboard) {
                                Text(text = "${currentConnectionRequest?.getSender()?.name} wants to send you a text")
                            }
                            else {
                                if (fileTransferIntent?.fileCount == 1UL) {
                                    Text(text = "${currentConnectionRequest?.getSender()?.name} wants to send you a file")
                                } else {
                                    Text(text = "${currentConnectionRequest?.getSender()?.name} wants to send you ${fileTransferIntent?.fileCount} files")
                                }
                            }
                        },
                        text = {
                            Column {
                                if (isClipboard) {
                                    Text(text = "${clipboardTransferIntent?.clipboardContent}", maxLines = 4, overflow = TextOverflow.Ellipsis)
                                } else {
                                    if (fileTransferIntent?.fileName != null) {
                                        Text(text = fileTransferIntent.fileName!!)
                                    }

                                    Text(text = "Size: ${toHumanReadableSize(fileTransferIntent?.fileSize)}")   
                                }
                            }
                        },
                        onDismissRequest = {
                            showConnectionRequest = false

                            CoroutineScope(Dispatchers.Main).launch {
                                currentConnectionRequest?.decline()
                            }
                        },
                        confirmButton = {
                            if (isClipboard) {
                                Row {
                                    TextButton(
                                        onClick = {
                                            copyToClipboard(baseContext, clipboardTransferIntent?.clipboardContent ?: "")
                                            showConnectionRequest = false
                                        }
                                    ) {
                                        Text("Copy")
                                    }

                                    if (currentConnectionRequest?.isLink() == true) {
                                        TextButton(
                                            onClick = {
                                                openUrlInBrowser(baseContext, clipboardTransferIntent?.clipboardContent ?: "")
                                                showConnectionRequest = false
                                            }
                                        ) {
                                            Text("Open Link")
                                        }
                                    }
                                } 
                            } else {
                                TextButton(
                                    onClick = {
                                        showConnectionRequest = false
                                        receiveProgress = ReceiveProgress()
                                        currentConnectionRequest?.setProgressDelegate(receiveProgress!!)
                                        showReceivingSheet = true

                                        navController.navigate("receive")

                                        Thread {
                                            val files = currentConnectionRequest?.accept()

                                            if (files?.isNotEmpty() == true) {
                                                registerFilesWithSystem(files)
                                            }
                                        }.start()
                                    }
                                ) {
                                    Text("Accept")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showConnectionRequest = false
                                    currentConnectionRequest?.decline()
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
