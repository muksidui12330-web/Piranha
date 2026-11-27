package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.StarRatingRow
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.LocationHelper

@Composable
fun AdminCustomersTab(
    customersState: UiState<List<UserProfile>>,
    onUpdateCustomerTrust: (customerId: String, rating: Int, cashEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current

    when (customersState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KiranaGreen)
            }
        }
        is UiState.Error -> {
            Text(customersState.message, color = KiranaRed, modifier = Modifier.padding(16.dp))
        }
        is UiState.Success -> {
            val customers = customersState.data
            if (customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No customers registered yet.", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(customers, key = { it.uid }) { user ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, KiranaOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name.ifBlank { user.email },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Text("${user.mobile} • ${user.email}", fontSize = 13.sp)
                                    }
                                    StarRatingRow(rating = user.rating)
                                }

                                Text(user.address, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Delivered orders: ${user.successfulDeliveredCount}",
                                        fontWeight = FontWeight.Medium,
                                        color = KiranaGreenDark
                                    )
                                    if (user.latitude != null && user.longitude != null) {
                                        OutlinedButton(onClick = {
                                            LocationHelper.openGoogleMaps(
                                                context,
                                                user.latitude,
                                                user.longitude,
                                                user.name
                                            )
                                        }) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Maps")
                                        }
                                    }
                                }

                                HorizontalDivider(color = KiranaOutline)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Edit Rating:", fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = {
                                                if (user.rating > 1) {
                                                    val next = user.rating - 1
                                                    onUpdateCustomerTrust(
                                                        user.uid,
                                                        next,
                                                        if (next <= 1) false else user.cashEnabled
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease rating")
                                        }
                                        Text("${user.rating}★", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        IconButton(
                                            onClick = {
                                                if (user.rating < 5) {
                                                    val next = user.rating + 1
                                                    onUpdateCustomerTrust(
                                                        user.uid,
                                                        next,
                                                        if (next >= 2) true else user.cashEnabled
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase rating")
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (user.cashEnabled) "Cash ON" else "Cash OFF",
                                            fontWeight = FontWeight.Bold,
                                            color = if (user.cashEnabled) KiranaGreenDark else KiranaRed
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = user.cashEnabled,
                                            onCheckedChange = { checked ->
                                                onUpdateCustomerTrust(user.uid, user.rating, checked)
                                            }
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
}
