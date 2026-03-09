package com.krachtix.aws.domain.util

import com.krachtix.storage.model.FileStorageProvider
import com.krachtix.storage.StorageGateway
import com.krachtix.aws.exception.UnsupportedFileStorageProvider
import org.springframework.stereotype.Component

@Component
class FileStorageUtil(val storageProviderServices: Set<StorageGateway>) {

    fun getProviderStorageService(fileStorageProvider: FileStorageProvider): StorageGateway {
        return storageProviderServices.find { it.supports(fileStorageProvider) }
            ?: throw UnsupportedFileStorageProvider(fileStorageProvider)
    }

}