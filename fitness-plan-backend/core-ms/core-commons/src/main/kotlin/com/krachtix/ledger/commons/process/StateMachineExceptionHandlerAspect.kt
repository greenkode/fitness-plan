package com.krachtix.process

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.process.PROCESS_ERROR
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.statemachine.StateContext
import org.springframework.stereotype.Component

@Aspect
@Component
class StateMachineExceptionHandlerAspect {

    val log = KotlinLogging.logger {}

    @Around("execution(* org.springframework.statemachine.action.Action.*(..))")
    fun handleException(joinPoint: ProceedingJoinPoint): Any? {

        val context = joinPoint.args[0] as StateContext<*, *>

        var proceed: Any? = null
        try {
            proceed = joinPoint.proceed()
        } catch (e: Exception) {
            context.stateMachine.extendedState.variables[PROCESS_ERROR] = e
        }

        return proceed
    }
}