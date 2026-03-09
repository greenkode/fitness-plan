package com.krachtix.commons.process.domain.action

import com.krachtix.commons.process.ProcessGateway
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessHeader
import com.krachtix.commons.process.enumeration.ProcessState
import org.springframework.statemachine.StateContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.statemachine.action.Action
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnBean(ProcessGateway::class)
class ProcessExpiredAction(private val processGateway: ProcessGateway) : Action<ProcessState, ProcessEvent> {

    private val log = KotlinLogging.logger {}

    override fun execute(context: StateContext<ProcessState, ProcessEvent>) {

        log.info { "Process: ${context.messageHeaders[ProcessHeader.PROCESS_ID.name]} expired!" }

        processGateway.findByPublicId(context.messageHeaders[ProcessHeader.PROCESS_ID.name] as UUID)?.let { process ->
            processGateway.expireProcess(process.publicId, true)
        }
    }
}