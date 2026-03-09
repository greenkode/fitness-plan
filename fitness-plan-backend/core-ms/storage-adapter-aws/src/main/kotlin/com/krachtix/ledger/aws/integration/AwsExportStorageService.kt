package com.krachtix.aws.integration

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.export.ExportFileReference
import com.krachtix.commons.export.ExportStorageGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class AwsExportStorageService(
    private val s3Client: AmazonS3,
    @Value("\${application.files.storage-provider.aws-s3.bucket:krachtix-exports}")
    private val bucket: String
) : ExportStorageGateway {

    override fun storeExportFile(merchantId: UUID, fileName: String, contentType: String, contents: ByteArray): ExportFileReference {
        val s3Key = "exports/$merchantId/$fileName"
        log.info { "Storing export file: bucket=$bucket, key=$s3Key, size=${contents.size}" }

        val metadata = ObjectMetadata().apply {
            this.contentType = contentType
            this.contentLength = contents.size.toLong()
            addUserMetadata("merchant-id", merchantId.toString())
        }

        ByteArrayInputStream(contents).use { inputStream ->
            s3Client.putObject(bucket, s3Key, inputStream, metadata)
        }

        return ExportFileReference(
            s3Key = s3Key,
            bucket = bucket,
            sizeBytes = contents.size.toLong()
        )
    }

    override fun generatePresignedDownloadUrl(s3Key: String, expirationMinutes: Long): String {
        val expiration = Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000)
        val url = s3Client.generatePresignedUrl(bucket, s3Key, expiration, HttpMethod.GET)
        log.debug { "Generated presigned URL for key=$s3Key, expires=$expiration" }
        return url.toString()
    }

    override fun deleteExportFile(s3Key: String) {
        log.info { "Deleting export file: bucket=$bucket, key=$s3Key" }
        s3Client.deleteObject(bucket, s3Key)
    }
}
