package com.krachtix.commons.performance

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.function.Supplier

@Service
class MeterRegistryService(private val meterRegistry: MeterRegistry) {

    fun buildCounterMetric(metricName: String, vararg tags: String) {
        Counter.builder(metricName).tags(*tags).register(meterRegistry).increment()
    }

    fun GaugeMetric(metricName: String, supplier: Supplier<Number>, vararg tags: String) {
        Gauge.builder(metricName, supplier).tags(*tags).register(meterRegistry)
    }

    fun recordTimer(metricName: String, duration: Duration, vararg tags: String) {
        Timer.builder(metricName)
            .tags(*tags)
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(meterRegistry)
            .record(duration)
    }
}
