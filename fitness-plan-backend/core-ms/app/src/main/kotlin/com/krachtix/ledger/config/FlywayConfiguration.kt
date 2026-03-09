package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
class FlywayConfiguration {

    @Bean(name = ["flywayCoreMigrator"], initMethod = "migrate")
    @DependsOn("coreDataSource")
    @ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true")
    fun flywayCoreMigrator(@Qualifier("coreDataSource") coreDataSource: DataSource): Flyway {
        log.info { "Running Flyway migrations for core schema" }
        return Flyway.configure()
            .dataSource(coreDataSource)
            .schemas("core")
            .locations(
                "classpath:db/migration",
                "classpath:db/seed",
                "classpath:db/sql/functions",
                "classpath:db/sql/procedures",
                "classpath:db/sql/views"
            )
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .placeholderReplacement(false)
            .load()
    }
}
