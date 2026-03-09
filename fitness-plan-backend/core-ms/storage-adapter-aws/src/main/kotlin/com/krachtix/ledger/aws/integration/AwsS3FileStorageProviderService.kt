package com.krachtix.aws.integration

import com.krachtix.storage.model.FileDetail
import com.krachtix.storage.model.FileStorageProvider
import com.krachtix.storage.model.FileStorageProvider.AWS_S3
import com.krachtix.storage.StorageGateway
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream


@Component
class AwsS3FileStorageProviderService(private val s3Client: AmazonS3) :
    StorageGateway {

    val log = KotlinLogging.logger { }
    override fun store(fileDetail: FileDetail) {

        log.debug { "Uploading file to S3: $fileDetail" }

        val metadata = ObjectMetadata().apply {
            contentLength = fileDetail.fileContents.size.toLong()
            contentType = fileDetail.fileType
        }
        val putObjectRequest = PutObjectRequest(
            fileDetail.bucket,
            fileDetail.path,
            ByteArrayInputStream(fileDetail.fileContents),
            metadata
        )
        s3Client.putObject(
            putObjectRequest
        )
    }

    override fun supports(fileStorageProvider: FileStorageProvider): Boolean {
        return fileStorageProvider == AWS_S3
    }
}