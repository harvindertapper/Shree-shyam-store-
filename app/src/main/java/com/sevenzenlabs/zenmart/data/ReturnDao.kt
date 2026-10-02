package com.sevenzenlabs.zenmart.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(returnRecord: Return): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturnItems(items: List<ReturnItem>)

    @Query("SELECT * FROM returns WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllReturns(): Flow<List<Return>>

    @Query("SELECT * FROM returns WHERE id = :id LIMIT 1")
    suspend fun getReturnById(id: Long): Return?

    @Query("SELECT * FROM returns WHERE saleId = :saleId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getReturnsForSale(saleId: Long): Flow<List<Return>>

    @Query("SELECT * FROM returns WHERE saleId = :saleId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getReturnsForSaleList(saleId: Long): List<Return>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId AND isDeleted = 0")
    fun getReturnItemsForReturn(returnId: Long): Flow<List<ReturnItem>>

    @Query("SELECT * FROM return_items WHERE returnId = :returnId AND isDeleted = 0")
    suspend fun getReturnItemsForReturnList(returnId: Long): List<ReturnItem>

    @Query("SELECT * FROM return_items WHERE saleItemId = :saleItemId AND isDeleted = 0")
    suspend fun getReturnItemsForSaleItem(saleItemId: Long): List<ReturnItem>

    /**
     * Calculates the cumulative returned quantity for a specific sold line item across all return events.
     */
    @Query("SELECT COALESCE(SUM(quantityReturned), 0.0) FROM return_items WHERE saleItemId = :saleItemId AND isDeleted = 0")
    suspend fun getReturnedQuantityForSaleItem(saleItemId: Long): Double

    @Query("SELECT * FROM returns WHERE createdAt >= :start AND createdAt <= :end AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getReturnsForDateRange(start: Long, end: Long): Flow<List<Return>>

    @Query("SELECT COALESCE(SUM(totalRefundAmount), 0) FROM returns WHERE createdAt >= :start AND createdAt <= :end AND isDeleted = 0")
    fun getTotalRefundsForDateRange(start: Long, end: Long): Flow<Long>

    @Query("SELECT * FROM returns WHERE isSynced = 0")
    suspend fun getUnsyncedReturns(): List<Return>

    @Query("SELECT * FROM return_items WHERE isSynced = 0")
    suspend fun getUnsyncedReturnItems(): List<ReturnItem>

    @Query("UPDATE returns SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markReturnsSynced(ids: List<Long>)

    @Query("UPDATE return_items SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markReturnItemsSynced(ids: List<Long>)

    @Query("UPDATE returns SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markReturnSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Query("UPDATE return_items SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markReturnItemSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM returns WHERE globalId = :globalId LIMIT 1")
    suspend fun getReturnSyncStamp(globalId: String): SyncRecordStamp?

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM return_items WHERE globalId = :globalId LIMIT 1")
    suspend fun getReturnItemSyncStamp(globalId: String): SyncRecordStamp?

    @Query("DELETE FROM returns")
    suspend fun clearAllReturns()

    @Query("DELETE FROM return_items")
    suspend fun clearAllReturnItems()
}
