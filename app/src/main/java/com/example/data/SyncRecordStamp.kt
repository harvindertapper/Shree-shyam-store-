package com.example.data

data class SyncRecordStamp(
    val id: Long,
    val globalId: String,
    val mutationVersion: Long,
    val mutationDeviceId: String
)
