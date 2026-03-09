package com.krachtix.commons.security

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireSignedRequest(
    val description: String = "This endpoint requires request signing"
)
