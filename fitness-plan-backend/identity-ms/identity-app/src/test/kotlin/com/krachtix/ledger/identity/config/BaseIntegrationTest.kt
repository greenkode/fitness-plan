package com.krachtix.identity.config

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@ActiveProfiles("test")
@Import(TestFlywayConfiguration::class, TestCacheConfiguration::class)
abstract class BaseIntegrationTest {

    companion object {
        private val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("krachtix_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.main.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.main.username") { postgresContainer.username }
            registry.add("spring.datasource.main.password") { postgresContainer.password }
            registry.add("spring.datasource.main.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.datasource.main.hikari.maximum-pool-size") { 5 }
            registry.add("spring.datasource.main.hikari.schema") { "identity" }
            registry.add("spring.datasource.main.hikari.pool-name") { "test-pool" }
            registry.add("spring.datasource.main.hikari.connectionTimeout") { 30000L }
            registry.add("spring.datasource.main.hikari.idleTimeout") { 600000L }
            registry.add("spring.datasource.main.hikari.maxLifetime") { 1800000L }
            registry.add("spring.datasource.main.hikari.register-mbeans") { false }

            registry.add("spring.datasource.read-replica.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.read-replica.username") { postgresContainer.username }
            registry.add("spring.datasource.read-replica.password") { postgresContainer.password }
            registry.add("spring.datasource.read-replica.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.datasource.read-replica.hikari.maximum-pool-size") { 5 }
            registry.add("spring.datasource.read-replica.hikari.schema") { "identity" }
            registry.add("spring.datasource.read-replica.hikari.pool-name") { "test-read-pool" }
            registry.add("spring.datasource.read-replica.hikari.connectionTimeout") { 30000L }
            registry.add("spring.datasource.read-replica.hikari.idleTimeout") { 600000L }
            registry.add("spring.datasource.read-replica.hikari.maxLifetime") { 1800000L }
            registry.add("spring.datasource.read-replica.hikari.register-mbeans") { false }

            registry.add("spring.flyway.url") { postgresContainer.jdbcUrl }
            registry.add("spring.flyway.user") { postgresContainer.username }
            registry.add("spring.flyway.password") { postgresContainer.password }
        }
    }

    @Autowired
    private lateinit var flyway: Flyway

    @BeforeEach
    fun cleanDatabase() {
        flyway.clean()
        flyway.migrate()
    }
}
