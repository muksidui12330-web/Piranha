package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.OrderStatus
import com.example.model.UiState
import com.example.service.AdminOrderAlertService
import com.example.ui.components.KiranaTopHeader
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaSurfaceWhite

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.get(language)
    val shopConfig by viewModel.shopConfig.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        AdminOrderAlertService.start(context)
    }

    LaunchedEffect(toastMsg) {
        if (!toastMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMsg ?: "")
            viewModel.clearToast()
        }
    }

    val unacknowledgedCount = remember(ordersState) {
        val list = (ordersState as? UiState.Success)?.data ?: emptyList()
        list.count {
            !it.adminAcknowledged &&
                (it.orderStatus() == OrderStatus.PLACED || it.orderStatus() == OrderStatus.WAITING_PAYMENT_CONFIRMATION)
        }
    }

    Scaffold(
        topBar = {
            Column {
                KiranaTopHeader(
                    title = strings.adminPanelTitle,
                    language = language,
                    onToggleLanguage = onToggleLanguage,
                    showSignOut = true,
                    onSignOut = {
                        AdminOrderAlertService.stop(context)
                        onSignOut()
                    }
                )
                if (unacknowledgedCount > 0) {
                    Surface(
                        color = KiranaRed,
                        contentColor = KiranaSurfaceWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${strings.newOrderAlertTitle} ($unacknowledgedCount)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    AdminOrderAlertService.silence(context)
                                    viewModel.acknowledgeAllAlerts()
                                    selectedTab = 0
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KiranaSurfaceWhite,
                                    contentColor = KiranaRed
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(strings.silenceAlertBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = strings.tabOrders) },
                    label = { Text(strings.tabOrders) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = strings.tabItems) },
                    label = { Text(strings.tabItems) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.People, contentDescription = strings.tabCustomers) },
                    label = { Text(strings.tabCustomers) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = strings.tabReports) },
                    label = { Text(strings.tabReports) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = strings.tabSettings) },
                    label = { Text(strings.tabSettings) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AdminOrdersTab(
                    ordersState = ordersState,
                    language = language,
                    onConfirmUpiPayment = viewModel::confirmUpiPayment,
                    onAcceptCashOrder = viewModel::acceptCashOrder,
                    onUpdateStatus = viewModel::updateOrderStatus,
                    onCancelOrRefuse = viewModel::cancelOrRefuseOrder
                )
                1 -> AdminItemsTab(
                    itemsState = itemsState,
                    onSaveItem = viewModel::saveItem,
                    onToggleStock = viewModel::toggleItemStock,
                    onDeleteItem = viewModel::deleteItem
                )
                2 -> AdminCustomersTab(
                    customersState = customersState,
                    onUpdateCustomerTrust = viewModel::updateCustomerTrust
                )
                3 -> AdminReportsTab(ordersState = ordersState)
                else -> AdminSettingsStaffTab(
                    shopConfig = shopConfig,
                    staffState = staffState,
                    onSaveShopConfig = viewModel::saveShopSettings,
                    onAddOrUpdateStaff = viewModel::addOrUpdateStaff,
                    onRemoveStaff = viewModel::removeStaff
                )
            }
        }
    }
}
