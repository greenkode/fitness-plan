package com.krachtix.pipelinr

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.repack.com.google.common.reflect.TypeToken
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

interface CommandValidator<C : Command<R>?, R> {
    fun validate(command: C)

    fun matches(command: C): Boolean {
        val typeToken: TypeToken<C> = object : TypeToken<C>(javaClass) {
        }

        return typeToken.isSupertypeOf(command!!::class.java)
    }
}

@Component
internal class ValidationMiddleware(private val validators: ObjectProvider<CommandValidator<Command<*>, *>>) :
    Command.Middleware {
    override fun <R, C : Command<R>> invoke(command: C, next: Command.Middleware.Next<R>): R {
        validators.stream().filter { v -> v.matches(command) }.findFirst()
            .ifPresent { v -> v.validate(command) }
        return next.invoke()
    }
}