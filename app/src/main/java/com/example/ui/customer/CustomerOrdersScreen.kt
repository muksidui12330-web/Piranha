package com.example.ui.customer

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.ShopConfig
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.DeliveryCountdownTimer
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.StarRatingRow
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceTint
import com.example.ui.theme.KiranaSurfaceWhite

@Composable
fun CustomerOrdersScreen(
    ordersState: UiState<List<Order>>,
    shopConfig: ShopConfig,
    profile: UserProfile,
    language: AppLanguage,
    onCancelOrder: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.get(language)
    var orderToCancel by remember { mutableStateOf<Order?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KiranaSurfaceTint)
    ) {
        Surface(
            color = KiranaSurfaceWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, KiranaOutline)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.customerRatingLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                StarRatingRow(rating = profile.rating)
            }
        }

        when (ordersState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KiranaGreen)
                }
            }
            is UiState.Error -> {
                Surface(
                    color = KiranaRedLight,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(ordersState.message, color = KiranaRed, modifier = Modifier.padding(16.dp))
                }
            }
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) {
                                "আপনার এখনও কোনো অর্ডার নেই"
                            } else {
                                "You have not placed any orders yet."
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(orders, key = { it.id }) { order ->
                            val status = order.orderStatus()
                            val canCancel = status in listOf(
                                OrderStatus.WAITING_PAYMENT_CONFIRMATION,
                                OrderStatus.PLACED,
                                OrderStatus.PAYMENT_CONFIRMED,
                                OrderStatus.PACKING
                            )

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
                                            text = "#${order.id.takeLast(6).uppercase()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        OrderStatusChip(status = status, language = language)
                                    }

                                    // 30-min countdown timer shown only after order is confirmed and before Delivered/Cancelled
                                    if (order.confirmedAt != null &&
                                        status != OrderStatus.DELIVERED &&
                                        status != OrderStatus.CANCELLED
                                    ) {
                                        DeliveryCountdownTimer(
                                            confirmedAt = order.confirmedAt,
                                            deliveryMinutes = shopConfig.deliveryMinutes,
                                            language = language
                                        )
                                    }

                                    order.items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${item.bilingualName()} x${item.quantity}",
                                                fontSize = 15.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Rs ${item.price * item.quantity}",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = KiranaOutline)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${order.method().name} • ${strings.grandTotal}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Rs ${order.grandTotal}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = KiranaGreenDark
                                        )
                                    }

                                    if (canCancel) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        OutlinedButton(
                                            onClick = { orderToCancel = order },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KiranaRed),
                                            border = BorderStroke(1.dp, KiranaRed),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Text(strings.cancelOrder, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    orderToCancel?.let { target ->
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text(strings.cancelOrder, fontWeight = FontWeight.Bold) },
            text = { Text(strings.cancelWarning, fontSize = 16.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelOrder(target)
                        orderToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KiranaRed)
                ) {
                    Text(strings.confirmCancel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToCancel = null }) {
                    Text(strings.keepOrder, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
