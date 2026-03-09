package com.krachtix.identity.process.strategy

import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessStrategyBeanNames
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component(ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY)
class DefaultProcessStrategy : ProcessStrategy {

    private val log = KotlinLogging.logger {}

    override fun processEvent(
        process: ProcessDto,
        event: ProcessEvent
    ): ProcessState {

        log.info { "Processing Process event: $event for process: ${process.publicId}" }

        return when (process.state to event) {
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED -> {
                log.info { "Default operation completed for process: ${process.publicId}" }
                ProcessState.COMPLETE
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> {
                log.info { "Default operation failed for process: ${process.publicId}" }
                ProcessState.FAILED
            }

            ProcessState.PENDING to ProcessEvent.AUTH_TOKEN_RESEND -> {
                log.info { "Authentication token resent for process: ${process.publicId}" }
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> {
                log.info { "Default operation expired for process: ${process.publicId}" }
                ProcessState.EXPIRED
            }

            ProcessState.PENDING to ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED ->{
                log.info { "Default operation verified for process: ${process.publicId}" }
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.ORGANIZATION_PROFILE_COMPLETED -> {
                log.info { "Organization profile step completed for process: ${process.publicId}" }
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.ORGANIZATION_CURRENCY_SELECTED -> {
                log.info { "Organization currency step completed for process: ${process.publicId}" }
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.ORGANIZATION_PREFERENCES_SAVED -> {
                log.info { "Organization preferences step completed for process: ${process.publicId}" }
                ProcessState.PENDING
            }

            else -> {
                log.warn { "Invalid state transition: ${process.state} -> $event" }
                throw IllegalStateException("Invalid transition from ${process.state} with event $event")
            }
        }
    }

    override fun isValidTransition(
        currentState: ProcessState,
        event: ProcessEvent,
        targetState: ProcessState
    ): Boolean {
        return when (Triple(currentState, event, targetState)) {
            Triple(ProcessState.PENDING, ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.AUTH_TOKEN_RESEND, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_COMPLETED, ProcessState.COMPLETE) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_FAILED, ProcessState.FAILED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_EXPIRED, ProcessState.EXPIRED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.ORGANIZATION_PROFILE_COMPLETED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.ORGANIZATION_CURRENCY_SELECTED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.ORGANIZATION_PREFERENCES_SAVED, ProcessState.PENDING) -> true
            else -> false
        }
    }

    override fun calculateExpectedState(
        currentState: ProcessState,
        event: ProcessEvent
    ): ProcessState {
        return when (currentState to event) {
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED -> ProcessState.COMPLETE
            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> ProcessState.FAILED
            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> ProcessState.EXPIRED
            ProcessState.PENDING to ProcessEvent.ORGANIZATION_PROFILE_COMPLETED -> ProcessState.PENDING
            ProcessState.PENDING to ProcessEvent.ORGANIZATION_CURRENCY_SELECTED -> ProcessState.PENDING
            ProcessState.PENDING to ProcessEvent.ORGANIZATION_PREFERENCES_SAVED -> ProcessState.PENDING
            else -> currentState
        }
    }
}