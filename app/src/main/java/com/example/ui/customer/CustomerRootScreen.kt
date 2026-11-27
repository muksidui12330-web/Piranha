package com.example.ui.customer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.UiState
import com.example.ui.auth.OnboardingScreen
import com.example.ui.components.DeliveryBannerStrip
import com.example.ui.components.KiranaTopHeader
import com.example.ui.theme.KiranaGreen

@Composable
fun CustomerRootScreen(
    viewModel: CustomerViewModel,
    defaultDisplayName: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit
) {
    val shopConfig by viewModel.shopConfig.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cartQuantities by viewModel.cartQuantities.collectAsStateWithLifecycle()
    val cartEntries by viewModel.cartEntries.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val strings = AppStrings.get(language)
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showingCart by rememberSaveable { mutableStateOf(false) }
    var editingAddress by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        if (!actionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionMessage ?: "")
            viewModel.clearActionMessage()
        }
    }

    when (val prof = profileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KiranaGreen)
            }
        }
        is UiState.Error -> {
            OnboardingScreen(
                existingProfile = null,
                defaultName = defaultDisplayName,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onSignOut = onSignOut,
                onSaveProfile = viewModel::saveProfile
            )
        }
        is UiState.Success -> {
            val profile = prof.data
            if (profile == null || !profile.profileCompleted || editingAddress) {
                if (editingAddress) {
                    BackHandler { editingAddress = false }
                }
                OnboardingScreen(
                    existingProfile = profile,
                    defaultName = defaultDisplayName,
                    language = language,
                    onToggleLanguage = onToggleLanguage,
                    onSignOut = onSignOut,
                    onSaveProfile = { name, mobile, address, lat, lng ->
                        val res = viewModel.saveProfile(name, mobile, address, lat, lng)
                        if (res.isSuccess) editingAddress = false
                        res
                    }
                )
            } else if (showingCart) {
                CartCheckoutScreen(
                    cartEntries = cartEntries,
                    shopConfig = shopConfig,
                    profile = profile,
                    language = language,
                    onIncrement = viewModel::incrementItem,
                    onDecrement = viewModel::decrementItem,
                    onBack = { showingCart = false },
                    onPlaceOrder = { method, txnId, screenshot ->
                        val res = viewModel.submitOrder(profile, shopConfig, method, txnId, screenshot)
                        if (res.isSuccess) {
                            showingCart = false
                            selectedTab = 1
                        }
                        res
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        Column {
                            KiranaTopHeader(
                                title = shopConfig.shopName,
                                language = language,
                                onToggleLanguage = onToggleLanguage,
                                showSignOut = true,
                                onSignOut = onSignOut
                            )
                            DeliveryBannerStrip(
                                bannerText = if (language == AppLanguage.BN) {
                                    shopConfig.deliveryTimeTextBn
                                } else {
                                    shopConfig.deliveryTimeText
                                }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Storefront, contentDescription = strings.shopTab) },
                                label = { Text(strings.shopTab) }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = strings.myOrders) },
                                label = { Text(strings.myOrders) }
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> CustomerStorefrontScreen(
                            shopConfig = shopConfig,
                            catalogState = catalogState,
                            profile = profile,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            cartQuantities = cartQuantities,
                            cartEntries = cartEntries,
                            language = language,
                            onSearchChange = viewModel::setSearchQuery,
                            onCategorySelected = viewModel::setSelectedCategory,
                            onIncrement = viewModel::incrementItem,
                            onDecrement = viewModel::decrementItem,
                            onOpenCart = { showingCart = true },
                            onEditAddress = { editingAddress = true },
                            modifier = Modifier.padding(innerPadding)
                        )
                        else -> CustomerOrdersScreen(
                            ordersState = ordersState,
                            shopConfig = shopConfig,
                            profile = profile,
                            language = language,
                            onCancelOrder = { order -> viewModel.cancelOrder(order, profile) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
