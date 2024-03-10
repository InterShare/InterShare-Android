package com.julian_baumann.intershare

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.julian_baumann.data_rct.*
import com.julian_baumann.intershare.ui.theme.DataRCTTheme
import com.julian_baumann.intershare.views.ReceiveContentView
import com.julian_baumann.intershare.views.StartView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity(), NearbyConnectionDelegate, DiscoveryDelegate {
    companion object {
        var nearbyServer: NearbyServer? = null
        var currentDevice: Device? = null

        fun startAdvertising() {
            CoroutineScope(Dispatchers.Main).launch {
                nearbyServer?.start()
            }
        }
    }

    private var currentConnectionRequest: ConnectionRequest? = null
    private var showConnectionRequest by mutableStateOf(false)
    private var showReceivingSheet by mutableStateOf(false)
    private var receiveProgress: ReceiveProgress? = null
    private val devices = mutableStateListOf<Device>()
    private var userPreferencesManager by mutableStateOf<UserPreferencesManager?>(null)

    private var bluetoothConnectPermissionGranted = false
    private var bluetoothAdvertisePermissionGranted = false
    private var bluetoothScanPermissionGranted = false
    private var accessLocationPermissionGranted = false

    private val bleConnectPermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        bluetoothConnectPermissionGranted = isGranted
        bleAdvertisePermissionActivity.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
    }

    private val accessLocationPermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        accessLocationPermissionGranted = isGranted
        startServer()
    }

    private val bleAdvertisePermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        bluetoothAdvertisePermissionGranted = isGranted
        accessLocationPermissionActivity.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val bleScanPermissionActivity = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        bluetoothScanPermissionGranted = isGranted
        bleConnectPermissionActivity.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun startServer() {
        if (!bluetoothConnectPermissionGranted
            || !bluetoothAdvertisePermissionGranted
            || !bluetoothScanPermissionGranted
            || !accessLocationPermissionGranted) {
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
            deviceType = 0
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
        }
    }

    override fun deviceAdded(value: Device) {
        devices.add(value)
    }

    override fun deviceRemoved(deviceId: String) {
        TODO("Not yet implemented")
    }

    override fun receivedConnectionRequest(request: ConnectionRequest) {
        currentConnectionRequest = request
        showConnectionRequest = true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPreferencesManager = UserPreferencesManager(baseContext)
        bleScanPermissionActivity.launch(Manifest.permission.BLUETOOTH_SCAN)
        val discovery = Discovery(baseContext, this)

        setContent {
            DataRCTTheme {
                if (userPreferencesManager != null) {
                    StartView(userPreferencesManager!!, discovery, devices)
                }

                if (showReceivingSheet && receiveProgress != null && currentConnectionRequest != null) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showReceivingSheet = false
                        }
                    ) {
                        ReceiveContentView(receiveProgress!!, currentConnectionRequest)
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

                                    Thread {
                                        currentConnectionRequest?.accept()
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
