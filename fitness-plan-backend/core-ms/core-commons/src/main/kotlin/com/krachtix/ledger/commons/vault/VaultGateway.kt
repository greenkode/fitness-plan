package com.krachtix.vault

interface VaultGateway {
    fun writeToVault(key: String, data: Map<String, String>)
    fun readFromVault(key: String): Map<String, String>?
}