package com.krachtix.notification.job

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.notification.service.WebhookDeliveryService
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@DisallowConcurrentExecution
class WebhookDeliveryJob : QuartzJobBean() {

    @Autowired
    private lateinit var webhookDeliveryService: WebhookDeliveryService

    override fun executeInternal(context: JobExecutionContext) {

        val batchSize = context.mergedJobDataMap.getIntValue("batchSize").takeIf { it > 0 } ?: 50

        webhookDeliveryService.processDeliveryBatch(batchSize)
    }
}
