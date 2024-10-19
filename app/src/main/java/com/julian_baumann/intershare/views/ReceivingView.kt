package com.julian_baumann.intershare.views

import android.app.DownloadManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.julian_baumann.data_rct.ConnectionRequest
import com.julian_baumann.data_rct.ReceiveProgressState
import com.julian_baumann.intershare.ReceiveProgress
import com.julian_baumann.intershare.toHumanReadableSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveContentView(progress: ReceiveProgress, navController: NavController, connectionRequest: ConnectionRequest?) {
    val context = LocalContext.current

    BackHandler(true) {
        navController.popBackStack()
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
                        "Receiving",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
    ) { innerPadding ->
        Column {
            when (val state = progress.state) {
                is ReceiveProgressState.Receiving -> {
                    DynamicBackgroundGradient(
                        (LocalConfiguration.current.screenHeightDp * state.progress.toFloat()).dp,
                        DynamicBackgroundGradientColors.Receive
                    )
                }
                is ReceiveProgressState.Finished -> {
                    DynamicBackgroundGradient(
                        LocalConfiguration.current.screenHeightDp.toFloat().dp,
                        DynamicBackgroundGradientColors.Receive
                    )
                }
                else -> {
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(20.dp, innerPadding.calculateTopPadding() - 10.dp, 20.dp, innerPadding.calculateBottomPadding())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                fontSize = 25.sp,
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 30.dp).alpha(0.7f),
                fontWeight = FontWeight.Bold,
                text = "From ${connectionRequest?.getSender()?.name}"
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
                ),
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 50.dp).fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp),
                    text = connectionRequest?.getFileTransferIntent()?.fileName ?: "Unknown file",
                    style = TextStyle(
                        lineBreak = LineBreak.Paragraph,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    modifier = Modifier.padding(20.dp).alpha(0.6f),
                    text = "Size: ${toHumanReadableSize(connectionRequest?.getFileTransferIntent()?.fileSize)}",
                    style = TextStyle(
                        lineBreak = LineBreak.Paragraph,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            when (val state = progress.state) {
                is ReceiveProgressState.Receiving, ReceiveProgressState.Finished -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp)),
                    ) {
                        Text(modifier = Modifier.fillMaxWidth(),
                            fontSize = 40.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            text = "%.0f".format(if (state is ReceiveProgressState.Receiving) state.progress.toFloat() * 100 else 100f) + "%"
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(PaddingValues(bottom = 45.dp, top = 20.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (progress.state) {
                    is ReceiveProgressState.Receiving -> {
                        FilledTonalButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEF5350)),
                            onClick = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    connectionRequest?.cancel()
                                }
                                navController.popBackStack()
                            }) {
                            Text("Cancel")
                        }
                    }
                    is ReceiveProgressState.Finished -> {
                        FilledTonalButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            onClick = {
                                context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                                navController.popBackStack()
                            }) {
                            Text("Open Received Files")
                        }
                    }
                    else -> {
                        FilledTonalButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            onClick = {
                                navController.popBackStack()
                            }) {
                            Text("Go back")
                        }
                    }
                }
            }
        }
    }
}
