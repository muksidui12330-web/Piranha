package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.UiState
import com.example.ui.components.KiranaImage
import com.example.ui.components.OrderStatusChip
import com.example.ui.theme.KiranaAmber
import com.example.ui.theme.KiranaAmberLight
import com.example.ui.theme.KiranaBlue
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.LocationHelper

@Composable
fun AdminOrdersTab(
    ordersState: UiState<List<Order>>,
    language: AppLanguage,
    onConfirmUpiPayment: (String) -> Unit,
    onAcceptCashOrder: (String) -> Unit,
    onUpdateStatus: (Order, OrderStatus, Boolean?) -> Unit,
    onCancelOrRefuse: (Order) -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.get(language)
    var previewScreenshotBase64 by remember { mutableStateOf<String?>(null) }
    var confirmUpiTarget by remember { mutableStateOf<Order?>(null) }

    when (ordersState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KiranaGreen)
            }
        }
        is UiState.Error -> {
            Surface(color = KiranaRedLight, shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(16.dp)) {
                Text(ordersState.message, color = KiranaRed, modifier = Modifier.padding(16.dp))
            }
        }
        is UiState.Success -> {
            val orders = ordersState.data
            if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders yet.", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(orders, key = { it.id }) { order ->
                        val status = order.orderStatus()
                        val isUpi = order.method() == PaymentMethod.UPI

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
                                        text = "#${order.id.takeLast(6).uppercase()} • ${order.customerName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OrderStatusChip(status = status, language = language)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(onClick = {
                                        val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerMobile}"))
                                        runCatching { context.startActivity(dial) }
                                    }) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(order.customerMobile, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(onClick = {
                                        LocationHelper.openGoogleMaps(
                                            context = context,
                                            latitude = order.customerLat,
                                            longitude = order.customerLng,
                                            label = order.customerName
                                        )
                                    }) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.openInMaps, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = order.customerAddress,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                HorizontalDivider(color = KiranaOutline)

                                order.items.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${line.bilingualName()} x${line.quantity}", modifier = Modifier.weight(1f))
                                        Text("Rs ${line.price * line.quantity}", fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(color = KiranaOutline)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${order.method().name} • Total",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Rs ${order.grandTotal}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = KiranaGreenDark
                                    )
                                }

                                if (isUpi) {
                                    Surface(
                                        color = KiranaAmberLight,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "UPI Txn ID: ${order.upiTransactionId ?: "N/A"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            if (!order.paymentScreenshotBase64.isNullOrBlank()) {
                                                KiranaImage(
                                                    imageBase64 = order.paymentScreenshotBase64,
                                                    imageKey = null,
                                                    contentDescription = "UPI Screenshot",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            previewScreenshotBase64 = order.paymentScreenshotBase64
                                                        }
                                                )
                                                Text(
                                                    text = "Tap screenshot to enlarge",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (status == OrderStatus.WAITING_PAYMENT_CONFIRMATION) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = KiranaAmber)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = strings.confirmPaymentWarning,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Status Action Buttons
                                when (status) {
                                    OrderStatus.WAITING_PAYMENT_CONFIRMATION -> {
                                        Button(
                                            onClick = { confirmUpiTarget = order },
                                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(strings.confirmPaymentBtn, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OrderStatus.PLACED -> {
                                        Button(
                                            onClick = { onAcceptCashOrder(order.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                        ) {
                                            Text(strings.startPacking, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OrderStatus.PAYMENT_CONFIRMED -> {
                                        Button(
                                            onClick = { onUpdateStatus(order, OrderStatus.PACKING, null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                        ) {
                                            Text(strings.startPacking, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OrderStatus.PACKING, OrderStatus.READY -> {
                                        Button(
                                            onClick = { onUpdateStatus(order, OrderStatus.OUT_FOR_DELIVERY, null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = KiranaBlue),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(strings.outForDeliveryBtn, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OrderStatus.OUT_FOR_DELIVERY -> {
                                        Button(
                                            onClick = {
                                                onUpdateStatus(
                                                    order,
                                                    OrderStatus.DELIVERED,
                                                    if (order.method() == PaymentMethod.CASH) true else null
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (order.method() == PaymentMethod.CASH) {
                                                    "${strings.cashReceivedBtn} & ${strings.markDeliveredBtn}"
                                                } else {
                                                    strings.markDeliveredBtn
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    else -> Unit
                                }

                                if (status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED) {
                                    OutlinedButton(
                                        onClick = { onCancelOrRefuse(order) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KiranaRed),
                                        border = BorderStroke(1.dp, KiranaRed),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text(
                                            text = if (order.method() == PaymentMethod.CASH) strings.refusedCashBtn else strings.cancelOrder,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmUpiTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmUpiTarget = null },
            title = { Text(strings.confirmPaymentBtn, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${strings.confirmPaymentWarning}\n\nOrder Rs ${target.grandTotal}\nUPI Txn: ${target.upiTransactionId ?: ""}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmUpiPayment(target.id)
                        confirmUpiTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen)
                ) {
                    Text("Money Received in Bank", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUpiTarget = null }) {
                    Text("Wait")
                }
            }
        )
    }

    previewScreenshotBase64?.let { b64 ->
        AlertDialog(
            onDismissRequest = { previewScreenshotBase64 = null },
            title = { Text("UPI Payment Screenshot") },
            text = {
                KiranaImage(
                    imageBase64 = b64,
                    imageKey = null,
                    contentDescription = "Full UPI screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { previewScreenshotBase64 = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
