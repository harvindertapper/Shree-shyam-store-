package com.harrylabs.shreeshyamstore.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class OwnerIdentity(
    val uid: String,
    val email: String,
    val displayName: String
)

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val activeShopId: String? = null,
    val createdAt: Timestamp? = null
)

data class ShopProfile(
    val shopId: String,
    val name: String,
    val ownerPhone: String,
    val ownerUid: String,
    val createdAt: Timestamp? = null
)

interface FirebaseOwnerRepository {
    fun getCurrentUser(): OwnerIdentity?
    suspend fun signInWithGoogle(idToken: String): Result<OwnerIdentity>
    suspend fun fetchUserProfile(uid: String): Result<UserProfile?>
    suspend fun fetchShopProfile(shopId: String): Result<ShopProfile?>
    suspend fun fetchShopDataSnapshot(shopId: String): Result<ShopDataSnapshot>
    suspend fun pushShopDataSnapshot(shopId: String, snapshot: ShopDataSnapshot): Result<Unit>
    suspend fun createShopAndProfileAtomically(
        uid: String,
        email: String,
        displayName: String,
        shopId: String,
        shopName: String,
        ownerPhone: String
    ): Result<Unit>
    suspend fun signOut()
}

class FirebaseOwnerRepositoryImpl : FirebaseOwnerRepository {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    override fun getCurrentUser(): OwnerIdentity? {
        val user = auth.currentUser ?: return null
        return OwnerIdentity(
            uid = user.uid,
            email = user.email ?: "",
            displayName = normalizedDisplayName(user.displayName)
        )
    }

