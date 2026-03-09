package com.krachtix.config

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.CommandHandlers
import an.awesome.pipelinr.Notification
import an.awesome.pipelinr.NotificationHandlers
import an.awesome.pipelinr.Pipeline
import an.awesome.pipelinr.Pipelinr
import com.krachtix.commons.exception.TransactionProcessingException
import com.krachtix.commons.i18n.MessageService
import org.javamoney.moneta.Money
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.datatype.moneta.MonetaMoneyModule
import tools.jackson.module.kotlin.KotlinModule
import java.util.concurrent.ThreadPoolExecutor


@Configuration
class GstConfiguration(val messageService: MessageService) {

    @Bean
    fun pipeline(
        commandHandlers: ObjectProvider<Command.Handler<*, *>>,
        notificationHandlers: ObjectProvider<Notification.Handler<*>>,
        middlewares: ObjectProvider<Command.Middleware>
    ): Pipeline {
        return Pipelinr()
            .with(CommandHandlers { commandHandlers.stream() })
            .with(NotificationHandlers { notificationHandlers.stream() })
            .with(Command.Middlewares { middlewares.orderedStream() })
    }

    @Bean
    fun objectMapper(): JsonMapper {
        return JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .addModule(KotlinModule.Builder().build())
            .addModule(MonetaMoneyModule()
                .withMoney().withAmountFieldName("value").withCurrencyFieldName("currency"))
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
    }

    @Bean
    fun getAsyncExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 20
        executor.maxPoolSize = 40
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("threadPoolExecutor-")
        executor.setRejectedExecutionHandler { r: Runnable?, ex: ThreadPoolExecutor ->
            try {
                r?.let { ex.queue.put(it) }
            } catch (e: InterruptedException) {
                throw TransactionProcessingException("Unable to add runnable to a blocking queue")
            }
        }
        executor.initialize()
        return executor
    }

    @Bean
    fun taskExecutor(): DelegatingSecurityContextAsyncTaskExecutor {
        return DelegatingSecurityContextAsyncTaskExecutor(getAsyncExecutor())
    }
}