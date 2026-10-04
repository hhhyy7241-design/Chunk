package com.example.model

data class ChunkPart(
    val index: Int,
    val url: String
)

data class MoodleManifest(
    val version: Int,
    val filename: String,
    val size: Long,
    val sha256: String?,
    val created: Long?,
    val parts: List<ChunkPart>,
    val rawCode: String
)

enum class DownloadState {
    IDLE,
    VALIDATING,
    PREPARING,
    DOWNLOADING_PART,
    REUSING_COMPLETED_PART,
    RECONSTRUCTING,
    VERIFYING_SIZE,
    VERIFYING_SHA256,
    COMPLETED,
    CANCELLED,
    RETRYING,
    FAILED
}
