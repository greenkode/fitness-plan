package com.krachtix.identity.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
class FlywayConfiguration {

    @Bean
    fun flyway(
        @Qualifier("primaryDataSource") dataSource: DataSource,
        @Value("\${spring.flyway.schemas:identity}") schemas: Array<String>,
        @Value("\${spring.flyway.locations:classpath:db/migration}") locations: Array<String>
    ): Flyway {
        log.info { "Configuring Flyway with schemas: ${schemas.joinToString()}, locations: ${locations.joinToString()}" }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .schemas(*schemas)
            .locations(*locations)
            .createSchemas(true)
            .load()

        log.info { "Running Flyway migrations..." }
        val result = flyway.migrate()
        log.info { "Flyway migrations completed: ${result.migrationsExecuted} migrations executed" }

        return flyway
    }

    @Bean
    fun flywayDependencyPostProcessor(): BeanFactoryPostProcessor {
        return BeanFactoryPostProcessor { beanFactory: ConfigurableListableBeanFactory ->
            val beanNames = arrayOf("entityManagerFactory")
            for (beanName in beanNames) {
                if (beanFactory.containsBeanDefinition(beanName)) {
                    val definition = beanFactory.getBeanDefinition(beanName)
                    val existingDependencies = definition.dependsOn ?: emptyArray()
                    if (!existingDependencies.contains("flyway")) {
                        definition.setDependsOn(*existingDependencies, "flyway")
                        log.info { "Added flyway dependency to $beanName" }
                    }
                }
            }
        }
    }
}
