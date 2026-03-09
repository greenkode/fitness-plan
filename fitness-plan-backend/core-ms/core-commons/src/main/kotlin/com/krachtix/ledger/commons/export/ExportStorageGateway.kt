package com.krachtix.commons.export

import java.util.UUID

interface ExportStorageGateway {

    fun storeExportFile(merchantId: UUID, fileName: String, contentType: String, contents: ByteArray): ExportFileReference

    fun generatePresignedDownloadUrl(s3Key: String, expirationMinutes: Long): String

    fun deleteExportFile(s3Key: String)
}
