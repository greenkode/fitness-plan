package com.krachtix.aws.domain.command

import com.krachtix.storage.model.FileDetail
import com.krachtix.storage.model.FileStorageProvider
import com.krachtix.aws.domain.util.FileStorageUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class StoreFileCommandHandler(
    val fileStorageUtil: FileStorageUtil,
    @Value("\${application.files.storage-provider.default}")
    val defaultStorageProvider: FileStorageProvider
) {
    val log = KotlinLogging.logger { }
    fun handle(command: StoreFileCommand): FileDetail {

        log.debug { "Handling store file command: $command" }

        val provider = command.storageProvider ?: defaultStorageProvider
        val fileDetail = FileDetail(command.bucket, command.path, command.fileContents, command.fileType, provider)

        fileStorageUtil.getProviderStorageService(provider).store(fileDetail)

        return fileDetail
    }
}