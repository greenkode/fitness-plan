package com.krachtix.monitoring

import com.krachtix.commons.json.ObjectMapperFacade
import com.krachtix.commons.performance.MeterRegistryService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.ApplicationEventPublisher
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeoutException

@Aspect
@Component
class IntegrationMonitoringAspect(
    private val meterRegistryService: MeterRegistryService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val log = KotlinLogging.logger {}
    private val spelParser = SpelExpressionParser()

    @Around("@annotation(monitoredIntegration)")
    fun monitorIntegration(
        joinPoint: ProceedingJoinPoint,
        monitoredIntegration: MonitoredIntegration
    ): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = joinPoint.signature.name
        val integrationId = resolveIntegrationId(joinPoint, monitoredIntegration)

        val requestBody = captureRequest(joinPoint.args, monitoredIntegration)
        val processReference = extractProcessReference(joinPoint, monitoredIntegration)

        var status = IntegrationStatus.SUCCESS
        var responseBody: String? = null
        var errorMessage: String? = null
        var errorType: String? = null
        var result: Any? = null

        try {
            result = joinPoint.proceed()
            responseBody = captureResponse(result, monitoredIntegration)
        } catch (e: Exception) {
            status = determineStatus(e)
            errorMessage = e.message
            errorType = e.javaClass.simpleName
            log.error(e) { "Integration error in $integrationId.$methodName" }
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime

            recordMetrics(integrationId, methodName, status, errorType, duration)

            publishLogEvent(
                integrationId = integrationId,
                methodName = methodName,
                requestBody = requestBody,
                responseBody = responseBody,
                errorMessage = errorMessage,
                errorType = errorType,
                durationMs = duration,
                status = status,
                direction = monitoredIntegration.direction,
                processReference = processReference
            )
        }

        return result
    }

    private fun resolveIntegrationId(
        joinPoint: ProceedingJoinPoint,
        annotation: MonitoredIntegration
    ): String {
        if (annotation.integration.isNotEmpty()) {
            return annotation.integration
        }

        val target = joinPoint.target
        return try {
            val getIdMethod = target.javaClass.getMethod("getId")
            getIdMethod.invoke(target) as? String ?: target.javaClass.simpleName
        } catch (e: Exception) {
            target.javaClass.simpleName
        }
    }

    private fun captureRequest(args: Array<Any?>, annotation: MonitoredIntegration): String? {
        if (args.isEmpty()) return null

        return try {
            val json = ObjectMapperFacade.writeValueAsString(args)
            truncateAndMask(json, annotation.maxRequestSize, annotation.sensitiveFields)
        } catch (e: Exception) {
            log.warn { "Failed to serialize request: ${e.message}" }
            null
        }
    }

    private fun captureResponse(result: Any?, annotation: MonitoredIntegration): String? {
        if (result == null) return null

        return try {
            val json = ObjectMapperFacade.writeValueAsString(result)
            truncateAndMask(json, annotation.maxResponseSize, annotation.sensitiveFields)
        } catch (e: Exception) {
            log.warn { "Failed to serialize response: ${e.message}" }
            null
        }
    }

    private fun truncateAndMask(json: String, maxSize: Int, sensitiveFields: Array<String>): String {
        var result = json
        sensitiveFields.forEach { field ->
            result = result.replace(Regex("\"$field\"\\s*:\\s*\"[^\"]*\""), "\"$field\":\"***\"")
        }
        return if (result.length > maxSize) result.take(maxSize) + "...[TRUNCATED]" else result
    }

    private fun extractProcessReference(
        joinPoint: ProceedingJoinPoint,
        annotation: MonitoredIntegration
    ): UUID? {
        if (annotation.processReferenceExpression.isEmpty()) return null

        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        val context = StandardEvaluationContext()
        parameterNames.forEachIndexed { index, name ->
            context.setVariable(name, args[index])
        }

        return try {
            val expression = spelParser.parseExpression(annotation.processReferenceExpression)
            when (val value = expression.getValue(context)) {
                is UUID -> value
                is String -> UUID.fromString(value)
                else -> null
            }
        } catch (e: Exception) {
            log.warn { "Failed to extract process reference: ${e.message}" }
            null
        }
    }

    private fun determineStatus(e: Exception): IntegrationStatus {
        return when (e) {
            is TimeoutException, is SocketTimeoutException -> IntegrationStatus.TIMEOUT
            else -> IntegrationStatus.FAILURE
        }
    }

    private fun recordMetrics(
        integrationId: String,
        methodName: String,
        status: IntegrationStatus,
        errorType: String?,
        durationMs: Long
    ) {
        meterRegistryService.recordTimer(
            "integration.request.duration",
            Duration.ofMillis(durationMs),
            "integration_name", integrationId,
            "method_name", methodName,
            "status", status.name,
            "error_type", errorType ?: "none"
        )

        meterRegistryService.buildCounterMetric(
            "integration.request.total",
            "integration_name", integrationId,
            "method_name", methodName,
            "status", status.name,
            "error_type", errorType ?: "none"
        )
    }

    private fun publishLogEvent(
        integrationId: String,
        methodName: String,
        requestBody: String?,
        responseBody: String?,
        errorMessage: String?,
        errorType: String?,
        durationMs: Long,
        status: IntegrationStatus,
        direction: IntegrationDirection,
        processReference: UUID?
    ) {
        val event = IntegrationLogEvent(
            integrationId = integrationId,
            methodName = methodName,
            requestBody = requestBody,
            responseBody = responseBody,
            errorMessage = errorMessage,
            errorType = errorType,
            durationMs = durationMs,
            status = status,
            direction = direction,
            processReference = processReference
        )
        eventPublisher.publishEvent(event)
    }
}
