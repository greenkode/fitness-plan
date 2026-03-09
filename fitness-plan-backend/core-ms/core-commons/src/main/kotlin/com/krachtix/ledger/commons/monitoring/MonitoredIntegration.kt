package com.krachtix.monitoring

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MonitoredIntegration(
    val integration: String,
    val direction: IntegrationDirection = IntegrationDirection.OUTBOUND,
    val processReferenceExpression: String = "",
    val maxRequestSize: Int = 10000,
    val maxResponseSize: Int = 10000,
    val sensitiveFields: Array<String> = []
)
