package com.example.ui.packer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KiranaRepository
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.UiState
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

class PackerViewModel(
    private val repository: KiranaRepository
) : ViewModel() {

    val packingOrdersState: StateFlow<UiState<List<Order>>> = repository.observeAllOrders()
        .map<List<Order>, UiState<List<Order>>> { list ->
            UiState.Success(list.filter { it.orderStatus() == OrderStatus.PACKING })
        }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Packer orders stream error", root)
            emit(UiState.Error(root.message ?: "Unable to load packing list"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun markReady(orderId: String) {
        viewModelScope.launch {
            repository.packerMarkReady(orderId)
                .onSuccess { _message.value = "Order marked Ready!" }
                .onFailure { err -> _message.value = err.localizedMessage ?: "Update failed" }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private companion object {
        const val TAG = "PackerVM"
    }
}
