package com.example.ui.packer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.Order
import com.example.model.UiState
import com.example.ui.components.KiranaTopHeader
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceTint
import com.example.ui.theme.KiranaSurfaceWhite

@Composable
fun PackerScreen(
    viewModel: PackerViewModel,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.packingOrdersState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = AppStrings.get(language)

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message ?: "")
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            KiranaTopHeader(
                title = strings.packerQueueTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                showSignOut = true,
                onSignOut = onSignOut
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KiranaSurfaceTint)
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            when (val ui = state) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        color = KiranaGreen,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Error -> {
                    Surface(
                        color = KiranaRedLight,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        Text(ui.message, color = KiranaRed, modifier = Modifier.padding(16.dp))
                    }
                }
                is UiState.Success -> {
                    val orders = ui.data
                    if (orders.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = KiranaGreenDark,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = strings.noPackingOrders,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(orders, key = { it.id }) { order ->
                                PackerOrderCard(
                                    order = order,
                                    readyLabel = strings.markReady,
                                    onMarkReady = { viewModel.markReady(order.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackerOrderCard(
    order: Order,
    readyLabel: String,
    onMarkReady: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, KiranaOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id.takeLast(6).uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Surface(color = KiranaGreenLight, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        text = order.customerName.substringBefore(" "),
                        fontWeight = FontWeight.Bold,
                        color = KiranaGreenDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = KiranaOutline)

            order.items.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.bilingualName(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = line.unit,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        color = KiranaGreenLight,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "x ${line.quantity}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = KiranaGreenDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = onMarkReady,
                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("packer_ready_${order.id}")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(readyLabel, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
