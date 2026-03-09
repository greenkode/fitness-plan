package com.krachtix.audit.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// @Configuration - Kafka disabled
//class KafkaConfig {
//
//    // @Bean
//    fun auditLogsRetry() = NewTopic("krachtix-audit-logs-retry", 5, 1)
//
//    // @Bean
//    fun auditLogsDlt() = NewTopic("krachtix-audit-logs-dlt", 5, 1)
//}