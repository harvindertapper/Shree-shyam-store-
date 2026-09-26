package com.sevenzenlabs.zenmart.data

data class SyncRecordStamp(
    val id: Long,
    val globalId: String,
    val mutationVersion: Long,
    val mutationDeviceId: String
)
