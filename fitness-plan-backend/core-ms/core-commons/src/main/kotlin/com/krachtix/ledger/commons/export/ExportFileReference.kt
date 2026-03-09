package com.krachtix.commons.export

data class ExportFileReference(
    val s3Key: String,
    val bucket: String,
    val sizeBytes: Long
)
