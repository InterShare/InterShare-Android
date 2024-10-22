package com.julian_baumann.intershare

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.julian_baumann.intershare.ui.theme.DataRCTTheme
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
        private var selectedFileUri = mutableStateOf<String?>(null)
        private var sharedFilePath: String? = null

        private var bluetoothConnectPermissionGranted = false
        private var bluetoothAdvertisePermissionGranted = false
        private var bluetoothScanPermissionGranted = false
        private var accessNearbyDevicesPermissionGranted = false
        private var bluetoothManager: BluetoothManager? = null
        private var appWasPaused = false

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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        userPreferencesManager = UserPreferencesManager(baseContext)
        bleScanPermissionActivity.launch(Manifest.permission.BLUETOOTH_SCAN)

        setContent {
            navController = rememberNavController()
            val discovery = remember { Discovery(baseContext, this) }
            val isExternalShare = remember { mutableStateOf(false) }
            var startDestination = remember { "start" }
            val bluetoothAdapter = bluetoothManager!!.adapter
            var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter.isEnabled) }

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

            DataRCTTheme {
                when {
                    intent?.action == Intent.ACTION_SEND -> {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java) as? Uri
                        } else {
                            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                        }

                        uri?.let {
                            selectedFileUri.value = getPathFromUri(baseContext, it)
                            isExternalShare.value = true
                            devices.clear()
                            devices.addAll(discovery.getDevices())
                            startDestination = "send"
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
                            if (isBluetoothEnabled && serverStarted) {
                                discovery.stopScanning()
                            }

                            StartView(userPreferencesManager!!, discovery, devices, sharedFilePath, navController, selectedFileUri, isBluetoothEnabled, serverStarted)
                        }

                        composable(
                            route = "send",
                        ) {
                            if (isBluetoothEnabled && serverStarted) {
                                discovery.startScanning()
                            }

                            SendView(devices, selectedFileUri.value!!, isExternalShare.value, navController, isBluetoothEnabled, discovery, {
                                finish()
                            })
                        }

                        composable(route = "receive") {
                            if (receiveProgress != null && currentConnectionRequest != null) {
                                ReceiveContentView(receiveProgress!!, navController, currentConnectionRequest)
                            }
                        }
                    }
                }

                if (showConnectionRequest && currentConnectionRequest != null) {
                    AlertDialog(
                        title = {
                            Text(text = "${currentConnectionRequest?.getSender()?.name} wants to send you a file")
                        },
                        text = {
                            Text(text = "${currentConnectionRequest?.getFileTransferIntent()?.fileName} (${toHumanReadableSize(currentConnectionRequest?.getFileTransferIntent()?.fileSize)})")
                        },
                        onDismissRequest = {
                            showConnectionRequest = false

                            CoroutineScope(Dispatchers.Main).launch {
                                currentConnectionRequest?.decline()
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showConnectionRequest = false
                                    receiveProgress = ReceiveProgress()
                                    currentConnectionRequest?.setProgressDelegate(receiveProgress!!)
                                    showReceivingSheet = true

                                    navController.navigate("receive")

                                    Thread {
                                        currentConnectionRequest?.accept()
                                        val fileName = currentConnectionRequest?.getFileTransferIntent()?.fileName

                                        val file = File(
                                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                            fileName
                                        )

                                        MediaScannerConnection.scanFile(
                                            baseContext,
                                            arrayOf<String>(file.absolutePath),
                                            null
                                        ) { path, uri ->
                                            Log.i("MediaScanner", "Scanned $path:")
                                            Log.i("MediaScanner", "-> uri=$uri")
                                        }

                                    }.start()
                                }
                            ) {
                                Text("Accept")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showConnectionRequest = false
                                    currentConnectionRequest?.decline()
                                }
                            ) {
                                Text("Decline")
                            }
                        }
                    )
                }
            }
        }
    }
}
