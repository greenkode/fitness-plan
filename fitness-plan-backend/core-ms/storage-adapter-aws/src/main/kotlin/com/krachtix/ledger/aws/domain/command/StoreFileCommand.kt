package com.krachtix.aws.domain.command

import com.krachtix.storage.model.FileStorageProvider

class StoreFileCommand(
    val bucket: String,
    val path: String,
    val fileContents: ByteArray,
    val fileType: String,
    val storageProvider: FileStorageProvider? = null
)