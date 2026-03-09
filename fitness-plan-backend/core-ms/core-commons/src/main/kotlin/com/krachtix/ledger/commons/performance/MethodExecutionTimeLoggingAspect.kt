package com.krachtix.commons.performance

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import java.time.temporal.ChronoUnit

@Aspect
@Component
class MethodExecutionTimeLoggingAspect(private val meterRegistryService: MeterRegistryService) {

    private val log = logger {}

    @Around("@annotation(com.krachtix.commons.performance.LogExecutionTime)")
    @Throws(
        Throwable::class
    )
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.currentTimeMillis()

        val proceed = joinPoint.proceed()

        val executionTime = System.currentTimeMillis() - start

        meterRegistryService.buildCounterMetric(
            MetricName.EXECUTION_TIME.property,
            MetricTag.EXECUTION_NAME.name, joinPoint.signature.toLongString(),
            MetricTag.EXECUTION_TIME.name, executionTime.toString(),
            MetricTag.EXECUTION_TIME_UNIT.name, ChronoUnit.MILLIS.name
        )

        log.info{"Method [${joinPoint.signature.name}] took $executionTime millis"}

        return proceed
    }
}