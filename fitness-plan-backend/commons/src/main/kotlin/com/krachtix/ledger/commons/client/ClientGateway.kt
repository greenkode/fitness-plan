package com.krachtix.commons.client

import java.util.UUID

interface ClientGateway {

    fun getLoggedInClientDetails(): ClientDetailsDto?

    fun getClientDetailsById(clientId: UUID):ClientDetailsDto?
}
