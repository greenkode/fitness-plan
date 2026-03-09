package com.krachtix.identity.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@TestConfiguration
class TestFlywayConfiguration {

    @Bean
    @Primary
    fun testFlyway(@Qualifier("primaryDataSource") primaryDataSource: DataSource): Flyway {
        val flyway = Flyway.configure()
            .dataSource(primaryDataSource)
            .locations(
                "classpath:db/migration",
                "classpath:db/sql/functions",
                "classpath:db/sql/views"
            )
            .schemas("identity")
            .createSchemas(true)
            .defaultSchema("identity")
            .cleanDisabled(false)
            .load()
        flyway.migrate()
        return flyway
    }
}
