package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.TenantScope
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.utils.CloudRestorableSnapshot
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocalRecoveryPointStore
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreRecoveryCoordinator
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreSnapshotCodec
import com.aistudio.shreeshyamstore.pqwzkb.utils.RestoreSnapshotValidator
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotEmptyException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotEnvelope
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotIncompleteException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotIntegrityException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotMalformedException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotReferentialIntegrityException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotSchemaUnsupportedException
import com.aistudio.shreeshyamstore.pqwzkb.utils.SnapshotTenantMismatchException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class RestoreRecoveryEnvelopeTest {
    @Test
    fun emptySnapshotIsRejectedBeforeReplacement() {
        val envelope = SnapshotEnvelope.create(
            snapshot = emptySnapshot(),
            tenant = tenant()
        )

        assertThrows(SnapshotEmptyException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun missingTableIsRejectedAsIncomplete() {
        val envelope = SnapshotEnvelope.create(validSnapshot(), tenant())
            .copy(tableCounts = mapOf("categories" to 1))

        assertThrows(SnapshotIncompleteException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun wrongTenantIsRejectedBeforeRecordsAreReturned() {
        val envelope = SnapshotEnvelope.create(validSnapshot(), tenant())

        assertThrows(SnapshotTenantMismatchException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant(storeId = "other-store"))
        }
    }

    @Test
    fun checksumMismatchIsRejected() {
        val envelope = SnapshotEnvelope.create(validSnapshot(), tenant())
            .copy(checksumSha256 = "0".repeat(64))

        assertThrows(SnapshotIntegrityException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun invalidForeignReferenceIsRejected() {
        val invalid = validSnapshot().copy(
            saleItems = listOf(validSnapshot().saleItems.single().copy(productId = 999L))
        )
        val envelope = SnapshotEnvelope.create(invalid, tenant())

        assertThrows(SnapshotReferentialIntegrityException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun duplicateGlobalIdentityIsRejected() {
        val product = validSnapshot().products.single()
        val invalid = validSnapshot().copy(
            products = listOf(product, product.copy(id = 5L))
        )
        val envelope = SnapshotEnvelope.create(invalid, tenant())

        assertThrows(SnapshotIntegrityException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun unsupportedSchemaVersionIsRejected() {
        val envelope = SnapshotEnvelope.create(validSnapshot(), tenant())
            .copy(schemaVersion = 99)

        assertThrows(SnapshotSchemaUnsupportedException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun partialSnapshotIsRejected() {
        val envelope = SnapshotEnvelope.create(validSnapshot(), tenant())
            .copy(complete = false)

        assertThrows(SnapshotIncompleteException::class.java) {
            RestoreSnapshotValidator.validate(envelope, tenant())
        }
    }

    @Test
    fun malformedEnvelopeJsonIsRejected() {
        assertThrows(SnapshotMalformedException::class.java) {
            RestoreSnapshotCodec.decode("{not-json")
        }
    }

    @Test
    fun localRecoveryPointRoundTripsAtomically() {
        val directory = File.createTempFile("restore-recovery-test", "").apply {
            delete()
            mkdirs()
        }
        try {
            val envelope = SnapshotEnvelope.create(validSnapshot(), tenant())
            val store = LocalRecoveryPointStore(directory)
            store.save(envelope)

            val recovered = store.read()
            assertNotNull(recovered)
            assertEquals(envelope, recovered)

            store.clear()
            assertNull(store.read())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun replacementFailureRollsBackToPreviousRecoverySnapshot() {
        kotlinx.coroutines.runBlocking {
            val recoverySnapshot = validSnapshot()
            val targetSnapshot = validSnapshot().copy(
                categories = listOf(Category(id = 10L, globalId = "category-10", name = "Grocery"))
            )
            val recoveryEnvelope = SnapshotEnvelope.create(recoverySnapshot, tenant())
            val attempted = mutableListOf<CloudRestorableSnapshot>()
            val coordinator = RestoreRecoveryCoordinator { snapshot ->
                attempted += snapshot
                if (attempted.size == 1) throw IllegalStateException("simulated Room failure")
            }

            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking {
                    coordinator.replaceWithRollback(targetSnapshot, tenant(), recoveryEnvelope)
                }
            }

            assertEquals(listOf(targetSnapshot, recoverySnapshot), attempted)
        }
    }

    @Test
    fun emptyRecoveryPointCanBeValidatedForRollback() {
        val envelope = SnapshotEnvelope.create(emptySnapshot(), tenant())

        val restored = RestoreSnapshotValidator.validate(
            envelope = envelope,
            expectedTenant = tenant(),
            allowEmpty = true
        )

        assertEquals(emptySnapshot(), restored)
    }

    private fun validSnapshot() = CloudRestorableSnapshot(
        categories = listOf(
            Category(id = 1L, globalId = "category-1", name = "Dairy")
        ),
        products = listOf(
            Product(
                id = 2L,
                globalId = "product-1",
                name = "Milk",
                categoryId = 1L,
                mrp = 600L,
                sellingPrice = 550L,
                currentStock = 10.0
            )
        ),
        sales = listOf(
            Sale(
                id = 3L,
                globalId = "sale-1",
                billNumber = "B-1",
                totalAmount = 550L,
                paymentMode = "CASH",
                paymentState = PaymentState.RECEIVED.wireValue,
                receivedAmount = 550L
            )
        ),
        saleItems = listOf(
            SaleItem(
                id = 4L,
                globalId = "sale-item-1",
                saleId = 3L,
                productId = 2L,
                productNameSnapshot = "Milk",
                quantity = 1.0,
                unitPrice = 550L,
                lineTotal = 550L
            )
        ),
        customers = emptyList(),
        udhaarTransactions = emptyList(),
        stockAdjustments = emptyList()
    )

    private fun emptySnapshot() = CloudRestorableSnapshot(
        categories = emptyList(),
        products = emptyList(),
        sales = emptyList(),
        saleItems = emptyList(),
        customers = emptyList(),
        udhaarTransactions = emptyList(),
        stockAdjustments = emptyList()
    )

    private fun tenant(storeId: String = "store-1") = TenantScope(
        organizationId = "org-1",
        storeId = storeId,
        membershipId = "membership-1",
        deviceId = "device-1",
        appInstallationId = "installation-1"
    )
}
