package com.krachtix.db

import org.springframework.transaction.annotation.Transactional

/**
 * Annotation to mark methods that should use the read replica database.
 * 
 * Usage:
 * ```kotlin
 * @Service
 * class ReportingService {
 *     
 *     @ReadOnly
 *     fun generateReport(): Report {
 *         // This method will use the read replica
 *         return reportRepository.findAll()
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(
    transactionManager = "readReplicaTransactionManager", 
    readOnly = true
)
annotation class ReadOnly