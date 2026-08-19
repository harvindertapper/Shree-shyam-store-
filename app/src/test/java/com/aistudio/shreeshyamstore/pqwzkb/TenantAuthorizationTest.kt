package com.aistudio.shreeshyamstore.pqwzkb

import android.content.Context
import androidx.room.Room
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PlatformActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.AppDatabase
import com.aistudio.shreeshyamstore.pqwzkb.data.Customer
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TenantAuthorizationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShopRepository

    private val tenant = TenantScope(
        organizationId = "org-1",
        storeId = "store-1",
        membershipId = "membership-1",
        deviceId = "device-1",
        appInstallationId = "install-1"
    )
    private val owner = PlatformActor("owner-1", "Owner", "OWNER", "device-1")
    private val cashier = PlatformActor("cashier-1", "Cashier", "CASHIER", "device-1")
    private var authenticatedActor = owner

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShopRepository(
            categoryDao = database.categoryDao(),
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            customerDao = database.customerDao(),
            udhaarDao = database.udhaarDao(),
            stockAdjustmentDao = database.stockAdjustmentDao(),
            userDao = database.userDao(),
            database = database,
            shopProfileDao = database.shopProfileDao(),
            authorizationContextProvider = { tenant to authenticatedActor }
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun wrongStoreCheckoutIsRejectedBeforeAnyWrite() = runBlocking {
        val productId = insertProduct(stock = 3.0)
        val sale = Sale(billNumber = "AUTH-STORE-1", totalAmount = 1_000L, paymentMode = "CASH")
        val item = SaleItem(
            saleId = 0L,
            productId = productId,
            productNameSnapshot = "Test Product",
            quantity = 1.0,
            unit = "pcs",
            unitPrice = 1_000L,
            lineTotal = 1_000L
        )

        expectIllegalArgument {
            repository.insertSaleWithItems(
                sale = sale,
                items = listOf(item),
                command = command(tenant = tenant.copy(storeId = "other-store"))
            )
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertEquals(3.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun wrongMembershipAndActorDeviceAreRejectedBeforeLedgerWrites() = runBlocking {
        val customerId = database.customerDao().insertCustomer(Customer(name = "Scope Customer"))

        expectIllegalArgument {
            repository.recordUdhaarPayment(
                customerId = customerId,
                amountMinorUnits = 500L,
                note = "scope mismatch",
                command = command(tenant = tenant.copy(membershipId = "other-membership"))
            )
        }
        assertTrue(database.udhaarDao().getTransactionsForCustomerList(customerId).isEmpty())

        expectIllegalArgument {
            repository.recordUdhaarPayment(
                customerId = customerId,
                amountMinorUnits = 500L,
                note = "device mismatch",
                command = command(actor = owner.copy(deviceId = "other-device"))
            )
        }
        assertTrue(database.udhaarDao().getTransactionsForCustomerList(customerId).isEmpty())
    }

    @Test
    fun staleCheckoutCommandIsRejectedWithoutChangingStock() = runBlocking {
        val productId = insertProduct(stock = 4.0)
        val sale = Sale(billNumber = "AUTH-STALE-1", totalAmount = 1_000L, paymentMode = "CASH")
        val item = SaleItem(
            saleId = 0L,
            productId = productId,
            productNameSnapshot = "Test Product",
            quantity = 1.0,
            unit = "pcs",
            unitPrice = 1_000L,
            lineTotal = 1_000L
        )

        expectIllegalArgument {
            repository.insertSaleWithItems(
                sale = sale,
                items = listOf(item),
                command = command(clientCreatedAt = System.currentTimeMillis() - 6 * 60 * 1000L)
            )
        }

        assertEquals(0, database.saleDao().countAllSales())
        assertEquals(4.0, database.productDao().getProductById(productId)!!.currentStock, 0.0)
    }

    @Test
    fun cashierMayRecordPaymentButCannotCorrectLedger() = runBlocking {
        authenticatedActor = cashier
        val customerId = database.customerDao().insertCustomer(Customer(name = "Cashier Customer"))
        repository.recordUdhaarPayment(
            customerId = customerId,
            amountMinorUnits = 700L,
            note = "cash received",
            command = command(actor = cashier)
        )
        val original = database.udhaarDao().getTransactionsForCustomerList(customerId).single()

        expectIllegalArgument {
            repository.correctUdhaarTransaction(
                customerId = customerId,
                eventId = original.eventId,
                correctedAmountMinorUnits = 600L,
                reason = "cashier correction",
                command = command(actor = cashier)
            )
        }

        assertEquals(1, database.udhaarDao().getTransactionsForCustomerList(customerId).size)
    }

    @Test
    fun cashierCatalogWriteIsRejectedWithoutChangingProduct() = runBlocking {
        val productId = insertProduct(stock = 2.0)
        authenticatedActor = cashier
        val existing = database.productDao().getProductById(productId)!!

        expectIllegalArgument {
            repository.updateProduct(
                product = existing.copy(name = "Unauthorized Rename"),
                command = command(actor = cashier)
            )
        }

        assertEquals("Test Product", database.productDao().getProductById(productId)!!.name)
    }

    private fun command(
        actor: PlatformActor = authenticatedActor,
        tenant: TenantScope = this.tenant,
        clientCreatedAt: Long = System.currentTimeMillis()
    ) = CommandMetadata(
        idempotencyKey = UUID.randomUUID().toString(),
        clientEventId = UUID.randomUUID().toString(),
        tenant = tenant,
        actor = actor,
        clientCreatedAt = clientCreatedAt
    )

    private suspend fun insertProduct(stock: Double): Long = database.productDao().insert(
        Product(
            name = "Test Product",
            categoryId = 1L,
            mrp = 1_000L,
            sellingPrice = 1_000L,
            currentStock = stock,
            trackStock = true
        )
    )

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected authorization rejection.
        }
    }
}
