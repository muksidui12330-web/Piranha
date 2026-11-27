package com.example.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KiranaRepository
import com.example.data.StarterCatalog
import com.example.model.CatalogItem
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.ShopConfig
import com.example.model.StaffMember
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: KiranaRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureSeededForAdmin()
        }
    }

    val shopConfig: StateFlow<ShopConfig> = repository.observeShopConfig()
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(StarterCatalog.defaultShopConfig)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StarterCatalog.defaultShopConfig)

    val ordersState: StateFlow<UiState<List<Order>>> = repository.observeAllOrders()
        .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Admin orders error", root)
            emit(UiState.Error(root.message ?: "Unable to load orders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val itemsState: StateFlow<UiState<List<CatalogItem>>> = repository.observeCatalogItems()
        .map<List<CatalogItem>, UiState<List<CatalogItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            emit(UiState.Error(root.message ?: "Unable to load items"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val customersState: StateFlow<UiState<List<UserProfile>>> = repository.observeAllCustomers()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            emit(UiState.Error(root.message ?: "Unable to load customers"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val staffState: StateFlow<UiState<List<StaffMember>>> = repository.observeStaff()
        .map<List<StaffMember>, UiState<List<StaffMember>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            emit(UiState.Error(root.message ?: "Unable to load staff"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun acknowledgeAllAlerts() {
        viewModelScope.launch {
            repository.adminAcknowledgeAllActive()
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to acknowledge" }
        }
    }

    fun confirmUpiPayment(orderId: String) {
        viewModelScope.launch {
            repository.adminConfirmUpiAndMoveToPacking(orderId)
                .onSuccess { _toastMessage.value = "Payment confirmed! Moved to Packer queue." }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to confirm payment" }
        }
    }

    fun acceptCashOrder(orderId: String) {
        viewModelScope.launch {
            repository.adminAcceptCashAndMoveToPacking(orderId)
                .onSuccess { _toastMessage.value = "Cash order accepted! Moved to Packer queue." }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to accept order" }
        }
    }

    fun updateOrderStatus(order: Order, nextStatus: OrderStatus, cashReceived: Boolean? = null) {
        viewModelScope.launch {
            repository.adminUpdateOrderStatus(order, nextStatus, cashReceived)
                .onSuccess { _toastMessage.value = "Order updated to ${nextStatus.enLabel}" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to update order" }
        }
    }

    fun cancelOrRefuseOrder(order: Order) {
        viewModelScope.launch {
            repository.adminCancelOrRefuseOrder(order)
                .onSuccess { _toastMessage.value = "Order cancelled & customer rating adjusted (-1 star)" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to cancel order" }
        }
    }

    fun saveItem(item: CatalogItem) {
        viewModelScope.launch {
            repository.saveCatalogItem(item)
                .onSuccess { _toastMessage.value = "Item saved" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to save item" }
        }
    }

    fun toggleItemStock(itemId: String, available: Boolean) {
        viewModelScope.launch {
            repository.toggleItemAvailability(itemId, available)
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to update stock" }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteCatalogItem(itemId)
                .onSuccess { _toastMessage.value = "Item deleted" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to delete item" }
        }
    }

    fun updateCustomerTrust(customerId: String, rating: Int, cashEnabled: Boolean) {
        viewModelScope.launch {
            repository.adminUpdateCustomerTrust(customerId, rating, cashEnabled)
                .onSuccess { _toastMessage.value = "Customer trust settings updated" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to update customer" }
        }
    }

    fun saveShopSettings(config: ShopConfig) {
        viewModelScope.launch {
            repository.saveShopConfig(config)
                .onSuccess { _toastMessage.value = "Shop settings saved" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to save settings" }
        }
    }

    fun addOrUpdateStaff(email: String, role: UserRole, name: String) {
        viewModelScope.launch {
            repository.saveStaffMember(email, role, name)
                .onSuccess { _toastMessage.value = "Staff member saved" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to save staff" }
        }
    }

    fun removeStaff(email: String) {
        viewModelScope.launch {
            repository.removeStaffMember(email)
                .onSuccess { _toastMessage.value = "Staff member removed" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to remove staff" }
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
