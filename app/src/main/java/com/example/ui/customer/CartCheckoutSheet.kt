package com.example.ui.customer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.CartEntry
import com.example.model.CatalogItem
import com.example.model.PaymentMethod
import com.example.model.ShopConfig
import com.example.model.UserProfile
import com.example.ui.components.DeliveryBannerStrip
import com.example.ui.components.KiranaImage
import com.example.ui.components.UpiQrCodeDisplay
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceTint
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

@Composable
fun CartCheckoutScreen(
    cartEntries: List<CartEntry>,
    shopConfig: ShopConfig,
    profile: UserProfile,
    language: AppLanguage,
    onIncrement: (CatalogItem) -> Unit,
    onDecrement: (String) -> Unit,
    onBack: () -> Unit,
    onPlaceOrder: suspend (PaymentMethod, String?, String?) -> Result<String>
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = AppStrings.get(language)

    val itemsTotal = cartEntries.sumOf { it.item.price * it.quantity }
    val deliveryCharge = if (cartEntries.isNotEmpty()) shopConfig.deliveryCharge else 0
    val grandTotal = itemsTotal + deliveryCharge
    val meetsMinOrder = itemsTotal >= shopConfig.minimumOrderAmount
    val canPayCash = profile.canPayCash()

    var selectedMethod by remember(canPayCash) {
        mutableStateOf(if (canPayCash) PaymentMethod.CASH else PaymentMethod.UPI)
    }
    var upiTxnId by remember { mutableStateOf("") }
    var screenshotBase64 by remember { mutableStateOf<String?>(null) }
    var compressingImage by remember { mutableStateOf(false) }
    var placingOrder by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            compressingImage = true
            errorMsg = null
            scope.launch {
                ImageCompressionUtil.compressUriToBase64(context, uri, maxDimensionPx = 640, maxBytes = 180_000)
                    .onSuccess { b64 -> screenshotBase64 = b64 }
                    .onFailure { err -> errorMsg = err.localizedMessage ?: "Failed to load screenshot" }
                compressingImage = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                Surface(color = KiranaGreen, contentColor = KiranaSurfaceWhite) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = KiranaSurfaceWhite
                            )
                        }
                        Text(
                            text = strings.cartTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = KiranaSurfaceWhite
                        )
                    }
                }
                DeliveryBannerStrip(
                    bannerText = if (language == AppLanguage.BN) shopConfig.deliveryTimeTextBn else shopConfig.deliveryTimeText
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KiranaSurfaceTint)
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cart Line Items
            Card(
                colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KiranaOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    cartEntries.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.item.bilingualName(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${entry.item.unit} • Rs ${entry.item.price}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onDecrement(entry.item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    text = "${entry.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                IconButton(
                                    onClick = { onIncrement(entry.item) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                        HorizontalDivider(color = KiranaOutline)
                    }

                    // Separate Bill Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.itemsTotal, fontSize = 16.sp)
                        Text("Rs $itemsTotal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.deliveryCharge, fontSize = 16.sp)
                        Text("Rs $deliveryCharge", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    HorizontalDivider(color = KiranaOutline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.grandTotal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "Rs $grandTotal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = KiranaGreenDark
                        )
                    }
                }
            }

            if (!meetsMinOrder) {
                Surface(
                    color = KiranaRedLight,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.minOrderWarning(shopConfig.minimumOrderAmount),
                        color = KiranaRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Payment Method Card (Only Cash and UPI)
            Card(
                colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KiranaOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.paymentMethodTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Cash Option (only if rating > 1 star & enabled)
                    Surface(
                        color = if (canPayCash && selectedMethod == PaymentMethod.CASH) KiranaGreenLight else KiranaSurfaceTint,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (canPayCash && selectedMethod == PaymentMethod.CASH) KiranaGreen else KiranaOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canPayCash) { selectedMethod = PaymentMethod.CASH }
                            .testTag("payment_cash_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == PaymentMethod.CASH,
                                onClick = { if (canPayCash) selectedMethod = PaymentMethod.CASH },
                                enabled = canPayCash
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.payCash,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (canPayCash) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!canPayCash) {
                                    Text(
                                        text = strings.cashBlockedMsg,
                                        color = KiranaRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // UPI Option
                    Surface(
                        color = if (selectedMethod == PaymentMethod.UPI) KiranaGreenLight else KiranaSurfaceTint,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (selectedMethod == PaymentMethod.UPI) KiranaGreen else KiranaOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMethod = PaymentMethod.UPI }
                            .testTag("payment_upi_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == PaymentMethod.UPI,
                                onClick = { selectedMethod = PaymentMethod.UPI }
                            )
                            Text(
                                text = strings.payUpi,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (selectedMethod == PaymentMethod.UPI) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UpiQrCodeDisplay(
                                upiId = shopConfig.upiId,
                                shopName = shopConfig.shopName,
                                customQrBase64 = shopConfig.upiQrBase64
                            )
                            Surface(
                                color = KiranaSurfaceTint,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.shopUpiId, fontSize = 13.sp)
                                        Text(
                                            text = shopConfig.upiId,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = KiranaGreenDark
                                        )
                                    }
                                    OutlinedButton(onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("UPI ID", shopConfig.upiId))
                                        Toast.makeText(context, "UPI ID copied", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.copyUpiId, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("upload_upi_screenshot_button")
                            ) {
                                if (compressingImage) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = if (screenshotBase64 != null) Icons.Default.CheckCircle else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (screenshotBase64 != null) KiranaGreenDark else KiranaGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (screenshotBase64 != null) strings.screenshotSelected else strings.uploadScreenshot,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!screenshotBase64.isNullOrBlank()) {
                                KiranaImage(
                                    imageBase64 = screenshotBase64,
                                    imageKey = null,
                                    contentDescription = "UPI payment screenshot",
                                    modifier = Modifier
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            OutlinedTextField(
                                value = upiTxnId,
                                onValueChange = { upiTxnId = it },
                                label = { Text(strings.upiTxnNumber) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upi_txn_input")
                            )
                        }
                    }
                }
            }

            if (!errorMsg.isNullOrBlank()) {
                Surface(
                    color = KiranaRedLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorMsg ?: "", color = KiranaRed, modifier = Modifier.padding(12.dp))
                }
            }

            Button(
                onClick = {
                    if (!meetsMinOrder) return@Button
                    if (selectedMethod == PaymentMethod.UPI) {
                        if (screenshotBase64.isNullOrBlank()) {
                            errorMsg = if (language == AppLanguage.BN) {
                                "অনুগ্রহ করে পেমেন্ট স্ক্রিনশট আপলোড করুন"
                            } else {
                                "Please upload the UPI payment screenshot"
                            }
                            return@Button
                        }
                        if (upiTxnId.trim().length < 4) {
                            errorMsg = if (language == AppLanguage.BN) {
                                "অনুগ্রহ করে সঠিক UPI ট্রানজ্যাকশন নম্বর দিন"
                            } else {
                                "Please enter your UPI transaction number"
                            }
                            return@Button
                        }
                    }
                    placingOrder = true
                    errorMsg = null
                    scope.launch {
                        onPlaceOrder(selectedMethod, upiTxnId.takeIf { selectedMethod == PaymentMethod.UPI }, screenshotBase64)
                            .onSuccess { onBack() }
                            .onFailure { err -> errorMsg = err.localizedMessage ?: "Failed to place order" }
                        placingOrder = false
                    }
                },
                enabled = meetsMinOrder && cartEntries.isNotEmpty() && !placingOrder && !compressingImage,
                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("place_order_button")
            ) {
                if (placingOrder) {
                    CircularProgressIndicator(color = KiranaSurfaceWhite, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "${strings.placeOrder} • Rs $grandTotal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
