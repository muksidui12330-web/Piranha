package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.UiState
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaSurfaceWhite
import java.util.Calendar

@Composable
fun AdminReportsTab(
    ordersState: UiState<List<Order>>
) {
    when (ordersState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KiranaGreen)
            }
        }
        is UiState.Error -> {
            Text(ordersState.message, modifier = Modifier.padding(16.dp))
        }
        is UiState.Success -> {
            val orders = ordersState.data
            val todayStartSec = remember {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis / 1000L
            }

            val todaysOrders = remember(orders, todayStartSec) {
                orders.filter { (it.createdAt?.seconds ?: 0L) >= todayStartSec }
            }
            val todaysEarnings = remember(todaysOrders) {
                todaysOrders
                    .filter { it.orderStatus() != OrderStatus.CANCELLED }
                    .sumOf { it.grandTotal }
            }
            val bestSellers = remember(orders) {
                orders
                    .filter { it.orderStatus() != OrderStatus.CANCELLED }
                    .flatMap { it.items }
                    .groupBy { it.bilingualName() }
                    .map { (name, lines) ->
                        Triple(name, lines.sumOf { it.quantity }, lines.sumOf { it.price * it.quantity })
                    }
                    .sortedByDescending { it.second }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = KiranaGreenLight),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Today's Orders", fontWeight = FontWeight.Medium, color = KiranaGreenDark)
                                Text(
                                    text = "${todaysOrders.size}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaGreenDark
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = KiranaGreenLight),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Today's Earnings", fontWeight = FontWeight.Medium, color = KiranaGreenDark)
                                Text(
                                    text = "Rs $todaysEarnings",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KiranaGreenDark
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Best-Selling Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (bestSellers.isEmpty()) {
                    item {
                        Text("Sales rankings will appear once orders are placed.")
                    }
                } else {
                    items(bestSellers) { (name, qty, revenue) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, KiranaOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Sold: $qty units", fontSize = 14.sp)
                                }
                                Text(
                                    text = "Rs $revenue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = KiranaGreenDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
