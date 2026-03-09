package com.krachtix.storage.model

data class FileDetail(
    val bucket: String,
    val path: String,
    val fileContents: ByteArray,
    val fileType: String,
    val storageProvider: FileStorageProvider
)