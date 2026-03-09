package com.krachtix.user.integration.identity

object CustomJwtClaims {

    const val EMAIL = "email"
    const val FIRST_NAME = "first_name"
    const val LAST_NAME = "last_name"
    const val BUSINESS_NAME = "business_name"
    const val COUNTRY = "country"
    const val TYPE = "type"
    const val LANGUAGE = "language"
    const val PHONE_NUMBER = "phone_number_verified"
    const val SELFIE = "selfie_verified"
    const val PROOF_OF_RESIDENCE = "proof_of_residence_verified"
    const val ID = "id_verified"
    const val VERIFICATION_STATUS = "verification_status"

    // Client/Service claims (from client_credentials flow)
    const val CLIENT_ID = "client_id"
    const val CLIENT_TYPE = "client_type"
    const val MERCHANT_ID = "merchant_id"
    const val SCOPE = "scope"
}