    override suspend fun signInWithGoogle(idToken: String): Result<OwnerIdentity> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("Sign-in returned null user")
        OwnerIdentity(
            uid = user.uid,
            email = user.email ?: "",
            displayName = normalizedDisplayName(user.displayName)
        )
    }

    override suspend fun fetchUserProfile(uid: String): Result<UserProfile?> = runCatching {
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) return@runCatching null
        UserProfile(
            uid = doc.getString("uid") ?: uid,
            email = doc.getString("email") ?: "",
            displayName = doc.getString("displayName") ?: "",
            activeShopId = doc.getString("activeShopId"),
            createdAt = doc.getTimestamp("createdAt")
        )
    }

    override suspend fun fetchShopProfile(shopId: String): Result<ShopProfile?> = runCatching {
        val doc = firestore.collection("shops").document(shopId).get().await()
        if (!doc.exists()) return@runCatching null
        ShopProfile(
            shopId = doc.getString("shopId") ?: shopId,
            name = doc.getString("name") ?: "",
            ownerPhone = doc.getString("ownerPhone") ?: "",
            ownerUid = doc.getString("ownerUid") ?: "",
            createdAt = doc.getTimestamp("createdAt")
        )
    }

    override suspend fun fetchShopDataSnapshot(shopId: String): Result<ShopDataSnapshot> = runCatching {
        val shopRef = firestore.collection("shops").document(shopId)
        ShopDataSnapshot(
            categories = shopRef.collection("categories").get().await().documents.map { it.toCategory(shopId) },
            products = shopRef.collection("products").get().await().documents.map { it.toProduct(shopId) },
            customers = shopRef.collection("customers").get().await().documents.map { it.toCustomer(shopId) },
            udhaarTransactions = shopRef.collection("udhaarTransactions").get().await().documents.map { it.toUdhaarTransaction(shopId) },
            sales = shopRef.collection("sales").get().await().documents.map { it.toSale(shopId) },
            saleItems = shopRef.collection("saleItems").get().await().documents.map { it.toSaleItem(shopId) },
            stockAdjustments = shopRef.collection("stockAdjustments").get().await().documents.map { it.toStockAdjustment(shopId) }
        )
    }

    override suspend fun pushShopDataSnapshot(shopId: String, snapshot: ShopDataSnapshot): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val shopRef = firestore.collection("shops").document(shopId)
        val batch = firestore.batch()

        snapshot.categories.forEach { category ->
            batch.set(
                shopRef.collection("categories").document(category.localUuid),
                category.toFirestoreMap(shopId, uid)
            )
        }
        snapshot.products.forEach { product ->
            batch.set(
                shopRef.collection("products").document(product.localUuid),
                product.toFirestoreMap(shopId, uid)
            )
        }
        snapshot.customers.forEach { customer ->
            batch.set(
                shopRef.collection("customers").document(customer.localUuid),
                customer.toFirestoreMap(shopId, uid)
            )
        }
        snapshot.udhaarTransactions.forEach { transaction ->
            batch.set(
                shopRef.collection("udhaarTransactions").document(transaction.localUuid),
                transaction.toFirestoreMap(shopId, uid)
            )
        }
        snapshot.sales.forEach { sale ->
            batch.set(
                shopRef.collection("sales").document(sale.localUuid),
                sale.toFirestoreMap(shopId, uid)
            )
        }
        snapshot.saleItems.forEach { item ->
            batch.set(
                shopRef.collection("saleItems").document(item.localUuid),
                item.toFirestoreMap(shopId, uid)
            )
        }
        snapshot.stockAdjustments.forEach { adjustment ->
            batch.set(
                shopRef.collection("stockAdjustments").document(adjustment.localUuid),
                adjustment.toFirestoreMap(shopId, uid)
            )
        }

        batch.commit().await()
    }

    override suspend fun createShopAndProfileAtomically(
        uid: String,
        email: String,
        displayName: String,
        shopId: String,
        shopName: String,
        ownerPhone: String
    ): Result<Unit> = runCatching {
        val safeEmail = email.trim()
        val safeDisplayName = normalizedDisplayName(displayName)
        val safeShopName = shopName.trim()
        val safeOwnerPhone = ownerPhone.trim()

        require(safeEmail.isNotBlank()) { "Authenticated email is required." }
        require(safeShopName.isNotBlank()) { "Shop name is required." }
        require(safeOwnerPhone.length in 10..15 && safeOwnerPhone.all { it.isDigit() }) {
            "Owner phone must be 10 to 15 digits."
        }

        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("users").document(uid)
            val shopRef = firestore.collection("shops").document(shopId)
            val memberRef = firestore.collection("shops").document(shopId).collection("members").document(uid)

            val userDoc = transaction.get(userRef)
            if (userDoc.exists()) {
                val existingActiveShopId = userDoc.getString("activeShopId")
                if (!existingActiveShopId.isNullOrEmpty()) {
                    throw Exception("User already has an active shop profile: $existingActiveShopId")
                }
            }

            val userData = mapOf(
                "uid" to uid,
                "email" to safeEmail,
                "displayName" to safeDisplayName,
                "activeShopId" to shopId,
                "createdAt" to (userDoc.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())
            )
            transaction.set(userRef, userData)

            val shopData = mapOf(
                "shopId" to shopId,
                "name" to safeShopName,
                "ownerPhone" to safeOwnerPhone,
                "ownerUid" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(shopRef, shopData)

            val memberData = mapOf(
                "uid" to uid,
                "shopId" to shopId,
                "role" to "owner",
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp()
            )
            transaction.set(memberRef, memberData)
        }.await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun normalizedDisplayName(displayName: String?): String {
        return displayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Owner"
    }

    private fun DocumentSnapshot.toCategory(shopId: String): Category {
        return Category(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            name = getString("name") ?: "",
            isActive = getBoolean("isActive") ?: true,
            createdAt = long("createdAt"),
            updatedAt = long("updatedAt")
        )
    }

    private fun DocumentSnapshot.toProduct(shopId: String): Product {
        return Product(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            name = getString("name") ?: "",
            categoryId = getString("categoryId") ?: "",
            mrp = double("mrp"),
            sellingPrice = nullableDouble("sellingPrice"),
            purchasePrice = nullableDouble("purchasePrice"),
            unitType = getString("unitType") ?: DataUnitType.PIECE,
            displayUnit = getString("displayUnit") ?: DataDisplayUnit.PIECE,
            baseUnit = getString("baseUnit") ?: DataDisplayUnit.PIECE,
            allowsDecimalQuantity = getBoolean("allowsDecimalQuantity") ?: false,
            quantityScale = long("quantityScale").toInt(),
            pricePerUnitPaise = long("pricePerUnitPaise"),
            priceUnitBaseQty = long("priceUnitBaseQty", 1L),
            purchasePricePerUnitPaise = nullableLong("purchasePricePerUnitPaise"),
            purchasePriceUnitBaseQty = nullableLong("purchasePriceUnitBaseQty"),
            currentStock = long("currentStock").toInt(),
            stockQuantityBase = long("stockQuantityBase"),
            trackStock = getBoolean("trackStock") ?: true,
            lowStockAlertQty = long("lowStockAlertQty", 5L).toInt(),
            lowStockAlertBase = long("lowStockAlertBase", 5L),
            isActive = getBoolean("isActive") ?: true,
            createdAt = long("createdAt"),
            updatedAt = long("updatedAt")
        )
    }

    private fun DocumentSnapshot.toCustomer(shopId: String): Customer {
        return Customer(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            name = getString("name") ?: "",
            phone = getString("phone"),
            isActive = getBoolean("isActive") ?: true,
            createdAt = long("createdAt"),
            updatedAt = long("updatedAt")
        )
    }

    private fun DocumentSnapshot.toUdhaarTransaction(shopId: String): UdhaarTransaction {
        return UdhaarTransaction(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            customerId = getString("customerId") ?: "",
            saleId = getString("saleId"),
            type = getString("type") ?: "CREDIT",
            amount = double("amount"),
            amountPaise = long("amountPaise"),
            note = getString("note"),
            createdAt = long("createdAt")
        )
    }

    private fun DocumentSnapshot.toSale(shopId: String): Sale {
        return Sale(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            idempotencyKey = getString("idempotencyKey") ?: id,
            billNumber = getString("billNumber") ?: id,
            totalAmount = double("totalAmount"),
            totalAmountPaise = long("totalAmountPaise"),
            paymentMode = getString("paymentMode") ?: "CASH",
            saleStatus = getString("saleStatus") ?: SaleStatus.COMPLETED,
            customerId = getString("customerId"),
            note = getString("note"),
            createdAt = long("createdAt")
        )
    }

    private fun DocumentSnapshot.toSaleItem(shopId: String): SaleItem {
        return SaleItem(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            saleId = getString("saleId") ?: "",
            productId = getString("productId") ?: "",
            productNameSnapshot = getString("productNameSnapshot") ?: "",
            quantity = long("quantity").toInt(),
            unitTypeSnapshot = getString("unitTypeSnapshot") ?: DataUnitType.PIECE,
            displayUnitSnapshot = getString("displayUnitSnapshot") ?: DataDisplayUnit.PIECE,
            baseUnitSnapshot = getString("baseUnitSnapshot") ?: DataDisplayUnit.PIECE,
            enteredQuantityText = getString("enteredQuantityText") ?: long("quantity").toString(),
            quantityBase = long("quantityBase"),
            unitPrice = double("unitPrice"),
            originalPricePerUnitPaise = long("originalPricePerUnitPaise"),
            originalPriceUnitBaseQty = long("originalPriceUnitBaseQty", 1L),
            effectivePricePerUnitPaise = long("effectivePricePerUnitPaise"),
            effectivePriceUnitBaseQty = long("effectivePriceUnitBaseQty", 1L),
            rateOverridden = getBoolean("rateOverridden") ?: false,
            lineTotal = double("lineTotal"),
            lineTotalPaise = long("lineTotalPaise"),
            purchasePricePerUnitPaiseSnapshot = nullableLong("purchasePricePerUnitPaiseSnapshot"),
            purchasePriceUnitBaseQtySnapshot = nullableLong("purchasePriceUnitBaseQtySnapshot")
        )
    }

    private fun DocumentSnapshot.toStockAdjustment(shopId: String): StockAdjustment {
        return StockAdjustment(
            localUuid = id,
            remoteId = id,
            shopId = shopId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = System.currentTimeMillis(),
            createdByUid = getString("createdByUid"),
            updatedByUid = getString("updatedByUid"),
            sourceDeviceId = getString("sourceDeviceId"),
            productId = getString("productId") ?: "",
            oldStock = long("oldStock").toInt(),
            oldQuantityBase = long("oldQuantityBase"),
            newStock = long("newStock").toInt(),
            newQuantityBase = long("newQuantityBase"),
            difference = long("difference").toInt(),
            differenceBase = long("differenceBase"),
            displayUnitSnapshot = getString("displayUnitSnapshot") ?: DataDisplayUnit.PIECE,
            reason = getString("reason") ?: "",
            createdAt = long("createdAt")
        )
    }

    private fun Category.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "name" to name,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun Product.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "name" to name,
        "categoryId" to categoryId,
        "mrp" to mrp,
        "sellingPrice" to sellingPrice,
        "purchasePrice" to purchasePrice,
        "unitType" to unitType,
        "displayUnit" to displayUnit,
        "baseUnit" to baseUnit,
        "allowsDecimalQuantity" to allowsDecimalQuantity,
        "quantityScale" to quantityScale,
        "pricePerUnitPaise" to pricePerUnitPaise,
        "priceUnitBaseQty" to priceUnitBaseQty,
        "purchasePricePerUnitPaise" to purchasePricePerUnitPaise,
        "purchasePriceUnitBaseQty" to purchasePriceUnitBaseQty,
        "currentStock" to currentStock,
        "stockQuantityBase" to stockQuantityBase,
        "trackStock" to trackStock,
        "lowStockAlertQty" to lowStockAlertQty,
        "lowStockAlertBase" to lowStockAlertBase,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun Customer.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "name" to name,
        "phone" to phone,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun UdhaarTransaction.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "customerId" to customerId,
        "saleId" to saleId,
        "type" to type,
        "amount" to amount,
        "amountPaise" to amountPaise,
        "note" to note,
        "createdAt" to createdAt
    )

    private fun Sale.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "idempotencyKey" to idempotencyKey,
        "billNumber" to billNumber,
        "totalAmount" to totalAmount,
        "totalAmountPaise" to totalAmountPaise,
        "paymentMode" to paymentMode,
        "saleStatus" to saleStatus,
        "customerId" to customerId,
        "note" to note,
        "createdAt" to createdAt
    )

    private fun SaleItem.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "saleId" to saleId,
        "productId" to productId,
        "productNameSnapshot" to productNameSnapshot,
        "quantity" to quantity,
        "unitTypeSnapshot" to unitTypeSnapshot,
        "displayUnitSnapshot" to displayUnitSnapshot,
        "baseUnitSnapshot" to baseUnitSnapshot,
        "enteredQuantityText" to enteredQuantityText,
        "quantityBase" to quantityBase,
        "unitPrice" to unitPrice,
        "originalPricePerUnitPaise" to originalPricePerUnitPaise,
        "originalPriceUnitBaseQty" to originalPriceUnitBaseQty,
        "effectivePricePerUnitPaise" to effectivePricePerUnitPaise,
        "effectivePriceUnitBaseQty" to effectivePriceUnitBaseQty,
        "rateOverridden" to rateOverridden,
        "lineTotal" to lineTotal,
        "lineTotalPaise" to lineTotalPaise,
        "purchasePricePerUnitPaiseSnapshot" to purchasePricePerUnitPaiseSnapshot,
        "purchasePriceUnitBaseQtySnapshot" to purchasePriceUnitBaseQtySnapshot
    )

    private fun StockAdjustment.toFirestoreMap(shopId: String, uid: String): Map<String, Any?> = mapOf(
        "localUuid" to localUuid,
        "shopId" to shopId,
        "syncStatus" to SyncStatus.SYNCED,
        "createdByUid" to (createdByUid ?: uid),
        "updatedByUid" to uid,
        "productId" to productId,
        "oldStock" to oldStock,
        "oldQuantityBase" to oldQuantityBase,
        "newStock" to newStock,
        "newQuantityBase" to newQuantityBase,
        "difference" to difference,
        "differenceBase" to differenceBase,
        "displayUnitSnapshot" to displayUnitSnapshot,
        "reason" to reason,
        "createdAt" to createdAt
    )

    private fun DocumentSnapshot.long(field: String, fallback: Long = 0L): Long {
        return (get(field) as? Number)?.toLong() ?: fallback
    }

    private fun DocumentSnapshot.nullableLong(field: String): Long? {
        return (get(field) as? Number)?.toLong()
    }

    private fun DocumentSnapshot.double(field: String, fallback: Double = 0.0): Double {
        return (get(field) as? Number)?.toDouble() ?: fallback
    }

    private fun DocumentSnapshot.nullableDouble(field: String): Double? {
        return (get(field) as? Number)?.toDouble()
    }
}
