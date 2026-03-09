package com.krachtix.user.config

import com.krachtix.commons.json.ObjectMapperFacade
import jakarta.servlet.ServletResponseWrapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.krachtix.commons.exception.GlobalExceptionHandler
import com.krachtix.commons.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component


@Component
class CustomAuthenticationEntryPoint(
    private val resolver: GlobalExceptionHandler
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        populateErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ResponseEntity(resolver.handleAuthenticationException(authException), HttpStatus.UNAUTHORIZED),
            response
        )
    }

    private fun populateErrorResponse(
        code: Int,
        message: ResponseEntity<ErrorResponse>,
        response: HttpServletResponse
    ) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        response.status = code

        val responseWrapper = response as ServletResponseWrapper

        responseWrapper.response.outputStream.write(ObjectMapperFacade.writeValueAsString(message.body ?: "An Authentication Error Occurred").toByteArray())
    }
}