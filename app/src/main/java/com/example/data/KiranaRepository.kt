package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CatalogItem
import com.example.model.Order
import com.example.model.OrderLineItem
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.ShopConfig
import com.example.model.StaffMember
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class KiranaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    companion object {
        @Volatile
        private var configured = false

        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val dbId = appContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(dbId)
            if (!configured) {
                synchronized(this) {
                    if (!configured) {
                        runCatching {
                            val settings = FirebaseFirestoreSettings.Builder()
                                .setLocalCacheSettings(
                                    PersistentCacheSettings.newBuilder()
                                        .setSizeBytes(50L * 1024L * 1024L)
                                        .build()
                                )
                                .build()
                            instance.firestoreSettings = settings
                        }
                        configured = true
                    }
                }
            }
            return instance
        }
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required")

    private fun normalizedEmail(): String =
        (auth.currentUser?.email ?: "").trim().lowercase(Locale.US)

    private fun nonNullMap(vararg pairs: Pair<String, Any?>): Map<String, Any> =
        buildMap {
            for ((key, value) in pairs) {
                if (value != null) put(key, value)
            }
        }

    // --- ROLE RESOLUTION ---
    suspend fun resolveCurrentUserRole(): UserRole {
        val email = normalizedEmail()
        if (email.isBlank()) return UserRole.CUSTOMER
        if (email == StarterCatalog.PRIMARY_ADMIN_EMAIL) return UserRole.ADMIN
        return try {
            val doc = db.collection("staff").document(email).get().await()
            if (doc.exists()) UserRole.fromString(doc.getString("role")) else UserRole.CUSTOMER
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "staff/$email")
            UserRole.CUSTOMER
        }
    }

    // --- SHOP CONFIG ---
    fun observeShopConfig(): Flow<ShopConfig> = flow {
        requireUserId()
        val ref = db.collection("config").document("shop")
        emitAll(
            ref.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(ShopConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: StarterCatalog.defaultShopConfig
                    } else {
                        StarterCatalog.defaultShopConfig
                    }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.GET, "config/shop")
                    throw error
                }
        )
    }

    suspend fun saveShopConfig(config: ShopConfig): Result<Unit> {
        val path = "config/shop"
        return try {
            requireUserId()
            val payload = nonNullMap(
                "shopName" to config.shopName.trim().take(80),
                "bannerText" to config.bannerText.trim().take(160),
                "bannerTextBn" to config.bannerTextBn.trim().take(160),
                "locationText" to config.locationText.trim().take(200),
                "latitude" to config.latitude,
                "longitude" to config.longitude,
                "openingHours" to config.openingHours.trim().take(80),
                "deliveryCharge" to config.deliveryCharge.coerceIn(0, 500),
                "minimumOrderAmount" to config.minimumOrderAmount.coerceIn(0, 5000),
                "deliveryMinutes" to config.deliveryMinutes.coerceIn(5, 180),
                "deliveryTimeText" to config.deliveryTimeText.trim().take(80),
                "deliveryTimeTextBn" to config.deliveryTimeTextBn.trim().take(80),
                "upiId" to config.upiId.trim().take(100),
                "upiQrBase64" to config.upiQrBase64,
                "shopPhotoBase64" to config.shopPhotoBase64,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("config").document("shop").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN SEEDING ON FIRST LAUNCH ---
    suspend fun ensureSeededForAdmin() {
        try {
            val email = normalizedEmail()
            if (email == StarterCatalog.PRIMARY_ADMIN_EMAIL) {
                val staffRef = db.collection("staff").document(email)
                if (!staffRef.get().await().exists()) {
                    staffRef.set(
                        mapOf(
                            "email" to email,
                            "role" to UserRole.ADMIN.name,
                            "name" to (auth.currentUser?.displayName ?: "Shop Owner"),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            val configRef = db.collection("config").document("shop")
            if (!configRef.get().await().exists()) {
                saveShopConfig(StarterCatalog.defaultShopConfig)
            }
            val existingItems = db.collection("items").limit(1).get().await()
            if (existingItems.isEmpty) {
                val batch = db.batch()
                StarterCatalog.items.forEach { item ->
                    val ref = db.collection("items").document(item.id)
                    val payload = nonNullMap(
                        "nameEn" to item.nameEn,
                        "nameBn" to item.nameBn,
                        "category" to item.category,
                        "price" to item.price,
                        "unit" to item.unit,
                        "available" to item.available,
                        "imageKey" to item.imageKey,
                        "similarItemIds" to item.similarItemIds,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(ref, payload)
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed")
        }
    }

    // --- CATALOG ITEMS ---
    fun observeCatalogItems(): Flow<List<CatalogItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("items").snapshots()
                .map { snap ->
                    val docs = snap.toObjects(CatalogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isEmpty()) StarterCatalog.items else docs.sortedBy { it.nameEn.lowercase(Locale.US) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "items")
                    throw error
                }
        )
    }

    suspend fun saveCatalogItem(item: CatalogItem): Result<Unit> {
        val isNew = item.id.isBlank()
        val docRef = if (isNew) db.collection("items").document() else db.collection("items").document(item.id)
        return try {
            requireUserId()
            if (isNew) {
                val payload = nonNullMap(
                    "nameEn" to item.nameEn.trim().take(100),
                    "nameBn" to item.nameBn.trim().take(100),
                    "category" to item.category,
                    "price" to item.price.coerceIn(1, 100000),
                    "unit" to item.unit.trim().take(40),
                    "available" to item.available,
                    "imageKey" to item.imageKey,
                    "imageBase64" to item.imageBase64,
                    "similarItemIds" to item.similarItemIds.take(5),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "nameEn" to item.nameEn.trim().take(100),
                    "nameBn" to item.nameBn.trim().take(100),
                    "category" to item.category,
                    "price" to item.price.coerceIn(1, 100000),
                    "unit" to item.unit.trim().take(40),
                    "available" to item.available,
                    "similarItemIds" to item.similarItemIds.take(5),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!item.imageKey.isNullOrBlank()) updates["imageKey"] = item.imageKey
                if (!item.imageBase64.isNullOrBlank()) updates["imageBase64"] = item.imageBase64
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleItemAvailability(itemId: String, available: Boolean): Result<Unit> {
        val path = "items/$itemId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("available" to available, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCatalogItem(itemId: String): Result<Unit> {
        val path = "items/$itemId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- CUSTOMER PROFILE ---
    fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
        requireUserId()
        val ref = db.collection("users").document(uid)
        emitAll(
            ref.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, ref.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCustomerOnboardingProfile(
        name: String,
        mobile: String,
        address: String,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val email = normalizedEmail().ifBlank { "user@gmail.com" }
        val ref = db.collection("users").document(uid)
        return try {
            val existing = ref.get().await()
            if (!existing.exists()) {
                val payload = nonNullMap(
                    "uid" to uid,
                    "email" to email,
                    "name" to name.trim().take(80),
                    "mobile" to mobile.trim().take(20),
                    "address" to address.trim().take(300),
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "rating" to 5,
                    "successfulDeliveredCount" to 0,
                    "cashEnabled" to true,
                    "cashAdminOverride" to false,
                    "profileCompleted" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "name" to name.trim().take(80),
                    "mobile" to mobile.trim().take(20),
                    "address" to address.trim().take(300),
                    "profileCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (latitude != null) updates["latitude"] = latitude
                if (longitude != null) updates["longitude"] = longitude
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    fun observeAllCustomers(): Flow<List<UserProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase(Locale.US) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    suspend fun adminUpdateCustomerTrust(
        customerId: String,
        newRating: Int,
        cashEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$customerId"
        return try {
            requireUserId()
            val clampedRating = newRating.coerceIn(1, 5)
            val finalCash = if (clampedRating <= 1) false else cashEnabled
            db.document(path).update(
                mapOf(
                    "rating" to clampedRating,
                    "cashEnabled" to finalCash,
                    "cashAdminOverride" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ORDERS ---
    fun observeMyOrders(uid: String): Flow<List<Order>> = flow {
        requireUserId()
        emitAll(
            db.collection("orders")
                .whereEqualTo("customerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "orders")
                    }
                    throw error
                }
        )
    }

    fun observeAllOrders(): Flow<List<Order>> = flow {
        requireUserId()
        emitAll(
            db.collection("orders").snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "orders")
                    throw error
                }
        )
    }

    suspend fun placeOrder(
        profile: UserProfile,
        items: List<OrderLineItem>,
        itemsTotal: Int,
        deliveryCharge: Int,
        paymentMethod: PaymentMethod,
        upiTransactionId: String?,
        paymentScreenshotBase64: String?
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("orders").document()
        return try {
            require(items.isNotEmpty()) { "Cart is empty" }
            if (paymentMethod == PaymentMethod.CASH) {
                require(profile.canPayCash()) { "Cash on delivery is disabled for this account" }
            } else {
                require(!upiTransactionId.isNullOrBlank() && upiTransactionId.trim().length >= 4) {
                    "Enter a valid UPI transaction number"
                }
            }

            val initialStatus = if (paymentMethod == PaymentMethod.UPI) {
                OrderStatus.WAITING_PAYMENT_CONFIRMATION.name
            } else {
                OrderStatus.PLACED.name
            }

            val lineMaps = items.map { line ->
                mapOf(
                    "itemId" to line.itemId,
                    "nameEn" to line.nameEn,
                    "nameBn" to line.nameBn,
                    "unit" to line.unit,
                    "price" to line.price,
                    "quantity" to line.quantity
                )
            }

            val payload = nonNullMap(
                "customerId" to uid,
                "customerName" to profile.name.trim().take(80),
                "customerMobile" to profile.mobile.trim().take(20),
                "customerAddress" to profile.address.trim().take(300),
                "customerLat" to profile.latitude,
                "customerLng" to profile.longitude,
                "items" to lineMaps,
                "itemsTotal" to itemsTotal,
                "deliveryCharge" to deliveryCharge,
                "grandTotal" to (itemsTotal + deliveryCharge),
                "paymentMethod" to paymentMethod.name,
                "upiTransactionId" to upiTransactionId?.trim()?.take(80),
                "paymentScreenshotBase64" to paymentScreenshotBase64,
                "status" to initialStatus,
                "cashReceived" to false,
                "adminAcknowledged" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun customerCancelOrder(order: Order, profile: UserProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val orderRef = db.collection("orders").document(order.id)
        val userRef = db.collection("users").document(uid)
        return try {
            val newRating = (profile.rating - 1).coerceAtLeast(1)
            val newCashEnabled = if (newRating <= 1) false else profile.cashEnabled
            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "rating" to newRating,
                    "cashEnabled" to newCashEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun packerMarkReady(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to OrderStatus.READY.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminAcknowledgeOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "adminAcknowledged" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminAcknowledgeAllActive(): Result<Unit> {
        return try {
            requireUserId()
            val pending = db.collection("orders")
                .whereEqualTo("adminAcknowledged", false)
                .get()
                .await()
            if (!pending.isEmpty) {
                val batch = db.batch()
                pending.documents.forEach { doc ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "adminAcknowledged" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun adminConfirmUpiAndMoveToPacking(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to OrderStatus.PACKING.name,
                    "adminAcknowledged" to true,
                    "confirmedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminAcceptCashAndMoveToPacking(orderId: String): Result<Unit> =
        adminConfirmUpiAndMoveToPacking(orderId)

    suspend fun adminUpdateOrderStatus(order: Order, nextStatus: OrderStatus, cashReceived: Boolean? = null): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to nextStatus.name,
                "adminAcknowledged" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (order.confirmedAt == null && nextStatus != OrderStatus.CANCELLED && nextStatus != OrderStatus.WAITING_PAYMENT_CONFIRMATION) {
                updates["confirmedAt"] = FieldValue.serverTimestamp()
            }
            if (cashReceived != null) updates["cashReceived"] = cashReceived

            if (nextStatus == OrderStatus.DELIVERED && order.orderStatus() != OrderStatus.DELIVERED) {
                val userRef = db.collection("users").document(order.customerId)
                val userSnap = userRef.get().await()
                val batch = db.batch()
                if (order.method() == PaymentMethod.CASH) {
                    updates["cashReceived"] = true
                }
                batch.update(db.document(path), updates)

                if (userSnap.exists()) {
                    val user = userSnap.toObject(UserProfile::class.java)
                    if (user != null) {
                        val newCount = user.successfulDeliveredCount + 1
                        var newRating = user.rating
                        if (newCount % 10 == 0 && newRating < 5) {
                            newRating = (newRating + 1).coerceAtMost(5)
                        }
                        val newCash = if (!user.cashAdminOverride) {
                            newRating >= 2
                        } else {
                            user.cashEnabled
                        }
                        batch.update(
                            userRef,
                            mapOf(
                                "successfulDeliveredCount" to newCount,
                                "rating" to newRating,
                                "cashEnabled" to newCash,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                batch.commit().await()
            } else {
                db.document(path).update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminCancelOrRefuseOrder(order: Order): Result<Unit> {
        val orderRef = db.collection("orders").document(order.id)
        val userRef = db.collection("users").document(order.customerId)
        return try {
            requireUserId()
            val userSnap = userRef.get().await()
            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "adminAcknowledged" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (userSnap.exists() && order.orderStatus() != OrderStatus.CANCELLED) {
                val user = userSnap.toObject(UserProfile::class.java)
                if (user != null) {
                    val newRating = (user.rating - 1).coerceAtLeast(1)
                    val newCash = if (newRating <= 1) false else user.cashEnabled
                    batch.update(
                        userRef,
                        mapOf(
                            "rating" to newRating,
                            "cashEnabled" to newCash,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    // --- STAFF MANAGEMENT ---
    fun observeStaff(): Flow<List<StaffMember>> = flow {
        requireUserId()
        emitAll(
            db.collection("staff").snapshots()
                .map { snap ->
                    snap.toObjects(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy({ it.role }, { it.email }))
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "staff")
                    throw error
                }
        )
    }

    suspend fun saveStaffMember(email: String, role: UserRole, name: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase(Locale.US)
        val path = "staff/$cleanEmail"
        return try {
            requireUserId()
            require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "Enter a valid Gmail address" }
            val ref = db.collection("staff").document(cleanEmail)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "role" to role.name,
                        "name" to name.trim().take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "email" to cleanEmail,
                        "role" to role.name,
                        "name" to name.trim().take(80),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeStaffMember(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase(Locale.US)
        if (cleanEmail == StarterCatalog.PRIMARY_ADMIN_EMAIL) {
            return Result.failure(IllegalArgumentException("Primary owner Gmail cannot be removed"))
        }
        val path = "staff/$cleanEmail"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
