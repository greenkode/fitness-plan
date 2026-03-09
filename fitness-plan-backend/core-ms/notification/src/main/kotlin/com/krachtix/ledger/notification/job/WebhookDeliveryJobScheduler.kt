package com.krachtix.notification.job

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.scheduler.ScheduleJobPayload
import com.krachtix.scheduler.SchedulerGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class WebhookDeliveryJobScheduler(
    private val schedulerGateway: SchedulerGateway,
    @param:Value("\${app.webhook-delivery.enabled:true}")
    private val deliveryEnabled: Boolean,
    @param:Value("\${app.webhook-delivery.interval-seconds:10}")
    private val intervalSeconds: Int,
    @param:Value("\${app.webhook-delivery.batch-size:50}")
    private val batchSize: Int
) {

    @EventListener(ApplicationReadyEvent::class)
    fun scheduleDeliveryJob() {
        if (!deliveryEnabled) {
            log.info { "Webhook delivery is disabled, skipping job scheduling" }
            return
        }

        runCatching { schedulerGateway.deleteJobById(JOB_REFERENCE, JOB_GROUP) }
            .onFailure { log.debug { "No existing webhook delivery job to delete" } }

        runCatching {
            val payload = ScheduleJobPayload(
                reference = JOB_REFERENCE,
                group = JOB_GROUP,
                description = "Process webhook deliveries (every $intervalSeconds seconds)",
                startAt = Instant.now().plusSeconds(10),
                jobType = WebhookDeliveryJob::class.java,
                data = mapOf("batchSize" to batchSize.toString()),
                repeatForever = true,
                repeatIntervalInSeconds = intervalSeconds,
            )

            schedulerGateway.scheduleJob(payload)
            log.info { "Scheduled webhook delivery job, interval: $intervalSeconds seconds, batchSize: $batchSize" }
        }.onFailure { e ->
            log.error(e) { "Failed to schedule webhook delivery job" }
        }
    }

    companion object {
        private const val JOB_REFERENCE = "webhook-delivery"
        private const val JOB_GROUP = "webhook-management"
    }
}
