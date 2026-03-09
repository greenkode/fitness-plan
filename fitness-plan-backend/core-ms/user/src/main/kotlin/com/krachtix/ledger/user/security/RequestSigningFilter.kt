package com.krachtix.user.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.krachtix.commons.security.RequireSignedRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.ContentCachingRequestWrapper

private val log = KotlinLogging.logger {}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(
    name = ["security.request-signing.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class RequestSigningFilter(
    private val requestSigningService: RequestSigningService,
    private val secretKeyResolver: SigningSecretKeyResolver,
    private val handlerMapping: RequestMappingHandlerMapping
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!requiresSignature(request)) {
            filterChain.doFilter(request, response)
            return
        }

        val signature = request.getHeader(RequestSigningService.SIGNATURE_HEADER)
        val timestamp = request.getHeader(RequestSigningService.TIMESTAMP_HEADER)?.toLongOrNull()
        val nonce = request.getHeader(RequestSigningService.NONCE_HEADER)

        if (signature == null || timestamp == null || nonce == null) {
            log.warn { "Missing signature headers for signed request: ${request.method} ${request.requestURI}" }
            sendSignatureError(response, "Missing required signature headers")
            return
        }

        val wrappedRequest = ContentCachingRequestWrapper(request, request.contentLength.coerceAtLeast(1024))
        val body = wrappedRequest.reader.readText()

        val secretKey = secretKeyResolver.resolveSecretKey(wrappedRequest)
        if (secretKey == null) {
            log.warn { "Could not resolve signing secret key for request: ${request.method} ${request.requestURI}" }
            sendSignatureError(response, "Unable to validate signature")
            return
        }

        val result = requestSigningService.validateSignature(
            secretKey = secretKey,
            method = request.method,
            path = request.requestURI,
            body = body.takeIf { it.isNotBlank() },
            timestamp = timestamp,
            nonce = nonce,
            providedSignature = signature
        )

        when (result) {
            RequestSigningService.SignatureValidationResult.VALID -> {
                filterChain.doFilter(wrappedRequest, response)
            }
            RequestSigningService.SignatureValidationResult.TIMESTAMP_EXPIRED -> {
                sendSignatureError(response, "Request timestamp expired")
            }
            RequestSigningService.SignatureValidationResult.INVALID_SIGNATURE -> {
                sendSignatureError(response, "Invalid request signature")
            }
            RequestSigningService.SignatureValidationResult.MISSING_HEADERS -> {
                sendSignatureError(response, "Missing required signature headers")
            }
        }
    }

    private fun requiresSignature(request: HttpServletRequest): Boolean {
        return runCatching {
            val handler = handlerMapping.getHandler(request)?.handler
            if (handler is HandlerMethod) {
                handler.hasMethodAnnotation(RequireSignedRequest::class.java) ||
                        handler.beanType.isAnnotationPresent(RequireSignedRequest::class.java)
            } else {
                false
            }
        }.getOrElse { false }
    }

    private fun sendSignatureError(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """{"error":"signature_invalid","message":"$message","status":401}"""
        )
    }
}
