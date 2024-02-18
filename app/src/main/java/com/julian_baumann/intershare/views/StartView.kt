package com.julian_baumann.intershare.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julian_baumann.data_rct.ConnectionRequest
import com.julian_baumann.data_rct.NearbyConnectionDelegate
import com.julian_baumann.intershare.UserPreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartView(userPreferencesManager: UserPreferencesManager) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val nearbyServerDelegate = object : NearbyConnectionDelegate {
        override fun receivedConnectionRequest(request: ConnectionRequest) {
            TODO("Not yet implemented")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "InterShare",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {

//                    val visibility = remember { mutableStateOf(false) }
//                    val options = listOf("Visible", "Hidden")
//                    val icons = listOf(
//                        Icons.Filled.Visibility,
//                        Icons.Filled.VisibilityOff
//                    )
//
//                    SingleChoiceSegmentedButtonRow(
//                        modifier = Modifier.padding(20.dp)
//                    ) {
//                        options.forEachIndexed { index, label ->
//                            SegmentedButton(
//                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
//                                icon = {
//                                    SegmentedButtonDefaults.Icon(active = index in visibility) {
//                                        Icon(
//                                            imageVector = icons[index],
//                                            contentDescription = null,
//                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
//                                        )
//                                    }
//                                },
//                                onClick = {
//                                },
//                                selected = true
//                            ) {
//                                Text(label)
//                            }
//                        }
//                    }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { /* TODO: Handle image or video sharing */ },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Image or Video")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { /* TODO: Handle file sharing */ },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("File")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { /* TODO: Handle file sharing */ },
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text("Show received files")
                }
            }
        }
    }
}
