package com.krachtix.aws.exception

import com.krachtix.commons.exception.LedgerServiceException
import com.krachtix.storage.model.FileStorageProvider

class UnsupportedFileStorageProvider(fileStorageProvider: FileStorageProvider) :
    LedgerServiceException("Unsupported file storage provider $fileStorageProvider")