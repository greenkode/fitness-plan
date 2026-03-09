package com.krachtix.transaction

interface TransactionReferenceGateway {

    fun generateReference(transactionTypeName: String, includeDelimiters: Boolean = false): String
}
