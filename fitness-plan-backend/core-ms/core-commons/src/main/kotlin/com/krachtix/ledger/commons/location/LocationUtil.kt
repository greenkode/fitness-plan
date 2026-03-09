package com.krachtix.location

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object LocationUtil {
    val clientIpAddress: String?
        get() {
            val request: HttpServletRequest =
                (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
                    .request

            return request.getAttribute("clientIpAddress") as String?
        }

    val location: String?
        get() {
            val request: HttpServletRequest =
                (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes)
                    .request

            return request.getHeader("X-Location")
        }

    val defaultLatitude: String = "7.13492"
    val defaultLongitude: String = "6.29843"
}