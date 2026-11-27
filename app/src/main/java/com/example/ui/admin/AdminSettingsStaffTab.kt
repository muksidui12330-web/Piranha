package com.example.ui.admin

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StarterCatalog
import com.example.model.ShopConfig
import com.example.model.StaffMember
import com.example.model.UiState
import com.example.model.UserRole
import com.example.ui.components.KiranaImage
import com.example.ui.theme.KiranaAmberLight
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

@Composable
fun AdminSettingsStaffTab(
    shopConfig: ShopConfig,
    staffState: UiState<List<StaffMember>>,
    onSaveShopConfig: (ShopConfig) -> Unit,
    onAddOrUpdateStaff: (email: String, role: UserRole, name: String) -> Unit,
    onRemoveStaff: (email: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shopName by remember(shopConfig) { mutableStateOf(shopConfig.shopName) }
    var locationText by remember(shopConfig) { mutableStateOf(shopConfig.locationText) }
    var latText by remember(shopConfig) { mutableStateOf(shopConfig.latitude.toString()) }
    var lngText by remember(shopConfig) { mutableStateOf(shopConfig.longitude.toString()) }
    var openingHours by remember(shopConfig) { mutableStateOf(shopConfig.openingHours) }
    var deliveryChargeText by remember(shopConfig) { mutableStateOf(shopConfig.deliveryCharge.toString()) }
    var minOrderText by remember(shopConfig) { mutableStateOf(shopConfig.minimumOrderAmount.toString()) }
    var deliveryMinutesText by remember(shopConfig) { mutableStateOf(shopConfig.deliveryMinutes.toString()) }
    var bannerText by remember(shopConfig) { mutableStateOf(shopConfig.bannerText) }
    var bannerTextBn by remember(shopConfig) { mutableStateOf(shopConfig.bannerTextBn) }
    var upiId by remember(shopConfig) { mutableStateOf(shopConfig.upiId) }
    var shopPhotoBase64 by remember(shopConfig) { mutableStateOf(shopConfig.shopPhotoBase64) }
    var upiQrBase64 by remember(shopConfig) { mutableStateOf(shopConfig.upiQrBase64) }

    var newAdminEmail by remember { mutableStateOf("") }
    var newPackerEmail by remember { mutableStateOf("") }
    var newPackerName by remember { mutableStateOf("") }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val shopPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageCompressionUtil.compressUriToBase64(context, uri, maxDimensionPx = 560, maxBytes = 140_000)
                    .onSuccess { shopPhotoBase64 = it }
            }
        }
    }

    val qrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageCompressionUtil.compressUriToBase64(context, uri, maxDimensionPx = 420, maxBytes = 120_000)
                    .onSuccess { upiQrBase64 = it }
            }
        }
    }

    val staffList = (staffState as? UiState.Success)?.data ?: emptyList()
    val adminStaff = staffList.filter { it.role == UserRole.ADMIN.name }
    val packerStaff = staffList.filter { it.role == UserRole.PACKER.name }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Background Loud Alert & Unrestricted Battery Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = KiranaAmberLight),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Loud Order Alert & Lock-Screen Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Allow notifications and set Battery to Unrestricted so \"Sir, new order has come\" rings even when your phone is locked.",
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = { notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Notifications")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(appSettings) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unrestricted Battery")
                        }
                    }
                }
            }
        }

        // 2. Editable Shop Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KiranaOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Shop Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    KiranaImage(
                        imageBase64 = shopPhotoBase64,
                        imageKey = "img_shop_hero",
                        contentDescription = "Shop photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    OutlinedButton(
                        onClick = {
                            shopPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Shop Photo", fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = locationText, onValueChange = { locationText = it }, label = { Text("Shop Location Address") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = openingHours, onValueChange = { openingHours = it }, label = { Text("Opening & Closing Hours") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deliveryChargeText,
                            onValueChange = { deliveryChargeText = it.filter { c -> c.isDigit() } },
                            label = { Text("Delivery Rs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minOrderText,
                            onValueChange = { minOrderText = it.filter { c -> c.isDigit() } },
                            label = { Text("Min Order Rs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = deliveryMinutesText,
                        onValueChange = { deliveryMinutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Delivery Minutes (Timer)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = bannerText, onValueChange = { bannerText = it }, label = { Text("Banner Text (English)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bannerTextBn, onValueChange = { bannerTextBn = it }, label = { Text("Banner Text (Bengali)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("Shop UPI ID") }, modifier = Modifier.fillMaxWidth())

                    OutlinedButton(
                        onClick = {
                            qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (upiQrBase64 != null) "Custom UPI QR Uploaded (Change)" else "Upload Custom UPI QR Image", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val mins = (deliveryMinutesText.toIntOrNull() ?: 30).coerceIn(5, 180)
                            onSaveShopConfig(
                                shopConfig.copy(
                                    shopName = shopName,
                                    locationText = locationText,
                                    latitude = latText.toDoubleOrNull() ?: shopConfig.latitude,
                                    longitude = lngText.toDoubleOrNull() ?: shopConfig.longitude,
                                    openingHours = openingHours,
                                    deliveryCharge = deliveryChargeText.toIntOrNull() ?: 20,
                                    minimumOrderAmount = minOrderText.toIntOrNull() ?: 200,
                                    deliveryMinutes = mins,
                                    deliveryTimeText = bannerText.ifBlank { "Delivery in $mins minutes" },
                                    deliveryTimeTextBn = bannerTextBn.ifBlank { "$mins মিনিটে ডেলিভারি" },
                                    bannerText = bannerText,
                                    bannerTextBn = bannerTextBn,
                                    upiId = upiId,
                                    shopPhotoBase64 = shopPhotoBase64,
                                    upiQrBase64 = upiQrBase64
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Text("Save Shop Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // 3. Admin Accounts (3 Pre-set / Editable Slots) & Packer Staff
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KiranaOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Admin Accounts (${adminStaff.size.coerceAtLeast(1)}/${StarterCatalog.MAX_ADMIN_SLOTS} slots)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(color = KiranaOutline.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "Primary Owner: ${StarterCatalog.PRIMARY_ADMIN_EMAIL}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    adminStaff.filter { it.email != StarterCatalog.PRIMARY_ADMIN_EMAIL }.forEach { admin ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(admin.email, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveStaff(admin.email) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Admin", tint = KiranaRed)
                            }
                        }
                    }

                    val adminCount = (adminStaff.map { it.email } + StarterCatalog.PRIMARY_ADMIN_EMAIL).distinct().size
                    if (adminCount < StarterCatalog.MAX_ADMIN_SLOTS) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newAdminEmail,
                                onValueChange = { newAdminEmail = it },
                                label = { Text("Add Admin Gmail") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newAdminEmail.isNotBlank()) {
                                        onAddOrUpdateStaff(newAdminEmail, UserRole.ADMIN, "Co-Admin")
                                        newAdminEmail = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen)
                            ) {
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = KiranaOutline)

                    Text("Packer Staff (Packing List Only)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    packerStaff.forEach { packer ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(packer.name.ifBlank { "Packer" }, fontWeight = FontWeight.Bold)
                                Text(packer.email, fontSize = 13.sp, color = KiranaGreenDark)
                            }
                            IconButton(onClick = { onRemoveStaff(packer.email) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Packer", tint = KiranaRed)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newPackerName,
                        onValueChange = { newPackerName = it },
                        label = { Text("Packer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPackerEmail,
                            onValueChange = { newPackerEmail = it },
                            label = { Text("Packer Gmail") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newPackerEmail.isNotBlank()) {
                                    onAddOrUpdateStaff(newPackerEmail, UserRole.PACKER, newPackerName.ifBlank { "Packer" })
                                    newPackerEmail = ""
                                    newPackerName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen)
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
