package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    CUSTOMER,
    PACKER,
    ADMIN;

    companion object {
        fun fromString(raw: String?): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class ItemCategory(val enLabel: String, val bnLabel: String) {
    GRAINS("Grains & Rice", "চাল ও ডাল"),
    OILS_SPICES("Oil & Spices", "তেল ও মশলা"),
    DAIRY_TEA("Tea & Dairy", "চা ও দুধ"),
    SNACKS_ESSENTIALS("Daily Essentials", "নিত্যপ্রয়োজনীয়");

    companion object {
        fun fromString(raw: String?): ItemCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SNACKS_ESSENTIALS
    }
}

enum class PaymentMethod {
    CASH,
    UPI;

    companion object {
        fun fromString(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UPI
    }
}

enum class OrderStatus(val enLabel: String, val bnLabel: String) {
    WAITING_PAYMENT_CONFIRMATION("Waiting for payment confirmation", "পেমেন্ট নিশ্চিতকরণের অপেক্ষায়"),
    PLACED("Placed", "অর্ডার দেওয়া হয়েছে"),
    PAYMENT_CONFIRMED("Payment confirmed", "পেমেন্ট নিশ্চিত হয়েছে"),
    PACKING("Packing", "প্যাকিং চলছে"),
    READY("Ready", "প্রস্তুত"),
    OUT_FOR_DELIVERY("Out for delivery", "ডেলিভারির জন্য বেরিয়েছে"),
    DELIVERED("Delivered", "ডেলিভারি সম্পন্ন"),
    CANCELLED("Cancelled", "বাতিল হয়েছে");

    companion object {
        fun fromString(raw: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PLACED
    }
}

@Keep
@IgnoreExtraProperties
data class ShopConfig(
    @DocumentId val id: String = "shop",
    val shopName: String = "Gramin Kirana",
    val bannerText: String = "Delivery in 30 minutes",
    val bannerTextBn: String = "৩০ মিনিটে ডেলিভারি",
    val locationText: String = "Bazar Tiniali, Nagaon, Assam 782001",
    val latitude: Double = 26.3464,
    val longitude: Double = 92.6840,
    val openingHours: String = "7:00 AM - 9:00 PM",
    val deliveryCharge: Int = 20,
    val minimumOrderAmount: Int = 200,
    val deliveryMinutes: Int = 30,
    val deliveryTimeText: String = "Delivery in 30 minutes",
    val deliveryTimeTextBn: String = "৩০ মিনিটে ডেলিভারি",
    val upiId: String = "graminkirana@upi",
    val upiQrBase64: String? = null,
    val shopPhotoBase64: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val email: String = "",
    val role: String = "PACKER",
    val name: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CatalogItem(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val category: String = ItemCategory.GRAINS.name,
    val price: Int = 0,
    val unit: String = "",
    val available: Boolean = true,
    val imageKey: String? = null,
    val imageBase64: String? = null,
    val similarItemIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun bilingualName(): String =
        if (nameBn.isNotBlank() && nameBn != nameEn) "$nameEn / $nameBn" else nameEn
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val mobile: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating: Int = 5,
    val successfulDeliveredCount: Int = 0,
    val cashEnabled: Boolean = true,
    val cashAdminOverride: Boolean = false,
    val profileCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun canPayCash(): Boolean = rating > 1 && cashEnabled
}

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val itemId: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val unit: String = "",
    val price: Int = 0,
    val quantity: Int = 1
) {
    fun bilingualName(): String =
        if (nameBn.isNotBlank() && nameBn != nameEn) "$nameEn / $nameBn" else nameEn
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val customerAddress: String = "",
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val items: List<OrderLineItem> = emptyList(),
    val itemsTotal: Int = 0,
    val deliveryCharge: Int = 20,
    val grandTotal: Int = 0,
    val paymentMethod: String = PaymentMethod.UPI.name,
    val upiTransactionId: String? = null,
    val paymentScreenshotBase64: String? = null,
    val status: String = OrderStatus.PLACED.name,
    val cashReceived: Boolean = false,
    val adminAcknowledged: Boolean = false,
    val confirmedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun orderStatus(): OrderStatus = OrderStatus.fromString(status)
    fun method(): PaymentMethod = PaymentMethod.fromString(paymentMethod)
}

data class CartEntry(
    val item: CatalogItem,
    val quantity: Int
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
