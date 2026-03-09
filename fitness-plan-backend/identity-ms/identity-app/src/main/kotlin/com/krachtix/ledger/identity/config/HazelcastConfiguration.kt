package com.krachtix.identity.config

import com.krachtix.commons.cache.SrCache
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.EvictionConfig
import com.hazelcast.config.EvictionPolicy
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MaxSizePolicy
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.cache.HazelcastCacheManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

private val log = KotlinLogging.logger {}

@Configuration
@ConditionalOnProperty(value = ["spring.cache.type"], havingValue = "hazelcast")
class HazelcastConfiguration(
    private val environment: Environment,
    @param:Value("\${spring.application.name:identity-ms}")
    private val applicationName: String,
    @param:Value("\${hazelcast.members}")
    private val clientAddress: String,
    @param:Value("\${hazelcast.cluster.name}")
    private val clusterName: String
) {

    @Bean
    fun hazelcastInstance(): HazelcastInstance {
        log.info { "Creating Hazelcast CLIENT '$applicationName' connecting to $clientAddress (cluster: $clusterName)" }

        val clientConfig = ClientConfig()
        clientConfig.instanceName = applicationName
        clientConfig.clusterName = clusterName

        clientConfig.networkConfig.addAddress(clientAddress)
        clientConfig.networkConfig.connectionTimeout =
            environment.getProperty("HAZELCAST_CLIENT_TIMEOUT", "10000").toInt()
        clientConfig.connectionStrategyConfig.connectionRetryConfig.clusterConnectTimeoutMillis =
            environment.getProperty("HAZELCAST_CLIENT_RETRY_PERIOD", "3000").toLong() *
            environment.getProperty("HAZELCAST_CLIENT_RETRY_LIMIT", "5").toInt()

        clientConfig.serializationConfig.isCheckClassDefErrors = false
        clientConfig.serializationConfig.isAllowUnsafe = true

        return HazelcastClient.newHazelcastClient(clientConfig)
    }

    @Bean
    fun cacheManager(hazelcastInstance: HazelcastInstance): CacheManager {
        return HazelcastCacheManager(hazelcastInstance)
    }
}
