package com.krachtix.scheduler

import org.quartz.JobExecutionContext

interface SchedulerGateway {

    fun scheduleJob(payload: ScheduleJobPayload)

    fun deleteJob(context: JobExecutionContext)

    fun deleteJobById(reference: String, group: String): Boolean

    fun rescheduleJob(context: JobExecutionContext, nextRetryDelay: Long, id: String)

    companion object {

        const val RETRY_COUNT = "retryCount"

        const val MAX_RETRY = "maxRetry"
    }
}