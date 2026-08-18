package com.example.utils

import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.syncGlobalId(collection: String, localId: Long): String =
    getString("globalId")?.trim()?.ifEmpty { SyncIdentity.legacyGlobalId(collection, localId) }
        ?: SyncIdentity.legacyGlobalId(collection, localId)

fun DocumentSnapshot.syncMutationVersion(fallback: Long): Long =
    longValue("mutationVersion") ?: longValue("updatedAt") ?: fallback

fun DocumentSnapshot.syncMutationDeviceId(): String =
    getString("mutationDeviceId")?.trim()?.ifEmpty { SyncIdentity.LEGACY_DEVICE_ID }
        ?: SyncIdentity.LEGACY_DEVICE_ID

private fun DocumentSnapshot.longValue(field: String): Long? =
    getLong(field) ?: getDouble(field)?.toLong()
