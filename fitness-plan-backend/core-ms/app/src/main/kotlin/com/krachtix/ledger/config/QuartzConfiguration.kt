package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.scheduling.quartz.SpringBeanJobFactory
import java.util.Properties
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
@ConditionalOnProperty(name = ["quartz.jdbc.enabled"], havingValue = "true", matchIfMissing = true)
class QuartzConfiguration {

    @Bean
    fun schedulerFactoryBean(
        @Qualifier("coreDataSource") dataSource: DataSource,
        beanFactory: AutowireCapableBeanFactory
    ): SchedulerFactoryBean {
        log.info { "Configuring Quartz scheduler with core datasource (JDBC job store)" }

        val factory = SchedulerFactoryBean()
        factory.setJobFactory(AutowiringSpringBeanJobFactory(beanFactory))
        factory.setDataSource(dataSource)
        factory.setOverwriteExistingJobs(true)
        factory.setWaitForJobsToCompleteOnShutdown(true)

        val properties = Properties()
        properties["org.quartz.jobStore.driverDelegateClass"] = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"
        properties["org.quartz.jobStore.useProperties"] = "false"
        properties["org.quartz.jobStore.tablePrefix"] = "core.QRTZ_"
        properties["org.quartz.jobStore.isClustered"] = "false"
        properties["org.quartz.scheduler.instanceId"] = "AUTO"
        properties["org.quartz.threadPool.threadCount"] = "10"

        factory.setQuartzProperties(properties)

        return factory
    }
}

class AutowiringSpringBeanJobFactory(
    private val beanFactory: AutowireCapableBeanFactory
) : SpringBeanJobFactory() {

    override fun createJobInstance(bundle: TriggerFiredBundle): Any {
        val job = super.createJobInstance(bundle)
        beanFactory.autowireBean(job)
        return job
    }
}
