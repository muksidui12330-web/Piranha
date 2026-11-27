package com.example.ui.customer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KiranaRepository
import com.example.data.StarterCatalog
import com.example.model.CartEntry
import com.example.model.CatalogItem
import com.example.model.ItemCategory
import com.example.model.Order
import com.example.model.OrderLineItem
import com.example.model.PaymentMethod
import com.example.model.ShopConfig
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val repository: KiranaRepository,
    private val currentUserId: String
) : ViewModel() {

    val shopConfig: StateFlow<ShopConfig> = repository.observeShopConfig()
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Shop config stream error", fsErr ?: error)
            emit(StarterCatalog.defaultShopConfig)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StarterCatalog.defaultShopConfig)

    val catalogState: StateFlow<UiState<List<CatalogItem>>> = repository.observeCatalogItems()
        .map<List<CatalogItem>, UiState<List<CatalogItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Catalog stream error", root)
            emit(UiState.Error(root.message ?: "Unable to load items"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Profile stream error", root)
            emit(UiState.Error(root.message ?: "Unable to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ordersState: StateFlow<UiState<List<Order>>> = repository.observeMyOrders(currentUserId)
        .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Orders stream error", root)
            emit(UiState.Error(root.message ?: "Unable to load orders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
    val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

    private val _cartQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cartQuantities: StateFlow<Map<String, Int>> = _cartQuantities.asStateFlow()

    val cartEntries: StateFlow<List<CartEntry>> = combine(
        catalogState,
        _cartQuantities
    ) { state, qtyMap ->
        val items = (state as? UiState.Success)?.data ?: emptyList()
        val byId = items.associateBy { it.id }
        qtyMap.entries.mapNotNull { (itemId, qty) ->
            val item = byId[itemId]
            if (item != null && qty > 0 && item.available) CartEntry(item, qty) else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: ItemCategory?) {
        _selectedCategory.value = category
    }

    fun incrementItem(item: CatalogItem) {
        if (!item.available) return
        _cartQuantities.update { current ->
            val next = (current[item.id] ?: 0) + 1
            current + (item.id to next)
        }
    }

    fun decrementItem(itemId: String) {
        _cartQuantities.update { current ->
            val existing = current[itemId] ?: 0
            if (existing <= 1) current - itemId else current + (itemId to (existing - 1))
        }
    }

    fun clearCart() {
        _cartQuantities.value = emptyMap()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    suspend fun saveProfile(
        name: String,
        mobile: String,
        address: String,
        lat: Double?,
        lng: Double?
    ): Result<Unit> = repository.saveCustomerOnboardingProfile(name, mobile, address, lat, lng)

    suspend fun submitOrder(
        profile: UserProfile,
        config: ShopConfig,
        paymentMethod: PaymentMethod,
        upiTransactionId: String?,
        paymentScreenshotBase64: String?
    ): Result<String> {
        val entries = cartEntries.value
        val itemsTotal = entries.sumOf { it.item.price * it.quantity }
        if (itemsTotal < config.minimumOrderAmount) {
            return Result.failure(IllegalArgumentException("Minimum order is Rs ${config.minimumOrderAmount}"))
        }
        val lineItems = entries.map { entry ->
            OrderLineItem(
                itemId = entry.item.id,
                nameEn = entry.item.nameEn,
                nameBn = entry.item.nameBn,
                unit = entry.item.unit,
                price = entry.item.price,
                quantity = entry.quantity
            )
        }
        val result = repository.placeOrder(
            profile = profile,
            items = lineItems,
            itemsTotal = itemsTotal,
            deliveryCharge = config.deliveryCharge,
            paymentMethod = paymentMethod,
            upiTransactionId = upiTransactionId,
            paymentScreenshotBase64 = paymentScreenshotBase64
        )
        if (result.isSuccess) {
            clearCart()
        }
        return result
    }

    fun cancelOrder(order: Order, profile: UserProfile) {
        viewModelScope.launch {
            repository.customerCancelOrder(order, profile)
                .onSuccess {
                    _actionMessage.value = "Order cancelled (-1 star applied)"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to cancel order"
                }
        }
    }

    private companion object {
        const val TAG = "CustomerVM"
    }
}
