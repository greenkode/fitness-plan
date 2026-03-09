package com.krachtix.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
class DataSourceConfiguration {

    @Bean
    fun coreDataSource(
        @Value("\${spring.datasource.core.url}") url: String,
        @Value("\${spring.datasource.core.username}") username: String,
        @Value("\${spring.datasource.core.password}") password: String,
        @Value("\${spring.datasource.core.driver-class-name}") driverClassName: String,
        @Value("\${spring.datasource.core.hikari.maximum-pool-size}") maximumPoolSize: Int,
        @Value("\${spring.datasource.core.hikari.connectionTimeout}") connectionTimeout: Long,
        @Value("\${spring.datasource.core.hikari.idleTimeout}") idleTimeout: Long,
        @Value("\${spring.datasource.core.hikari.maxLifetime}") maxLifetime: Long,
        @Value("\${spring.datasource.core.hikari.register-mbeans}") registerMbeans: Boolean
    ): DataSource {
        log.info { "Creating core datasource: $url" }

        val config = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            this.driverClassName = driverClassName
            this.maximumPoolSize = maximumPoolSize
            this.schema = "core"
            poolName = "HikariPool-Core"
            this.connectionTimeout = connectionTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime
            isRegisterMbeans = registerMbeans
            connectionInitSql = "SELECT 1"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("reWriteBatchedInserts", "true")
        }

        return TenantAwareDataSource(HikariDataSource(config))
    }

    @Bean
    fun coreReadReplicaDataSource(
        @Value("\${spring.datasource.read-replica.url}") url: String,
        @Value("\${spring.datasource.read-replica.username}") username: String,
        @Value("\${spring.datasource.read-replica.password}") password: String,
        @Value("\${spring.datasource.read-replica.driver-class-name}") driverClassName: String,
        @Value("\${spring.datasource.read-replica.hikari.maximum-pool-size}") maximumPoolSize: Int,
        @Value("\${spring.datasource.read-replica.hikari.connectionTimeout}") connectionTimeout: Long,
        @Value("\${spring.datasource.read-replica.hikari.idleTimeout}") idleTimeout: Long,
        @Value("\${spring.datasource.read-replica.hikari.maxLifetime}") maxLifetime: Long,
        @Value("\${spring.datasource.read-replica.hikari.register-mbeans}") registerMbeans: Boolean
    ): DataSource {
        log.info { "Creating core read replica datasource: $url" }

        val config = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            this.driverClassName = driverClassName
            this.maximumPoolSize = maximumPoolSize
            this.schema = "core"
            poolName = "HikariPool-CoreReplica"
            this.connectionTimeout = connectionTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime
            isRegisterMbeans = registerMbeans
            isReadOnly = true
            connectionInitSql = "SELECT 1"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
        }

        return TenantAwareDataSource(HikariDataSource(config))
    }
}
