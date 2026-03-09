package com.krachtix.storage

import com.krachtix.storage.model.FileDetail
import com.krachtix.storage.model.FileStorageProvider

interface StorageGateway {

    fun store(fileDetail: FileDetail)
    fun supports(fileStorageProvider: FileStorageProvider): Boolean

}
