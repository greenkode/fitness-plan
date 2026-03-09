package com.krachtix.core.commons.client

import java.util.UUID

interface ClientAuthContext {

    fun getLoggedInClientId(): UUID?

    fun getClientType(): String?

    fun getScopes(): Set<String>

    fun hasScope(scope: String): Boolean

    fun isServiceClient(): Boolean
}
